package com.campushub.admin.dto;

/**
 * 踢用户下线请求参数。
 *
 * @param userId 需要踢下线的用户主键，不允许为空
 */
public record KickoutRequest(Long userId) {
}
