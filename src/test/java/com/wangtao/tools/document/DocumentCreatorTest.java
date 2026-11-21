package com.wangtao.tools.document;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2026-11-20
 */
@Slf4j
public class DocumentCreatorTest {

    /**
     * 生成的文档输出到项目根目录下的 target 目录，方便人工查看
     */
    private static final Path OUTPUT_DIR = Paths.get("target");

    @Test
    public void testCreatePdf() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("pdf-test.pdf");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.PDF).create("pdf-test", data, out);
        }
        verifyPdf(path);
    }

    @Test
    public void testCreatePdfWithWatermark() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("pdf-test-watermark.pdf");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.PDF)
                    .createWithWatermark("pdf-test", data, "机密文件", out);
        }
        verifyPdf(path);
        log.info("水印版 PDF 写入: {}", path.toAbsolutePath());
    }

    @Test
    public void testCreateWord() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("word-test.docx");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.WORD).create("pdf-test", data, out);
        }
        log.info("Word 写入: {}", path.toAbsolutePath());
    }

    @Test
    public void testCreateFakeWord() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("fakeWord-test.doc");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            // html伪装word
            FakeWordDocumentCreator.getInstance().create("pdf-test", data, out);
        }
        log.info("Word 写入: {}", path.toAbsolutePath());
    }

    @Test
    public void testCreateHtml() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("html-test.html");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.HTML).create("pdf-test", data, out);
        }
        log.info("HTML 写入: {}", path.toAbsolutePath());
    }

    @Test
    public void testOutputNull() {
        Assertions.assertThrows(Exception.class,
                () -> DocumentCreator.getInstance(DocumentType.PDF).create("pdf-test", data(), null));
    }

    /**
     * 校验生成的 pdf 文件：魔数与大小
     */
    private void verifyPdf(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        // PDF 文件魔数校验
        Assertions.assertEquals("%PDF", new String(bytes, 0, 4));
        Assertions.assertTrue(bytes.length > 0);
    }

    private Map<String, Object> data() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("reportTime", LocalDateTime.of(2026, 11, 21, 10, 0, 0));
        data.put("customerName", "张三");
        data.put("customerNo", "KH20261100001");
        data.put("certType", "统一社会信用代码");
        data.put("certNo", "91330100MA2XXXXXXX");
        data.put("companyName", "杭州某某科技有限公司");
        data.put("creditCode", "91330100MA2XXXXXXX");
        data.put("industry", "软件和信息技术服务业");
        data.put("companySize", "中型企业");
        data.put("registeredCapital", "5000万元人民币");
        data.put("foundDate", LocalDate.of(2016, 3, 18));
        data.put("businessStatus", "存续");
        data.put("businessYears", "10年");
        data.put("accountManager", "李四");
        data.put("contactPhone", "0571-88888888");
        data.put("customerLevel", "A级");
        data.put("addrInfo", "湖南省长沙市岳麓区金州大道583号");
        data.put("environmentDesc", "公司位于某某科技园区内，周边交通便捷，紧邻地铁2号线某某站，周边商业配套完善，企业聚集度高，营商环境良好。");
        data.put("environmentDesc2", "厂区外观整洁，绿化覆盖率高，办公与生产区域规划合理，无周边污染源，满足生产经营要求。");
        // 图片转 base64 内嵌（html 使用 data URI 格式）
        String imageBase64 = imageToBase64DataUrl("imgtest.jpg");
        data.put("surroundingImage", imageBase64);
        data.put("surroundingImage2", imageBase64);
        return data;
    }

    /**
     * 读取 template 目录下的图片并转为 base64 data URI
     */
    private String imageToBase64DataUrl(String fileName) throws Exception {
        Path imagePath = Paths.get("src/test/resources/template", fileName);
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/jpeg;base64," + base64;
    }
}
