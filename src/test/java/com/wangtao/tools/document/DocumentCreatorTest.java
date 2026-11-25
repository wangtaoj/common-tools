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

    /**
     * 验证字体、表格、图片、水印（一次生成所有类型）
     */
    @Test
    public void testCreate() throws Exception {
        for (DocumentType type : DocumentType.values()) {
            verify(type, create(type));
        }
        // html 伪装 word 方式（独立实现，不在工厂枚举中）
        Path fakeWord = OUTPUT_DIR.resolve("create-fakeword.doc");
        try (OutputStream out = new FileOutputStream(fakeWord.toFile())) {
            FakeWordDocumentCreator.getInstance().create("pdf-test", data(), out);
        }
        log.info("FAKE_WORD 文档写入: {}", fakeWord.toAbsolutePath());
    }

    /**
     * 单独生成 HTML 文档
     */
    @Test
    public void testCreateHtml() throws Exception {
        create(DocumentType.HTML);
    }

    /**
     * 单独生成 WORD 文档
     */
    @Test
    public void testCreateWord() throws Exception {
        create(DocumentType.WORD);
    }

    /**
     * 单独生成 flying-saucer PDF 文档
     */
    @Test
    public void testCreatePdf() throws Exception {
        create(DocumentType.PDF);
    }

    /**
     * 单独生成 openhtmltopdf PDF 文档
     */
    @Test
    public void testCreateOpenHtmlToPdf() throws Exception {
        create(DocumentType.OPENHTMLTOPDF);
    }

    /**
     * word 固定布局表格：列宽修正的专项测试（仅 word 存在该问题）
     */
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
     * 生成指定类型的文档并写入 target 目录，便于按类型单独运行
     */
    private Path create(DocumentType type) throws Exception {
        Path path = OUTPUT_DIR.resolve("create-" + type.name().toLowerCase() + extension(type));
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            DocumentCreator.getInstance(type).create("pdf-test", data(), baseUri(), "机密文件", out);
        }
        log.info("{} 文档写入: {}", type, path.toAbsolutePath());
        return path;
    }

    /**
     * 按文档类型校验生成结果
     */
    private void verify(DocumentType type, Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        Assertions.assertTrue(bytes.length > 0, type + " 输出为空");
        switch (type) {
            case PDF:
            case OPENHTMLTOPDF:
                Assertions.assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.ISO_8859_1));
                break;
            case WORD:
                // docx 为 zip 格式
                Assertions.assertEquals("PK", new String(bytes, 0, 2, StandardCharsets.ISO_8859_1));
                break;
            case HTML:
                Assertions.assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("企业客户报告"));
                break;
            default:
                break;
        }
    }

    /**
     * 各文档类型的输出文件扩展名
     */
    private String extension(DocumentType type) {
        switch (type) {
            case HTML:
                return ".html";
            case WORD:
                return ".docx";
            default:
                return ".pdf";
        }
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
        // 第一张图片使用 base64 data URI 内嵌
        data.put("surroundingImage", imageToBase64DataUrl("imgtest.jpg"));
        // 第二张使用相对路径（由 baseUri 解析）
        data.put("surroundingImage2", "imgtest.jpg");
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

    /**
     * 模板中"企业周边环境"的第二张图片使用相对路径 imgtest.jpg，由 baseUri 解析
     */
    private String baseUri() {
        return Paths.get("src/test/resources/template").toUri().toString();
    }
}
