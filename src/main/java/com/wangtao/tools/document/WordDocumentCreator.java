package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;

import java.io.OutputStream;
import java.util.Map;

/**
 * 基于 docx4j 的 Word 文档创建器：html 转 docx（Open XML），生成真正的 Word 文档
 * <p>html 由 {@link ThymeleafUtils} 渲染，再经 docx4j 的 XHTML importer 转换，
 * 支持 h1-h6、段落、表格（含样式）、图片（含 base64 data URI）、列表等
 * <p>注册到 fonts 目录的字体（见 {@link FontRegistrar}）在模板 CSS font-family 中指定后，
 * docx 内会引用对应字体名称（需目标机器安装该字体，docx4j 不内嵌字体）
 *
 * @author wangtao
 * Created at 2026-11-21
 */
public class WordDocumentCreator implements DocumentCreator {

    private static final WordDocumentCreator INSTANCE = new WordDocumentCreator();

    private WordDocumentCreator() {

    }

    /**
     * @return 单例实例
     */
    public static WordDocumentCreator getInstance() {
        return INSTANCE;
    }

    @Override
    public void create(String templatePath, Map<String, Object> data, OutputStream out) {
        createWithWatermark(templatePath, data, null, out);
    }

    /**
     * 会忽略水印
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void createWithWatermark(String templatePath, Map<String, Object> data, String watermarkText, OutputStream out) {
        String html = ThymeleafUtils.eval(templatePath, data);
        try {
            // 空白 docx 包，列表编号支持需挂载默认 numbering 定义
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            // 针对<ul>以及<ol>定义编号规则
            NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
            wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
            ndp.unmarshalDefaultNumbering();

            // html 转换为 docx 内容元素并挂到主文档
            XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
            wordMLPackage.getMainDocumentPart().getContent()
                    .addAll(importer.convert(html, null));

            // 序列化输出；write 只写不关闭，out 的关闭由调用方负责
            wordMLPackage.save(out);
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
    }
}
