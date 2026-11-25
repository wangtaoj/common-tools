package com.wangtao.tools.document;

import com.wangtao.tools.util.ThymeleafUtils;

import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档创建器抽象基类：统一渲染 html 模板，并向模板注入当前文档类型变量
 * <p>子类只需实现 {@link #documentType()}，即可在模板中通过 {@code documentType} 变量做条件渲染，
 * 例如 word 与 pdf 使用不同的字体
 *
 * @author wangtao
 * Created at 2026-11-25
 */
public abstract class AbstractDocumentCreator implements DocumentCreator {

    /**
     * 当前创建器对应的文档类型
     * <p>渲染模板时会以其枚举名注入为 {@link DocumentCreator#DOCUMENT_TYPE} 变量；
     * 非枚举中的变体（如 html 伪装 word）可返回最接近的类型
     *
     * @return 文档类型
     */
    protected abstract DocumentType documentType();

    /**
     * 复制模板数据并附加当前文档类型，避免修改调用方传入的 Map
     *
     * @param data 原始模板数据，可为 null
     * @return 含 {@link DocumentCreator#DOCUMENT_TYPE} 变量的新数据 Map
     */
    protected Map<String, Object> withDocumentType(Map<String, Object> data) {
        Map<String, Object> variables = new HashMap<>();
        if (data != null) {
            variables.putAll(data);
        }
        variables.put(DOCUMENT_TYPE, documentType().name());
        return variables;
    }

    /**
     * 创建文档（无水印）
     *
     * @param templatePath html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data         模板数据
     * @param out          文档输出流，由调用方负责关闭
     */
    @Override
    public void create(String templatePath, Map<String, Object> data, OutputStream out) {
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
    @Override
    public void create(String templatePath, Map<String, Object> data, String watermarkText, OutputStream out) {
        create(templatePath, data, null, watermarkText, out);
    }

    /**
     * 渲染 html 模板，并注入当前文档类型变量
     *
     * @param templatePath html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data         模板数据，可为 null
     * @return 渲染后的 html
     */
    protected String eval(String templatePath, Map<String, Object> data) {
        return ThymeleafUtils.eval(templatePath, withDocumentType(data));
    }

    /**
     * 流式渲染 html 模板，并注入当前文档类型变量
     *
     * @param templatePath html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data         模板数据，可为 null
     * @param writer       输出流
     */
    protected void eval(String templatePath, Map<String, Object> data, Writer writer) {
        ThymeleafUtils.eval(templatePath, withDocumentType(data), writer);
    }
}
