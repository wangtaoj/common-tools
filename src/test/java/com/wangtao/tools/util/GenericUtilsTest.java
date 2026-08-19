package com.wangtao.tools.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

/**
 * @author wangtao
 * Created at 2026-08-15
 */
public class GenericUtilsTest {

    public static class Case1Father<T> {
    }

    public static class Case1Son<T> extends Case1Father<String> {
    }

    public static class Case1GrandSon extends Case1Son<Integer> {

    }

    /**
     * 通过祖先类中的直接子类解析
     */
    @Test
    public void testResolveGenericAt_v1() {
        Type type = GenericUtils.resolveGenericAt(Case1Son.class, Case1GrandSon.class, 0);
        Assertions.assertEquals("java.lang.Integer", type.getTypeName());

        // 祖先类
        type = GenericUtils.resolveGenericAt(Case1Father.class, Case1GrandSon.class, 0);
        Assertions.assertEquals("java.lang.String", type.getTypeName());
    }

    public static class Case2Father<T> {
    }

    public static class Case2Son<T> extends Case2Father<T> {

    }

    public static class Case2GrandSon extends Case2Son<Integer> {

    }

    /**
     * 特殊情况:
     * 指定祖先类中的直接子类没有特化泛型, 通过实现类一层一层还原泛型变量
     */
    @Test
    public void testResolveGenericAt_v2() {
        // 无法推断
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> GenericUtils.resolveGenericAt(Case2Father.class, Case2Son.class, 0)
        );

        Type type = GenericUtils.resolveGenericAt(Case2Son.class, Case2GrandSon.class, 0);
        Assertions.assertEquals("java.lang.Integer", type.getTypeName());

        // 祖先类
        type = GenericUtils.resolveGenericAt(Case2Father.class, Case2GrandSon.class, 0);
        Assertions.assertEquals("java.lang.Integer", type.getTypeName());
    }
}
