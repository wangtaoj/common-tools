package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 伪 Word 文档创建器：将 html 内容包装进 OLE2 容器伪装成 .doc
 * <p>视图控制（页面视图/Web 视图）等 Word HTML 元数据由模板声明，
 * 见模板 head 内的 {@code <w:WordDocument>} 条件注释块
 * <p>仅骗过文件类型检测，Word 打开时依赖其 HTML 导入转换器渲染，
 * 兼容性差（新版 Word 常提示文件损坏，第三方软件基本不认），
 * 正式场景请使用基于 docx4j 的 {@link WordDocumentCreator}
 *
 * @author wangtao
 * Created at 2026-11-21
 */
public class FakeWordDocumentCreator extends AbstractDocumentCreator {

    private static final FakeWordDocumentCreator INSTANCE = new FakeWordDocumentCreator();

    private FakeWordDocumentCreator() {

    }

    /**
     * @return 单例实例
     */
    public static FakeWordDocumentCreator getInstance() {
        return INSTANCE;
    }

    /**
     * html 伪装 word 同样由 Word 打开，字体不内嵌，按 WORD 处理
     *
     * @return 当前创建器对应的文档类型
     */
    @Override
    protected DocumentType documentType() {
        return DocumentType.WORD;
    }

    /**
     * 会忽略水印与 baseUri（html 原样封装进 OLE2 容器，相对资源由 Word 打开时解析）
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void create(String templatePath, Map<String, Object> data, String baseUri, String watermarkText, OutputStream out) {
        String html = eval(templatePath, data);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        try (InputStream is = new ByteArrayInputStream(bytes);
             POIFSFileSystem fs = new POIFSFileSystem()) {
            fs.getRoot().createDocument("WordDocument", is);
            fs.writeFilesystem(out);
        } catch (IOException e) {
            throw new ToolRuntimeException(e);
        }
    }
}
