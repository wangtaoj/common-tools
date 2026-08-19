package com.wangtao.tools.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2023-09-17
 */
public final class Assert {

    private Assert() {}

    public static void notNull(Object value, String msg) {
        if (value == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void notEmpty(Object value, String msg) {
        notNull(value, msg);
        if (value instanceof String) {
            if (StringUtils.isNotBlank(value.toString())) {
                return;
            }
        }
        if (value instanceof Collection<?>) {
            if (CollectionUtils.isNotEmpty((Collection<?>) value)) {
                return;
            }
        }
        if (value instanceof Map<?, ?>) {
            if (MapUtils.isNotEmpty((Map<?, ?>) value)) {
                return;
            }
        }
        if (value instanceof Object[]) {
            if (ArrayUtils.isNotEmpty((Object[]) value)) {
                return;
            }
        }
        throw new IllegalArgumentException(msg);
    }

    public static void notBlank(String value, String msg) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

}
