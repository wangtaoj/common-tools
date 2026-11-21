package com.wangtao.tools.document;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2026-11-20
 */
@Slf4j
public class PdfDocumentCreatorTest {

    /**
     * 生成的 pdf 输出到项目根目录下的 target 目录，方便人工查看
     */
    private static final Path OUTPUT_DIR = Paths.get("target");

    private final DocumentCreator documentCreator = DocumentCreator.getInstance(DocumentType.PDF);

    @Test
    public void testCreate() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("pdf-test.pdf");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            documentCreator.create("pdf-test", data, out);
        }
        verifyPdf(path);
    }

    @Test
    public void testCreateWithWatermark() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("pdf-test-watermark.pdf");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            documentCreator.createWithWatermark("pdf-test", data, "机密文件", out);
        }
        verifyPdf(path);
        log.info("水印版 PDF 写入: {}", path.toAbsolutePath());
    }

    @Test
    public void testOutputNull() {
        Assertions.assertThrows(Exception.class,
                () -> documentCreator.create("pdf-test", data(), null));
    }

    /**
     * 校验生成的 pdf 文件：魔数与大小
     */
    private void verifyPdf(Path path) throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(path);
        // PDF 文件魔数校验
        Assertions.assertEquals("%PDF", new String(bytes, 0, 4));
        Assertions.assertTrue(bytes.length > 0);
    }

    private Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "PDF文档测试");
        data.put("name", "wangtao");
        data.put("localDateTime", LocalDateTime.of(2026, 11, 20, 10, 30, 45));
        return data;
    }
}
