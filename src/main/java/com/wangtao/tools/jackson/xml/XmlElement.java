package com.wangtao.tools.jackson.xml;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于指定 XML 元素名称。
 * 配合 {@link ObjectXmlSerializer} 使用，可在序列化容器类型时自定义内层元素标签名。
 * <p>
 * 设计目的：
 * 当同一个对象中有多个容器类型（如 List），且希望它们的包装名不同，但内层元素名称相同时，
 * 使用 Jackson 原生的 {@code @JacksonXmlElementWrapper} 和 {@code @JacksonXmlProperty} 注解无法满足需求，
 * 通过本注解 + {@link ObjectXmlSerializer} 可以实现该功能。
 * </p>
 *
 * @see ObjectXmlSerializer
 * @author wangtao
 * Created at 2026-08-19
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XmlElement {

    /**
     * 指定 XML 元素的本地名称（即标签名）。
     *
     * @return 元素名称
     */
    String localName();
}