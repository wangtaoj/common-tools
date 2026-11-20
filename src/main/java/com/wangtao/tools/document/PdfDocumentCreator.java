package com.wangtao.tools.document;

import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfWriter;
import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;
import org.apache.commons.lang3.StringUtils;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFCreationListener;

import java.io.OutputStream;
import java.util.Map;

/**
 * 基于 flying-saucer-pdf 的 PDF 文档创建器
 * <p>流程：Thymeleaf 渲染 html 模板 -> flying-saucer 将 html 布局并输出为 PDF
 *
 * @author wangtao
 * Created at 2026-11-20
 */
public class PdfDocumentCreator implements DocumentCreator {

    /**
     * 水印文字的字体，STSong-Light 为 openpdf 内置 CJK 字体，支持中文（不内嵌，依赖阅读器字体）
     */
    private static final String WATERMARK_FONT = "STSong-Light";

    private static final String WATERMARK_ENCODING = "UniGB-UCS2-H";

    /**
     * 水印文字大小
     */
    private static final float WATERMARK_FONT_SIZE = 36.0f;

    /**
     * 水印透明度（0.0f - 1.0f）
     */
    private static final float WATERMARK_OPACITY = 0.1f;

    /**
     * 水印旋转角度（弧度）
     */
    private static final float WATERMARK_ROTATION = 30;

    /**
     * 水印水平间距
     */
    private static final float WATERMARK_X_STEP = 200.0f;

    /**
     * 水印垂直间距
     */
    private static final float WATERMARK_Y_STEP = 150.0f;

    private static final PdfDocumentCreator INSTANCE = new PdfDocumentCreator();

    private PdfDocumentCreator() {

    }

    /**
     * @return 单例实例（饿汉式）
     */
    public static PdfDocumentCreator getInstance() {
        return INSTANCE;
    }

    @Override
    public void create(String templatePath, Map<String, Object> data, OutputStream out) {
        createWithWatermark(templatePath, data, out, null);
    }

    @Override
    public void createWithWatermark(String templatePath, Map<String, Object> data,
                                    OutputStream out, String watermarkText) {
        String html = ThymeleafUtils.eval(templatePath, data);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html, null);
        renderer.layout();
        // 水印文字不为空时，注册页面事件监听器给每一页绘制水印
        if (StringUtils.isNotBlank(watermarkText)) {
            // 页面布局完成后页数已确定，在写入页面前通过 PdfWriter 底层 API 绘制水印
            renderer.setListener(new WatermarkCreationListener(watermarkText));
        }
        try {
            renderer.createPDF(out);
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
    }

    /**
     * 在 preWrite 时机拿到 PdfWriter，注册页面事件监听器绘制水印。
     * 用 onEndPage 保证每一页都画，且画在 flying-saucer 渲染内容之上（同层直接绘制）。
     */
    private static class WatermarkCreationListener implements PDFCreationListener {

        private final String watermarkText;

        WatermarkCreationListener(String watermarkText) {
            this.watermarkText = watermarkText;
        }

        @Override
        public void preOpen(ITextRenderer iTextRenderer) {

        }

        @Override
        public void preWrite(ITextRenderer iTextRenderer, int pageCount) {
            PdfWriter writer = iTextRenderer.getOutputDevice().getWriter();
            writer.setPageEvent(new WatermarkPageEvent(watermarkText));
        }

        @Override
        public void onClose(ITextRenderer renderer) {

        }
    }

    /**
     * openpdf 页面事件，在每页结束时绘制平铺水印文字
     */
    private static class WatermarkPageEvent extends com.lowagie.text.pdf.PdfPageEventHelper {

        private final String watermarkText;

        WatermarkPageEvent(String watermarkText) {
            this.watermarkText = watermarkText;
        }

        @Override
        public void onEndPage(PdfWriter writer, com.lowagie.text.Document document) {
            try {
                BaseFont font = BaseFont.createFont(WATERMARK_FONT, WATERMARK_ENCODING,
                        BaseFont.NOT_EMBEDDED);
                PdfContentByte canvas = writer.getDirectContent();
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(WATERMARK_OPACITY);
                canvas.saveState();
                canvas.setGState(gs);
                canvas.beginText();
                canvas.setFontAndSize(font, WATERMARK_FONT_SIZE);
                canvas.setColorFill(new GrayColor(0.5f));
                float pageWidth = document.getPageSize().getWidth();
                float pageHeight = document.getPageSize().getHeight();
                // 平铺水印，覆盖整个页面
                for (float x = 0; x < pageWidth + WATERMARK_X_STEP; x += WATERMARK_X_STEP) {
                    for (float y = 0; y < pageHeight + WATERMARK_Y_STEP; y += WATERMARK_Y_STEP) {
                        canvas.showTextAligned(Element.ALIGN_CENTER, watermarkText, x, y,
                                WATERMARK_ROTATION);
                    }
                }
                canvas.endText();
                canvas.restoreState();
            } catch (Exception e) {
                throw new ToolRuntimeException(e);
            }
        }
    }
}
