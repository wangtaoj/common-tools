package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2026-11-21
 */
public class HtmlDocumentCreator extends AbstractDocumentCreator {

    private final static HtmlDocumentCreator INSTANCE = new HtmlDocumentCreator();

    private HtmlDocumentCreator() {
    }

    /**
     * @return 单例实例
     */
    public static HtmlDocumentCreator getInstance() {
        return INSTANCE;
    }

    /**
     * @return 当前创建器对应的文档类型
     */
    @Override
    protected DocumentType documentType() {
        return DocumentType.HTML;
    }

    /**
     * 会忽略水印与 baseUri
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void create(String templatePath, Map<String, Object> data, String baseUri, String watermarkText, OutputStream out) {
        // 流式渲染，避免中间 String 全量拷贝；
        // Writer 只 flush 不 close，out 的关闭由调用方负责
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        try {
            eval(templatePath, data, writer);
            writer.flush();
        } catch (IOException e) {
            throw new ToolRuntimeException(e);
        }
    }
}
