package com.campushub.common.util.impl;

import com.campushub.common.util.StringUtils;
import org.springframework.stereotype.Component;

/** 字符串工具接口实现。 */
@Component
public class StringUtilsImpl implements StringUtils {

    /** 去除字符串首尾空白，结果为空字符串时返回 null。 */
    @Override
    public String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
