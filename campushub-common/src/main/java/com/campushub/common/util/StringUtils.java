package com.campushub.common.util;

/** 字符串工具接口。 */
public interface StringUtils {

    // 去除字符串首尾空白，结果为空字符串时返回 null
    String normalizeText(String value);
}
