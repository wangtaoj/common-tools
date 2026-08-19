package com.wangtao.tools.util;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2026-08-15
 */
public final class GenericUtils {

    private GenericUtils() {

    }

    /**
     * 解析父类中泛型变量实际的运行类型
     * @param clazz class对象
     * @param index 泛型变量位置
     * @return 父类中泛型变量实际的运行类型
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> resolveGenericToClassAt(Class<?> clazz, int index) {
        Type type = resolveGenericAt(clazz, index);
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        throw new IllegalArgumentException(clazz.getTypeName() + "父类中泛型变量实际的运行类型不是Class类型");
    }

    /**
     * 解析指定祖先类(接口)中泛型变量实际的运行类型
     * @param ancestorClass 祖先类(接口)
     * @param implClass 实现类
     * @param index 泛型变量位置
     * @return 祖先类(接口)中泛型变量实际的运行类型
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> resolveGenericToClassAt(Class<?> ancestorClass, Class<?> implClass, int index) {
        Type type = resolveGenericAt(ancestorClass, implClass, index);
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        throw new IllegalArgumentException(implClass.getTypeName() + "祖先中泛型变量实际的运行类型不是Class类型");
    }

    /**
     * 解析父类中泛型变量实际的运行类型
     * @param clazz class对象
     * @param index 泛型变量位置
     * @return 父类中泛型变量实际的运行类型
     */
    public static Type resolveGenericAt(Class<?> clazz, int index) {
        Type type = clazz.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (index >= actualTypeArguments.length) {
                throw new IllegalArgumentException("无效索引位置: " + index + ", 实际的泛型变量个数: " + actualTypeArguments.length);
            }
            return actualTypeArguments[index];
        }
        throw new IllegalArgumentException(clazz.getTypeName() + "的父类不是ParameterizedType");
    }

    /**
     * 解析指定祖先类(接口)中泛型变量实际的运行类型
     * @param ancestorClass 祖先类(接口)
     * @param implClass 实现类
     * @param index 泛型变量位置
     * @return 祖先类(接口)中泛型变量实际的运行类型
     */
    public static Type resolveGenericAt(Class<?> ancestorClass, Class<?> implClass, int index) {
        if (!ancestorClass.isAssignableFrom(implClass)) {
            throw new IllegalArgumentException(implClass.getTypeName() + "与" + ancestorClass.getTypeName() + "无继承关系");
        }
        TypeVariable<? extends Class<?>>[] typeParameters = ancestorClass.getTypeParameters();
        if (index >= typeParameters.length) {
            throw new IllegalArgumentException("无效索引位置: " + index + ", 实际的泛型变量个数: " + typeParameters.length);
        }
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(implClass, ancestorClass);
        TypeVariable<?> typeVariable = typeParameters[index];
        Type type = typeArguments.get(typeVariable);
        if (type == null || type instanceof TypeVariable<?>) {
            throw new IllegalArgumentException(ancestorClass.getTypeName() + "中的泛型变量" + typeVariable + "无法从子类" + implClass.getTypeName() + "推断出来");
        }
        return type;

    }

    /**
     * 该方法备用
     * 解析指定祖先类(接口)中泛型变量实际的运行类型
     * @param ancestorClass 祖先类(接口)
     * @param implClass 实现类
     * @param index 泛型变量位置
     * @return 祖先类(接口)中泛型变量实际的运行类型
     */
    @Deprecated
    public static Type resolveGenericAt_bak(Class<?> ancestorClass, Class<?> implClass, int index) {
        if (!ancestorClass.isAssignableFrom(implClass)) {
            throw new IllegalArgumentException(implClass + "与" + ancestorClass + "无继承关系");
        }
        ResolvableType resolvableType = ResolvableType.forClass(implClass).as(ancestorClass);
        ResolvableType[] genericTypes = resolvableType.getGenerics();
        if (index >= genericTypes.length) {
            throw new IllegalArgumentException("无效索引位置: " + index + ", 实际的泛型变量个数: " + genericTypes.length);
        }
        ResolvableType genericResolvableType = resolvableType.getGeneric(index);
        Type type = genericResolvableType.getType();
        // 如果还是泛型变量, 手动解析
        if (type instanceof TypeVariable<?>) {
            Class<?> resolved = genericResolvableType.resolve();
            if (resolved == null) {
                throw new IllegalArgumentException(implClass + "无法解析泛型");
            }
            if (genericResolvableType.hasGenerics()) {
                type = ResolvableType.forClassWithGenerics(resolved, genericResolvableType.getGenerics()).getType();
            } else {
                type = resolved;
            }
        }
        return type;
    }
}
