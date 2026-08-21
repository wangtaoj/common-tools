package com.wangtao.tools.jackson.xml;

/**
 * 默认集合项元素名称为sdo, 与JAXB风格保持一致
 *
 * @author wangtao
 * Created at 2026-08-19
 */
public class SdoObjectXmlSerializer extends ObjectXmlSerializer {

    public SdoObjectXmlSerializer() {
        super("sdo");
    }
}
