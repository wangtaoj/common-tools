package com.wangtao.tools.document;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 基于 openhtmltopdf 的 PDF 文档创建器
 * <p>流程：Thymeleaf 渲染 html 模板 -> openhtmltopdf 布局并输出 PDF -> PDFBox 后处理绘制水印与页码
 * <p>与 flying-saucer 实现相比，openhtmltopdf 的 CSS 支持更完整（如 @page 边距盒、更规范的布局），
 * 字体通过 {@link FontRegistrar} 注册到渲染器，水印与页码用 PDFBox 统一绘制
 *
 * @author wangtao
 * Created at 2026-11-23
 */
public class OpenHtmlToPdfDocumentCreator implements DocumentCreator {

    /**
     * 水印/页码字体文件（fonts 目录内）
     */
    private static final String TEXT_FONT = FontRegistrar.FONT_DIR + "LXGWWenKai-Regular.ttf";

    /**
     * 水印文字大小
     */
    private static final float WATERMARK_FONT_SIZE = 36.0f;

    /**
     * 水印透明度（0.0f - 1.0f）
     */
    private static final float WATERMARK_OPACITY = 0.1f;

    /**
     * 水印旋转角度（度）
     */
    private static final double WATERMARK_ROTATION = 30;

    /**
     * 水印水平间距
     */
    private static final float WATERMARK_X_STEP = 200.0f;

    /**
     * 水印垂直间距
     */
    private static final float WATERMARK_Y_STEP = 150.0f;

    /**
     * 页码文字大小
     */
    private static final float PAGE_NUMBER_FONT_SIZE = 9.0f;

    /**
     * 页码底部边距
     */
    private static final float PAGE_NUMBER_BOTTOM_MARGIN = 24.0f;

    private static final OpenHtmlToPdfDocumentCreator INSTANCE = new OpenHtmlToPdfDocumentCreator();

    private OpenHtmlToPdfDocumentCreator() {

    }

    /**
     * @return 单例实例
     */
    public static OpenHtmlToPdfDocumentCreator getInstance() {
        return INSTANCE;
    }

    @Override
    public void create(String templatePath, Map<String, Object> data, OutputStream out) {
        createWithWatermark(templatePath, data, null, out);
    }

    @Override
    public void createWithWatermark(String templatePath, Map<String, Object> data, String watermarkText, OutputStream out) {
        String html = ThymeleafUtils.eval(templatePath, data);
        ByteArrayOutputStream rendered = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            registerFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(rendered);
            builder.run();
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
        // PDFBox 后处理：绘制水印（可选）与页码
        try (PDDocument document = PDDocument.load(rendered.toByteArray())) {
            decorate(document, watermarkText);
            document.save(out);
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
    }

    /**
     * 将 fonts 目录下的字体注册到 openhtmltopdf，模板 CSS font-family 写字体族名即可命中
     */
    private void registerFonts(PdfRendererBuilder builder) {
        for (String fileName : FontRegistrar.getFontFiles()) {
            String fontFamily = FontRegistrar.getFontFamilyName(fileName);
            FSSupplier<InputStream> supplier = () -> Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(FontRegistrar.FONT_DIR + fileName);
            builder.useFont(supplier, fontFamily);
        }
    }

    /**
     * 为每一页绘制水印与页码
     */
    private void decorate(PDDocument document, String watermarkText) throws IOException {
        PDType0Font font = loadFont(document);
        int totalPages = document.getNumberOfPages();
        int pageNumber = 0;
        for (PDPage page : document.getPages()) {
            pageNumber++;
            try (PDPageContentStream cs = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                if (StringUtils.isNotBlank(watermarkText)) {
                    drawWatermark(cs, font, page, watermarkText);
                }
                drawPageNumber(cs, font, page, pageNumber, totalPages);
            }
        }
    }

    /**
     * 加载内嵌 CJK 字体（PDFBox 子集化嵌入，任何阅读器均可显示中文）
     */
    private PDType0Font loadFont(PDDocument document) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(TEXT_FONT)) {
            if (in == null) {
                throw new IOException("字体文件不存在: " + TEXT_FONT);
            }
            return PDType0Font.load(document, in);
        }
    }

    /**
     * 平铺绘制半透明旋转水印
     */
    private void drawWatermark(PDPageContentStream cs, PDType0Font font,
                               PDPage page, String watermarkText) throws IOException {
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(WATERMARK_OPACITY);
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        cs.saveGraphicsState();
        cs.setGraphicsStateParameters(gs);
        cs.setNonStrokingColor(Color.GRAY);
        cs.beginText();
        cs.setFont(font, WATERMARK_FONT_SIZE);
        for (float x = 0; x < pageWidth + WATERMARK_X_STEP; x += WATERMARK_X_STEP) {
            for (float y = 0; y < pageHeight + WATERMARK_Y_STEP; y += WATERMARK_Y_STEP) {
                cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(WATERMARK_ROTATION), x, y));
                cs.showText(watermarkText);
            }
        }
        cs.endText();
        cs.restoreGraphicsState();
    }

    /**
     * 页面底部居中绘制 "第 x 页 / 共 y 页"
     */
    private void drawPageNumber(PDPageContentStream cs, PDType0Font font,
                                PDPage page, int pageNumber, int totalPages) throws IOException {
        String text = String.format("第 %d 页 / 共 %d 页", pageNumber, totalPages);
        float textWidth = font.getStringWidth(text) / 1000 * PAGE_NUMBER_FONT_SIZE;
        float x = (page.getMediaBox().getWidth() - textWidth) / 2;
        cs.beginText();
        cs.setFont(font, PAGE_NUMBER_FONT_SIZE);
        cs.setNonStrokingColor(new Color(0.3f, 0.3f, 0.3f));
        cs.newLineAtOffset(x, PAGE_NUMBER_BOTTOM_MARGIN);
        cs.showText(text);
        cs.endText();
    }
}
