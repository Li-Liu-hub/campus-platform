package com.campushub.post.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.log.OperationLog;
import com.campushub.common.log.OperationTypes;
import com.campushub.common.util.StringUtils;
import com.campushub.infrastructure.redis.RedisService;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.post.constant.PostCategories;
import com.campushub.post.constant.PostRedisKeys;
import com.campushub.post.dto.PostCreateRequest;
import com.campushub.post.dto.PostQueryRequest;
import com.campushub.post.dto.PostUpdateRequest;
import com.campushub.post.entity.Post;
import com.campushub.post.mapper.PostMapper;
import com.campushub.post.service.PostService;
import com.campushub.post.vo.PostFeedVO;
import com.campushub.post.vo.PostPageVO;
import com.campushub.post.vo.PostVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 帖子基础业务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    private final AuthenticationService authenticationService;

    private final StringUtils stringUtils;

    private final RedisService redisService;

    private final ObjectMapper objectMapper;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 帖子详情缓存键前缀，完整键为 campushub:post:info:{postId}。 */
    private static final String CACHE_KEY_PREFIX = "campushub:post:info:";

    /** 浏览增量聚合 Hash 键，field 为帖子 ID，value 为未落库的浏览增量；刷库任务共用该定义。 */
    private static final String VIEW_DELTA_KEY = PostRedisKeys.VIEW_DELTA_KEY;

    /** 详情缓存基础 TTL 秒数，保证任何脏数据到期必死、下次回源必然新鲜。 */
    private static final long CACHE_TTL_SECONDS = 300;

    /** 详情缓存 TTL 随机抖动上限秒数，防止大批键同时过期集体回源。 */
    private static final long CACHE_TTL_JITTER_SECONDS = 120;

    /** 空值占位 TTL 秒数，刻意设置较短以缩小"帖子刚创建即被误判不存在"的窗口。 */
    private static final long NULL_CACHE_TTL_SECONDS = 60;

    /** 直接插入创建当前登录用户的帖子，幂等由数据库唯一键保证；重复提交时返回已存在的原帖子。 */
    @Override
    public PostVO create(PostCreateRequest request) {
        Long userId = requireCurrentUserId();
        String idempotencyKey = request.postIdempotencyKey().trim();
        Post post = new Post();
        post.setPostUserId(userId);
        post.setPostTitle(request.postTitle().trim());
        post.setPostType(normalizePostType(request.postType(), true));
        post.setPostText(request.postText());
        post.setPostIdempotencyKey(idempotencyKey);
        post.setPostViewNumber(0L);
        post.setPostIsDelete(0);
        try {
            postMapper.insert(post);
            // 新帖落地即清除可能残留的空值占位，保证立即可查
            evictCache(post.getPostId());
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突说明同一幂等键已插入成功，直接返回已存在的帖子；查不到说明命中的是已软删除的帖子
            Post existingPost = postMapper.selectOne(Wrappers.<Post>lambdaQuery()
                    .eq(Post::getPostUserId, userId)
                    .eq(Post::getPostIdempotencyKey, idempotencyKey)
                    .last("LIMIT 1"));
            if (existingPost == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "帖子幂等键已被使用");
            }
            return toPostVO(existingPost);
        }
        return getById(post.getPostId());
    }

    /** 查询未删除帖子，走详情缓存单键三分支读取；帖子不存在时返回 404 业务异常。 */
    @Override
    public PostVO getById(Long postId) {
        return getCachedPost(postId);
    }

    /**
     * 功能：浏览帖子，优先读取详情缓存并将浏览增量记入 Redis 聚合 Hash。
     *
     * <p>详情读取为单键三分支：命中 JSON 直接返回；空串占位说明该 ID 已验证不存在，
     * 直接拒绝以挡住穿透请求；未命中才回源数据库并写回缓存。浏览增量通过一条
     * HINCRBY 记账，field 不存在时自动从 0 累加，落库由后续定时任务折叠处理。
     *
     * @param postId 帖子主键，不允许为空
     * @return 帖子详情，浏览量为缓存快照，允许短暂滞后于真实累计值
     * @throws BusinessException 帖子 ID 为空时抛 400，帖子不存在时抛 404
     */
    @Override
    public PostVO view(Long postId) {
        // 详情复用 getById 的缓存读取，MySQL 全程无感知（命中时）
        PostVO postVO = getById(postId);
        // 浏览增量记账：替代原先每次浏览的数据库 UPDATE；当前未启用定时落库，增量暂存于 Hash
        try {
            redisService.hIncrBy(VIEW_DELTA_KEY, String.valueOf(postId), 1L);
        } catch (Exception exception) {
            // Redis 异常时增量无法记账，按最多一次语义放弃本次计数，不影响详情返回
            log.warn("浏览增量记账失败，postId={}", postId, exception);
        }
        return postVO;
    }

    /** 按条件游标分页查询帖子，返回本页数据和下一页游标，默认按创建时间倒序。 */
    @Override
    public PostPageVO query(PostQueryRequest request) {
        String postType = normalizePostType(request.postType(), false);
        String postTitle = stringUtils.normalizeText(request.postTitle());
        boolean orderByAsc = "asc".equalsIgnoreCase(stringUtils.normalizeText(request.sortOrder()));
        int pageSize = resolvePageSize(request.pageSize());

        // 记录条件查询任务的耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("postMapper.selectByCondition");
        List<Post> posts = postMapper.selectByCondition(postType, postTitle, orderByAsc,
                request.cursorTime(), request.cursorId(), pageSize);
        stopWatch.stop();
        log.info("条件查询帖子任务耗时 {} ms，命中 {} 条", stopWatch.getTotalTimeMillis(), posts.size());

        List<PostFeedVO> list = posts.stream()
                .map(this::toFeedVO)
                .toList();
        if (list.size() < pageSize) {
            return new PostPageVO(list, false, null, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        Post lastPost = posts.get(posts.size() - 1);
        return new PostPageVO(list, true, lastPost.getCreateTime(), lastPost.getPostId());
    }

    /** 页大小缺省 20，并限制在 1 到 100 之间。 */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /** 修改当前登录用户创建的帖子，不更新创建时间和更新时间字段；操作留痕由日志切面异步完成。 */
    @OperationLog(type = OperationTypes.POST_UPDATE, targetId = "#args[0]")
    @Override
    public PostVO update(Long postId, PostUpdateRequest request) {
        Post post = requireOwnedPost(postId);
        post.setPostTitle(request.postTitle().trim());
        post.setPostType(normalizePostType(request.postType(), true));
        post.setPostText(request.postText());
        postMapper.updateById(post);
        // 写路径采用先改库再删缓存：删除幂等，最坏代价只是下一位读者多一次回源
        evictCache(postId);
        return getById(postId);
    }

    /** 软删除当前登录用户创建的帖子。 */
    @Override
    public void delete(Long postId) {
        Post post = requireOwnedPost(postId);
        postMapper.deleteById(post.getPostId());
        // 删除帖子后同步清除缓存，避免已删内容在缓存存活期内继续可见
        evictCache(postId);
    }

    /** 校验帖子存在且属于当前登录用户，否则抛出对应业务异常。 */
    private Post requireOwnedPost(Long postId) {
        Post post = findPost(postId);
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(post.getPostUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该帖子");
        }
        return post;
    }

    /** 根据帖子 ID 查询未删除帖子，帖子不存在时抛出 404 业务异常。 */
    private Post findPost(Long postId) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "帖子 ID 不能为空");
        }
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    /**
     * 功能：按单键三分支读取帖子详情缓存，未命中时回源数据库并写回。
     *
     * <p>缓存键 campushub:post:info:{postId} 的值存在三种形态：JSON 表示存在且命中；
     * 空串表示已验证不存在（空值缓存，60 秒内重复请求不再穿透数据库）；null 表示
     * 尚未查询过，需回源。回源命中时以 300 秒加随机抖动的 TTL 写回，命中读永不续期，
     * 保证脏数据到期必死；回源未命中时写入空串占位。
     *
     * @param postId 帖子主键，不允许为空
     * @return 帖子详情
     * @throws BusinessException 帖子 ID 为空时抛 400，数据库中也不存在时抛 404
     */
    private PostVO getCachedPost(Long postId) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "帖子 ID 不能为空");
        }
        String cacheKey = CACHE_KEY_PREFIX + postId;
        String cached = null;
        try {
            cached = redisService.get(cacheKey);
        } catch (Exception exception) {
            // Redis 异常一律降级为回源数据库，缓存故障不能影响详情接口可用性
            log.warn("帖子缓存读取失败，降级回源数据库，postId={}", postId, exception);
        }
        if (cached != null) {
            // 空串占位：该 ID 已被验证不存在，直接拒绝
            if (cached.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
            }
            try {
                return objectMapper.readValue(cached, PostVO.class);
            } catch (Exception exception) {
                // 反序列化失败说明缓存内容损坏，删除毒 key 后按未命中回源重写；删除失败不阻断回源
                log.warn("帖子缓存反序列化失败，删除毒 key 后回源，postId={}", postId, exception);
                try {
                    redisService.delete(cacheKey);
                } catch (Exception deleteException) {
                    // 删除失败时毒 key 残留，由 TTL 到期自愈，本次仍走回源保证接口可用
                    log.warn("毒 key 删除失败，等待 TTL 自愈，postId={}", postId, deleteException);
                }
            }
        }
        Post post = postMapper.selectById(postId);
        if (post == null) {
            writeNullCache(cacheKey);
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        PostVO postVO = toPostVO(post);
        writeCache(cacheKey, postVO);
        return postVO;
    }

    /** 将帖子详情写入缓存，TTL 为 300 秒加随机抖动，命中读永不续期；写失败仅记录日志。 */
    private void writeCache(String cacheKey, PostVO postVO) {
        try {
            String json = objectMapper.writeValueAsString(postVO);
            long ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(CACHE_TTL_JITTER_SECONDS + 1);
            redisService.set(cacheKey, json, Duration.ofSeconds(ttl));
        } catch (Exception exception) {
            log.warn("帖子详情缓存写入失败，cacheKey={}", cacheKey, exception);
        }
    }

    /** 写入空值占位并设置较短 TTL，声明过期越快，帖子创建后的误判窗口越小。 */
    private void writeNullCache(String cacheKey) {
        try {
            redisService.set(cacheKey, "", Duration.ofSeconds(NULL_CACHE_TTL_SECONDS));
        } catch (Exception exception) {
            log.warn("空值占位写入失败，cacheKey={}", cacheKey, exception);
        }
    }

    /** 删除帖子详情缓存，删除幂等；失败仅记录日志，残留数据由 TTL 到期自愈。 */
    private void evictCache(Long postId) {
        try {
            redisService.delete(CACHE_KEY_PREFIX + postId);
        } catch (Exception exception) {
            log.warn("帖子详情缓存删除失败，postId={}", postId, exception);
        }
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }

    /** 规范化帖子分类并校验是否在支持列表内，required 表示分类是否允许为空。 */
    private String normalizePostType(String postType, boolean required) {
        String normalized = stringUtils.normalizeText(postType);
        if (normalized == null) {
            if (required) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "帖子分类不能为空");
            }
            return null;
        }
        if (!PostCategories.VALUES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "帖子分类不支持");
        }
        return normalized;
    }

    /** 将帖子实体转换为接口返回对象。 */
    private PostVO toPostVO(Post post) {
        return new PostVO(
                post.getPostId(),
                post.getPostUserId(),
                post.getPostTitle(),
                post.getPostType(),
                post.getPostText(),
                post.getPostIdempotencyKey(),
                post.getPostViewNumber(),
                post.getCreateTime(),
                post.getUpdateTime()
        );
    }

    /** 将帖子实体转换为信息流列表返回对象，仅保留列表展示所需字段。 */
    private PostFeedVO toFeedVO(Post post) {
        return new PostFeedVO(
                post.getPostId(),
                post.getUserNickname(),
                post.getPostTitle(),
                post.getPostType(),
                post.getPostViewNumber(),
                post.getCreateTime()
        );
    }
}
