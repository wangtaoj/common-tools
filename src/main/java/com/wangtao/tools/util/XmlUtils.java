package com.wangtao.tools.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * XML 序列化/反序列化工具类，基于 Jackson XmlMapper 封装。
 * 提供了丰富的重载方法，支持普通类型、泛型集合、自定义 JavaType 等。
 * <p>
 * 配置特性：
 * <ul>
 *   <li>序列化时包含所有的字段, 包括null</li>
 *   <li>允许空 Bean 序列化</li>
 *   <li>忽略未知属性</li>
 *   <li>BigDecimal使用toPlainString()输出</li>
 *   <li>输出 XML 声明头</li>
 *   <li>日期默认格式：yyyy-MM-dd HH:mm:ss</li>
 *   <li>全局时区使用JVM启动时默认时区</li>
 *   <li>支持JSR310时间模块</li>
 * </ul>
 *
 * @see XmlMapper
 * @author wangtao
 * Created at 2026-08-19
 */
public final class XmlUtils {

    private static final XmlMapper xmlMapper = new XmlMapper();

    static {
        // 序列化包含策略
        xmlMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 禁用空 Bean 序列化失败
        xmlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 禁用未知属性反序列化失败
        xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 将 BigDecimal 输出为普通数字（而非科学计数法）
        xmlMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        // 序列化时输出 XML 声明（<?xml version="1.0" encoding="UTF-8"?>）
        xmlMapper.enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION);

        // 设置全局的DateFormat
        xmlMapper.setDateFormat(new SimpleDateFormat(JavaTimeModuleUtils.STANDARD_PATTERN));
        // 设置全局的时区, Jackson默认值为UTC
        xmlMapper.setTimeZone(TimeZone.getDefault());

        // 注册 Java 8 时间模块
        xmlMapper.registerModule(JavaTimeModuleUtils.create());
    }

    private XmlUtils() {

    }

    /**
     * 将Java对象序列化为XML字符串。
     *
     * @param obj 要序列化的对象
     * @return XML字符串
     * @throws IllegalArgumentException 如果序列化失败
     */
    public static String objToXml(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return xmlMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("XML serialization error", e);
        }
    }

    /**
     * 将XML字符串反序列化为指定类型的对象。
     *
     * @param xml   XML字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 如果反序列化失败
     */
    public static <T> T xmlToObj(String xml, Class<T> clazz) {
        if (StringUtils.isBlank(xml)) {
            return null;
        }
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("XML deserialization error: " + xml, e);
        }
    }

    /**
     * 将XML字符串反序列化为指定类型的对象。
     *
     * @param xml           XML 字符串
     * @param typeReference 类型引用，例如{@code new TypeReference<User>() {}}
     * @param <T>           目标泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 如果反序列化失败
     */
    public static <T> T xmlToObj(String xml, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(xml)) {
            return null;
        }
        try {
            return xmlMapper.readValue(xml, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("XML deserialization error: " + xml, e);
        }
    }

    /**
     * 将XML字符串反序列化为指定JavaType类型的对象。
     * JavaType 可通过 {@link #typeFactory()} 构建。
     *
     * @param xml       XML字符串
     * @param type      Java类型
     * @param <T>       目标类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 如果反序列化失败
     */
    public static <T> T xmlToObj(String xml, Type type) {
        if (StringUtils.isBlank(xml)) {
            return null;
        }
        try {
            JavaType javaType = typeFactory().constructType(type);
            return xmlMapper.readValue(xml, javaType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("XML deserialization error: " + xml, e);
        }
    }

    /**
     * 将XML字符串反序列化为指定JavaType类型的对象。
     * JavaType 可通过 {@link #typeFactory()} 构建。
     *
     * @param xml       XML字符串
     * @param javaType  Jackson的JavaType对象
     * @param <T>       目标类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 如果反序列化失败
     */
    public static <T> T xmlToObj(String xml, JavaType javaType) {
        if (StringUtils.isBlank(xml)) {
            return null;
        }
        try {
            return xmlMapper.readValue(xml, javaType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("XML deserialization error: " + xml, e);
        }
    }

    /**
     * 获取当前XmlMapper的TypeFactory，用于构建复杂的JavaType。
     *
     * @return TypeFactory 实例
     */
    public static TypeFactory typeFactory() {
        return xmlMapper.getTypeFactory();
    }
}
