package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
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
    @Override
    public void create(String templatePath, Map<String, Object> data, OutputStream out) {
        createWithWatermark(templatePath, data, null, out);
    }

    @Override
    public void createWithWatermark(String templatePath, Map<String, Object> data, String watermarkText, OutputStream out) {
        String html = ThymeleafUtils.eval(templatePath, data);
        try {
            IOUtils.write(html, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ToolRuntimeException(e);
        }
    }
}
