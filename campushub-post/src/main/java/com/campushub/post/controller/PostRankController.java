package com.campushub.post.controller;

import com.campushub.common.response.Result;
import com.campushub.post.service.PostRankService;
import com.campushub.post.vo.RankItemVO;
import com.campushub.post.vo.RankVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 帖子热度榜查询接口，数据源为 Redis ZSet，MySQL 全程无感知。 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostRankController {

    private final PostRankService postRankService;

    /** 当日热度榜前 N 名，n 缺省 10。 */
    @GetMapping("/rank/top")
    public Result<List<RankItemVO>> top(@RequestParam(required = false) Integer n) {
        return Result.success(postRankService.top(n));
    }

    /** 近 7 日周榜前 N 名，合成结果缓存 10 分钟，n 缺省 10。 */
    @GetMapping("/rank/week")
    public Result<List<RankItemVO>> week(@RequestParam(required = false) Integer n) {
        return Result.success(postRankService.week(n));
    }

    /** 单帖当日名次与热度分，未上榜时 rank 与 score 均为 null。 */
    @GetMapping("/{postId}/rank")
    public Result<RankVO> rankOf(@PathVariable Long postId) {
        return Result.success(postRankService.rankOf(postId));
    }
}
