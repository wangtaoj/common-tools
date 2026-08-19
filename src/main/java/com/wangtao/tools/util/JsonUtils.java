package com.wangtao.tools.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * @author wangtao
 * Created at 2023-09-23
 */
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {

        // 设置全局的DateFormat
        objectMapper.setDateFormat(new SimpleDateFormat(JavaTimeModuleUtils.STANDARD_PATTERN));

        // 设置全局的时区, Jackson默认值为UTC
        objectMapper.setTimeZone(TimeZone.getDefault());

        // 初始化JavaTimeModule
        JavaTimeModule javaTimeModule = JavaTimeModuleUtils.create();

        // 注册JSR310时间模块
        objectMapper.registerModules(javaTimeModule);

        // 注册JDK8新增的一些类型, 比如Optional
        objectMapper.registerModule(new Jdk8Module());

        // 包含所有字段
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 在序列化一个空对象时时不抛出异常
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 忽略反序列化时在json字符串中存在, 但在java对象中不存在的属性
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // BigDecimal.toPlainString(), 这样不会有科学计数法(序列化后仍是数字, 不是字符串)
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    }

    private JsonUtils() {

    }

    /**
     * 将Java对象序列化成一个JSON字符串.
     *
     * @param object Java对象
     * @return 返回一个JSON格式的字符串
     */
    public static String objToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("parse %s to json error", object), e);
        }
    }

    /**
     * 将JSON字符串反序列化成一个Java对象.
     *
     * @param json JSON字符串
     * @param cls  Java对象原始类型的class对象
     * @param <T>  Java对象的原始类型
     * @return 返回一个T类型的对象
     */
    public static <T> T jsonToObj(String json, Class<T> cls) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, cls);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("parse %s to obj error", json), e);
        }
    }

    /**
     * 将JSON字符串反序列化成一个Java对象.
     * TypeReference是一个抽象类, 用来构造类型
     * 调用方式: 传入一个TypeReference的匿名实现类即可
     *
     * <pre>{@code
     * User user = jsonToObj(json, new TypeReference<User>(){})
     * List<User> users = jsonToObj(json, new TypeReference<List<User>>(){})
     * }</pre>
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           返回值类型
     * @return 返回一个Java对象
     */
    public static <T> T jsonToObj(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("parse %s to obj error", json), e);
        }
    }

    /**
     * 将JSON字符串反序列化成一个Java对象.
     *
     * @param json     JSON字符串
     * @param javaType Java类型
     * @param <T>      返回值类型
     * @return 返回一个Java对象
     */
    public static <T> T jsonToObj(String json, JavaType javaType) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("parse %s to obj error", json), e);
        }
    }

    /**
     * 将JSON字符串反序列化成一个Java对象.
     *
     * @param json JSON字符串
     * @param type Java类型
     * @param <T>  返回值类型
     * @return 返回一个Java对象
     */
    public static <T> T jsonToObj(String json, Type type) {
        JavaType javaType = getTypeFactory().constructType(type);
        return jsonToObj(json, javaType);
    }

    /**
     * 将JSON字符串反序列化成一个List对象.
     *
     * @param json JSON字符串
     * @param cls  List中的元素类型
     * @param <T>  List中的元素类型
     * @return 返回一个List列表
     */
    public static <T> List<T> jsonToList(String json, Class<T> cls) {
        JavaType javaType = getTypeFactory().constructCollectionType(List.class, cls);
        return jsonToObj(json, javaType);
    }

    /**
     * 将对象转换为指定类型的对象.
     *
     * @param obj  源对象
     * @param type Java类型
     * @param <T>  返回值类型
     * @return 返回一个Java对象
     */
    public static <T> T convertValue(Object obj, Type type) {
        JavaType javaType = getTypeFactory().constructType(type);
        return convertValue(obj, javaType);
    }

    /**
     * 将对象转换为指定类型的对象.
     *
     * @param obj      源对象
     * @param javaType Java类型
     * @param <T>      返回值类型
     * @return 返回一个Java对象
     */
    public static <T> T convertValue(Object obj, JavaType javaType) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, javaType);
    }

    /**
     * 获取类型工厂
     *
     * @return 返回类型工厂
     */
    public static TypeFactory getTypeFactory() {
        return objectMapper.getTypeFactory();
    }
}
