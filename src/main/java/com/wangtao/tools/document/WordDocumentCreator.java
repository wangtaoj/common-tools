package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
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
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        try (InputStream is = new ByteArrayInputStream(bytes);
             POIFSFileSystem fs = new POIFSFileSystem()) {
            DirectoryNode root = fs.getRoot();
            root.createDocument("WordDocument", is);
            fs.writeFilesystem(out);
        } catch (IOException e) {
            throw new ToolRuntimeException(e);
        }
    }
}
