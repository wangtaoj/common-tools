package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 伪 Word 文档创建器：将 html 内容包装进 OLE2 容器伪装成 .doc
 * <p>仅骗过文件类型检测，Word 打开时依赖其 HTML 导入转换器渲染，
 * 兼容性差（新版 Word 常提示文件损坏，第三方软件基本不认），
 * 正式场景请使用基于 docx4j 的 {@link WordDocumentCreator}
 *
 * @author wangtao
 * Created at 2026-11-21
 */
public class FakeWordDocumentCreator implements DocumentCreator {

    private static final FakeWordDocumentCreator INSTANCE = new FakeWordDocumentCreator();

    private FakeWordDocumentCreator() {

    }

    /**
     * @return 单例实例
     */
    public static FakeWordDocumentCreator getInstance() {
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
