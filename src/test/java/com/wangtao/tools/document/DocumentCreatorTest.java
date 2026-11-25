package com.wangtao.tools.document;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
    public void testCreatePdfWithBaseUri() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "相对图片测试");
        // 模板中 img 使用相对路径 imgtest.jpg，由 baseUri 解析
        String baseUri = Paths.get("src/test/resources/template").toUri().toString();
        for (DocumentType type : new DocumentType[]{DocumentType.PDF, DocumentType.OPENHTMLTOPDF}) {
            Path path = OUTPUT_DIR.resolve("pdf-baseuri-" + type.name().toLowerCase() + ".pdf");
            try (OutputStream out = new FileOutputStream(path.toFile())) {
                DocumentCreator.getInstance(type).create("baseuri-test", data, baseUri, null, out);
            }
            verifyPdf(path);
            // 校验相对路径图片已被嵌入
            String raw = new String(Files.readAllBytes(path), StandardCharsets.ISO_8859_1);
            Assertions.assertTrue(raw.contains("/Subtype/Image") || raw.contains("/Subtype /Image"),
                    type + " 未嵌入相对路径图片");
        }
    }

    @Test
    public void testCreatePdfWithWatermark() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("pdf-test-watermark.pdf");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.PDF)
                    .create("pdf-test", data, "机密文件", out);
        }
        verifyPdf(path);
        log.info("水印版 PDF 写入: {}", path.toAbsolutePath());
    }

    @Test
    public void testCreateOpenHtmlToPdf() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("pdf-openhtml.pdf");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.OPENHTMLTOPDF).create("pdf-test", data, out);
        }
        verifyPdf(path);
    }

    @Test
    public void testCreateOpenHtmlToPdfWithWatermark() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("pdf-openhtml-watermark.pdf");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.OPENHTMLTOPDF)
                    .create("pdf-test", data, "机密文件", out);
        }
        verifyPdf(path);
        log.info("openhtmltopdf 水印版 PDF 写入: {}", path.toAbsolutePath());
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
    public void testCreateWordFixedTable() throws Exception {
        Map<String, Object> data = data();
        Path path = OUTPUT_DIR.resolve("word-fixed-table.docx");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(DocumentType.WORD).create("word-fixed-table", data, out);
        }
        // 固定布局表格的列宽合计不应超过页面内容宽（A4 11907 - 左右边距 1440*2 = 9027 twips），
        // 超出时 Word 会把表格渲染到页面外
        org.docx4j.openpackaging.packages.WordprocessingMLPackage pkg =
                org.docx4j.openpackaging.packages.WordprocessingMLPackage.load(path.toFile());
        org.docx4j.wml.Tbl tbl = findFirstTable(pkg);
        Assertions.assertNotNull(tbl, "未找到表格");
        int gridTotal = 0;
        for (org.docx4j.wml.TblGridCol col : tbl.getTblGrid().getGridCol()) {
            gridTotal += col.getW().intValue();
        }
        Assertions.assertTrue(gridTotal <= 9027, "固定布局表格宽度超出页面内容宽: " + gridTotal);
    }

    /**
     * 取文档中第一个表格
     */
    private org.docx4j.wml.Tbl findFirstTable(org.docx4j.openpackaging.packages.WordprocessingMLPackage pkg) {
        for (Object content : pkg.getMainDocumentPart().getContent()) {
            Object value = content instanceof javax.xml.bind.JAXBElement
                    ? ((javax.xml.bind.JAXBElement<?>) content).getValue() : content;
            if (value instanceof org.docx4j.wml.Tbl) {
                return (org.docx4j.wml.Tbl) value;
            }
        }
        return null;
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
