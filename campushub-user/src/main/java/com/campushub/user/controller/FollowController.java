package com.campushub.user.controller;

import com.campushub.common.response.Result;
import com.campushub.user.dto.FollowQueryRequest;
import com.campushub.user.service.FollowService;
import com.campushub.user.vo.FollowUserVO;
import com.campushub.user.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户关注接口。 */
@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /** 关注用户：重复关注幂等成功，成功后对方收到关注通知。 */
    @PostMapping("/{targetUserId}")
    public Result<Void> follow(@PathVariable Long targetUserId) {
        followService.follow(targetUserId);
        return Result.success();
    }

    /** 取消关注：未关注时幂等成功。 */
    @DeleteMapping("/{targetUserId}")
    public Result<Void> unfollow(@PathVariable Long targetUserId) {
        followService.unfollow(targetUserId);
        return Result.success();
    }

    /** 我关注的用户列表（游标分页）。 */
    @GetMapping("/following")
    public Result<PageVO<FollowUserVO>> following(@Valid @ModelAttribute FollowQueryRequest request) {
        return Result.success(followService.following(request));
    }

    /** 关注我的用户列表（粉丝，游标分页）。 */
    @GetMapping("/followers")
    public Result<PageVO<FollowUserVO>> followers(@Valid @ModelAttribute FollowQueryRequest request) {
        return Result.success(followService.followers(request));
    }
}
