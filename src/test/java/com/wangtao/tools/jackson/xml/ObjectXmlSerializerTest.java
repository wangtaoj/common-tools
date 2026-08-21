package com.wangtao.tools.jackson.xml;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wangtao.tools.util.XmlUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link ObjectXmlSerializer} 单元测试.
 * 通过 {@link XmlUtils} 端到端序列化, 用完整的序列化后 XML 字符串精确断言内层元素标签名覆写行为.
 *
 * @author wangtao
 * Created at 2026-08-19
 */
public class ObjectXmlSerializerTest {

    /**
     * 测试用的内层对象
     */
    public static class Item {
        public String id;
        public String name;

        public Item() {
        }

        public Item(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * 单个容器字段, 使用 @XmlElement 指定内层元素名为 bean
     */
    public static class SingleContainer {
        @XmlElement(localName = "bean")
        @JsonSerialize(contentUsing = ObjectXmlSerializer.class)
        public List<Item> beanList;
    }

    /**
     * 多个容器字段, 包装名不同但内层元素名相同, 这是该序列化器的设计目的
     */
    public static class MultiContainer {
        @XmlElement(localName = "bean")
        @JsonSerialize(contentUsing = ObjectXmlSerializer.class)
        public List<Item> firstList;

        @XmlElement(localName = "bean")
        @JsonSerialize(contentUsing = ObjectXmlSerializer.class)
        public List<Item> secondList;
    }

    /**
     * 使用 SdoObjectXmlSerializer, 内层元素名固定为 sdo, 无需 @XmlElement
     */
    public static class SdoContainer {
        @JsonSerialize(contentUsing = SdoObjectXmlSerializer.class)
        public List<Item> sdoList;
    }

    /**
     * 仅使用 ObjectXmlSerializer 但未标注 @XmlElement, 序列化时应抛异常
     */
    public static class NoAnnoContainer {
        @JsonSerialize(contentUsing = ObjectXmlSerializer.class)
        public List<Item> noAnnoList;
    }

    /**
     * {@code @XmlElement}指定的 localName 覆盖内层元素标签名, 且不沿用默认的包装名作为元素名.
     * 用完整 XML 字符串精确断言: 包装元素为字段名, 内层每个元素为 bean, 内层对象字段按声明顺序输出.
     */
    @Test
    public void testElementNameOverride() {
        SingleContainer c = new SingleContainer();
        c.beanList = Arrays.asList(new Item("1", "a"), new Item("2", "b"));

        String xml = XmlUtils.objToXml(c);

        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<SingleContainer><beanList>"
            + "<bean><id>1</id><name>a</name></bean>"
            + "<bean><id>2</id><name>b</name></bean>"
            + "</beanList></SingleContainer>";
        Assertions.assertEquals(expected, xml);
    }

    /**
     * 多个容器字段, 包装名不同但内层元素名相同(均为 bean).
     * 用完整 XML 字符串精确断言: firstList 与 secondList 各自独立包装, 内层元素统一为 bean.
     */
    @Test
    public void testMultipleContainersSameInnerName() {
        MultiContainer c = new MultiContainer();
        c.firstList = Arrays.asList(new Item("1", "a"), new Item("2", "b"));
        c.secondList = Collections.singletonList(new Item("3", "c"));

        String xml = XmlUtils.objToXml(c);

        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<MultiContainer>"
            + "<firstList>"
            + "<bean><id>1</id><name>a</name></bean>"
            + "<bean><id>2</id><name>b</name></bean>"
            + "</firstList>"
            + "<secondList>"
            + "<bean><id>3</id><name>c</name></bean>"
            + "</secondList>"
            + "</MultiContainer>";
        Assertions.assertEquals(expected, xml);
    }

    /**
     * SdoObjectXmlSerializer 内层元素名固定为 sdo.
     * 用完整 XML 字符串精确断言: 无需 @XmlElement, 内层元素即为 sdo.
     */
    @Test
    public void testSdoSerializer() {
        SdoContainer c = new SdoContainer();
        c.sdoList = Arrays.asList(new Item("1", "a"), new Item("2", "b"));

        String xml = XmlUtils.objToXml(c);

        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<SdoContainer><sdoList>"
            + "<sdo><id>1</id><name>a</name></sdo>"
            + "<sdo><id>2</id><name>b</name></sdo>"
            + "</sdoList></SdoContainer>";
        Assertions.assertEquals(expected, xml);
    }

    /**
     * 仅使用 ObjectXmlSerializer 但未标注 @XmlElement 时, elementName 为 null, 序列化抛 IllegalArgumentException.
     * 异常可能被 Jackson / XmlUtils 层层包装, 故沿因果链查找原始错误信息.
     */
    @Test
    public void testNoXmlElementAnnotationThrows() {
        NoAnnoContainer c = new NoAnnoContainer();
        c.noAnnoList = Collections.singletonList(new Item("1", "a"));

        IllegalArgumentException ex = Assertions.assertThrows(
            IllegalArgumentException.class, () -> XmlUtils.objToXml(c));

        Assertions.assertTrue(containsInCauseChain(ex, "elementName is null"),
            "异常链中应包含 elementName is null, 实际: " + describeChain(ex));
    }

    /**
     * 空列表不会触发 content 序列化器, 因此包装元素输出为自闭合标签, 不产生内层元素.
     * 用完整 XML 字符串精确断言: beanList 为空, 输出 {@code <beanList/>}.
     */
    @Test
    public void testEmptyListDoesNotInvokeSerializer() {
        SingleContainer c = new SingleContainer();
        c.beanList = Collections.emptyList();

        String xml = XmlUtils.objToXml(c);

        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<SingleContainer><beanList/></SingleContainer>";
        Assertions.assertEquals(expected, xml);
    }

    /**
     * 沿异常因果链查找是否存在包含指定关键字的消息
     */
    private static boolean containsInCauseChain(Throwable t, String keyword) {
        while (t != null) {
            if (t.getMessage() != null && t.getMessage().contains(keyword)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * 拼接异常因果链的描述, 便于断言失败时定位
     */
    private static String describeChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            sb.append(t.getClass().getSimpleName())
                .append(": ").append(t.getMessage()).append('\n');
            t = t.getCause();
        }
        return sb.toString();
    }
}
