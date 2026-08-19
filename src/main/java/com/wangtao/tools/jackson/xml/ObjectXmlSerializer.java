package com.wangtao.tools.jackson.xml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * 自定义 Jackson 序列化器，用于覆盖 XML 元素的标签名。
 * 实现 {@link ContextualSerializer} 接口，以便在序列化时动态获取 {@link XmlElement} 注解中指定的名称。
 * <p>
 * 使用方式：
 * 在需要自定义内层元素名称的 List 或容器字段上，同时标注：
 * <pre>{@code
 * @XmlElement(localName = "bean")
 * @JsonSerialize(contentUsing = ObjectXmlSerializer.class)
 * private List<Bean> beanList;
 * }</pre>
 * <p>
 * 序列化结果示例：
 * <pre>{@code
 * <ContainerBean>
 *     <BeanList1>
 *         <bean>...</bean>
 *     </BeanList1>
 *     <BeanList2>
 *         <bean>...</bean>
 *     </BeanList2>
 * </ContainerBean>
 * }</pre>
 *
 * @see XmlElement
 * @author wangtao20
 * Created at 2026-08-19
 */
public class ObjectXmlSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    /**
     * 要覆盖的 XML 元素名称（标签名）。
     */
    private String elementName;

    /**
     * 默认无参构造（框架要求）。
     */
    public ObjectXmlSerializer() {
        // 无操作
    }

    /**
     * 带元素名称的构造方法，由 {@link #createContextual} 调用。
     *
     * @param elementName 指定的 XML 标签名
     */
    public ObjectXmlSerializer(String elementName) {
        this.elementName = elementName;
    }

    /**
     * 序列化方法，覆盖当前节点的标签名。
     *
     * @param object     要序列化的对象
     * @param gen        JSON 生成器（实际为 {@link ToXmlGenerator}）
     * @param serializers 序列化提供者
     * @throws IOException 序列化异常
     */
    @Override
    public void serialize(Object object, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (this.elementName == null) {
            throw new IllegalArgumentException("elementName is null, please use @XmlElement to set the element name");
        }

        // 将当前节点标签名替换为指定的 elementName
        ToXmlGenerator xmlGen = (ToXmlGenerator) gen;
        xmlGen.setNextName(new QName(this.elementName));

        // 委托给默认序列化器处理值内容
        serializers.defaultSerializeValue(object, gen);
    }

    /**
     * 上下文序列化工厂方法，用于从字段/方法上读取 {@link XmlElement} 注解，
     * 并构造带有指定元素名称的序列化器实例。
     *
     * @param prov     序列化提供者
     * @param property 当前序列化的属性（字段或方法）
     * @return 如果注解存在则返回新的 {@link ObjectXmlSerializer} 实例，否则返回 null（表示不干预）
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        XmlElement annotation = property.getAnnotation(XmlElement.class);
        // 若用户指定了容器元素名称，则使用用户指定的名称
        if (annotation != null) {
            return new ObjectXmlSerializer(annotation.localName());
        }
        // 未标注注解时，返回 null，让 Jackson 使用默认行为
        return this;
    }
}
