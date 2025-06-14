package com.campushub.post.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 帖子条件查询请求参数，游标分页：首页不传游标，翻页传上一页返回的游标二元组。 */
public record PostQueryRequest(
        @Size(max = 64, message = "用户名称长度不能超过 64 个字符")
        String userName,

        @Size(max = 32, message = "帖子分类长度不能超过 32 个字符")
        String postType,

        @Size(max = 128, message = "帖子标题长度不能超过 128 个字符")
        String postTitle,

        @Pattern(regexp = "asc|desc|ASC|DESC", message = "排序方式只能为 asc 或 desc")
        String sortOrder,

        /** 上一页末行的创建时间，首页不传 */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime cursorTime,

        /** 上一页末行的帖子 ID，与 cursorTime 成对使用 */
        Long cursorId,

        /** 页大小，缺省 20，最大 100 */
        @Min(value = 1, message = "页大小不能小于 1")
        @Max(value = 100, message = "页大小不能超过 100")
        Integer pageSize
) {
}
