package com.wangtao.tools.document;

import java.io.OutputStream;
import java.util.Map;

/**
 * 文档创建器接口，用于将 html 模板渲染为 WORD、PDF 等文档格式
 *
 * @author wangtao
 * Created at 2026-11-20
 */
public interface DocumentCreator {

    /**
     * 创建文档（核心方法，实现类只需实现此方法）
     *
     * @param templatePath  html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data          模板数据
     * @param baseUri       资源基础 URI（如 classpath 目录、file: 目录/URL），用于解析模板中的相对图片等资源，可为 null
     * @param watermarkText 水印文字，null 表示不加
     * @param out           文档输出流，由调用方负责关闭
     */
    void create(String templatePath, Map<String, Object> data, String baseUri, String watermarkText, OutputStream out);

    /**
     * 创建文档（无水印）
     *
     * @param templatePath html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data         模板数据
     * @param out          文档输出流，由调用方负责关闭
     */
    default void create(String templatePath, Map<String, Object> data, OutputStream out) {
        create(templatePath, data, null, null, out);
    }

    /**
     * 创建文档（带文字水印）
     *
     * @param templatePath  html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data          模板数据
     * @param watermarkText 水印文字，null 表示不加
     * @param out           文档输出流，由调用方负责关闭
     */
    default void create(String templatePath, Map<String, Object> data, String watermarkText, OutputStream out) {
        create(templatePath, data, null, watermarkText, out);
    }

    static DocumentCreator getInstance(DocumentType documentType) {
        switch (documentType) {
            case HTML:
                return HtmlDocumentCreator.getInstance();
            case WORD:
                return WordDocumentCreator.getInstance();
            case PDF:
                return PdfDocumentCreator.getInstance();
            case OPENHTMLTOPDF:
                return OpenHtmlToPdfDocumentCreator.getInstance();
            default:
                throw new IllegalArgumentException("unsupported document type: " + documentType);
        }
    }
}
