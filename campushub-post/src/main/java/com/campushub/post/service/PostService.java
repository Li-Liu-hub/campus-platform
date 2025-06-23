package com.campushub.post.service;

import com.campushub.post.dto.PostCreateRequest;
import com.campushub.post.dto.PostQueryRequest;
import com.campushub.post.dto.PostUpdateRequest;
import com.campushub.post.vo.PostPageVO;
import com.campushub.post.vo.PostVO;

/** 帖子基础业务接口。 */
public interface PostService {

    /** 创建帖子。 */
    PostVO create(PostCreateRequest request);

    /** 根据帖子 ID 查询未删除帖子。 */
    PostVO getById(Long postId);

    /** 浏览帖子：优先读取详情缓存，并将浏览增量记入 Redis 聚合 Hash，由后续定时任务折叠落库。 */
    PostVO view(Long postId);

    /** 按条件游标分页查询未删除帖子。 */
    PostPageVO query(PostQueryRequest request);

    /** 修改帖子。 */
    PostVO update(Long postId, PostUpdateRequest request);

    /** 软删除帖子。 */
    void delete(Long postId);
}
