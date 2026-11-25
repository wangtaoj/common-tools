package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;

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
public class HtmlDocumentCreator implements DocumentCreator {

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
            ThymeleafUtils.eval(templatePath, data, writer);
            writer.flush();
        } catch (IOException e) {
            throw new ToolRuntimeException(e);
        }
    }
}
