package com.campushub.post.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.util.StringUtils;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.post.constant.PostCategories;
import com.campushub.post.dto.PostCreateRequest;
import com.campushub.post.dto.PostQueryRequest;
import com.campushub.post.dto.PostUpdateRequest;
import com.campushub.post.entity.Post;
import com.campushub.post.mapper.PostMapper;
import com.campushub.post.service.PostService;
import com.campushub.post.vo.PostFeedVO;
import com.campushub.post.vo.PostPageVO;
import com.campushub.post.vo.PostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

/** 帖子基础业务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    private final AuthenticationService authenticationService;

    private final StringUtils stringUtils;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

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

    /** 查询未删除帖子；帖子不存在时返回 404 业务异常。 */
    @Override
    public PostVO getById(Long postId) {
        // 记录按主键查询任务的耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("postMapper.selectById");
        Post post = findPost(postId);
        stopWatch.stop();
        log.info("按 ID 查询帖子任务耗时 {} ms", stopWatch.getTotalTimeMillis());
        return toPostVO(post);
    }

    /** 浏览帖子：返回帖子详情并将浏览量原子加一，落库计数由数据库自增保证并发下不丢失。 */
    @Override
    public PostVO view(Long postId) {
        Post post = findPost(postId);

        // 记录浏览量自增任务的耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("postMapper.increaseViewNumber");
        postMapper.increaseViewNumber(post.getPostId());
        stopWatch.stop();
        log.info("帖子浏览量自增任务耗时 {} ms", stopWatch.getTotalTimeMillis());

        // 返回的浏览量为读取值加一；并发浏览时展示值可能略有滞后，但落库计数不会丢失
        post.setPostViewNumber(post.getPostViewNumber() + 1);
        return toPostVO(post);
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

    /** 修改当前登录用户创建的帖子，不更新创建时间和更新时间字段。 */
    @Override
    public PostVO update(Long postId, PostUpdateRequest request) {
        Post post = requireOwnedPost(postId);
        post.setPostTitle(request.postTitle().trim());
        post.setPostType(normalizePostType(request.postType(), true));
        post.setPostText(request.postText());
        postMapper.updateById(post);
        return getById(postId);
    }

    /** 软删除当前登录用户创建的帖子。 */
    @Override
    public void delete(Long postId) {
        Post post = requireOwnedPost(postId);
        postMapper.deleteById(post.getPostId());
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
