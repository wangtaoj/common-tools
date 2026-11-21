package com.wangtao.tools.document;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
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
     * 水印文字字体文件（fonts 目录内），以内嵌方式写入 PDF，任何阅读器均可显示
     */
    private static final String WATERMARK_FONT = FontRegistrar.FONT_DIR + "LXGWWenKai-Regular.ttf";

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
        // 自动注册 fonts 目录下的字体，模板 CSS font-family 写字体族名即可命中；
        // 相对路径由 openpdf 自动回退 classpath 加载，jar 包与本地文件系统均兼容
        FontRegistrar.registerTo(renderer.getFontResolver());
        renderer.setDocumentFromString(html, null);
        renderer.layout();
        boolean hasWatermark = StringUtils.isNotBlank(watermarkText);
        // 水印或页码均需要页面事件监听器，页面布局完成后页数已确定，
        // 在写入页面前通过 PdfWriter 底层 API 在每一页绘制
        if (hasWatermark) {
            renderer.setListener(new PageEventCreationListener(
                    new WatermarkPageEvent(watermarkText), new PageNumberPageEvent()));
        } else {
            renderer.setListener(new PageEventCreationListener(new PageNumberPageEvent()));
        }
        try {
            renderer.createPDF(out);
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
    }

    /**
     * 在 preWrite 时机拿到 PdfWriter，注册页面事件监听器。
     */
    private static class PageEventCreationListener implements PDFCreationListener {

        private final PdfPageEventHelper[] pageEvents;

        PageEventCreationListener(PdfPageEventHelper... pageEvents) {
            this.pageEvents = pageEvents;
        }

        @Override
        public void preOpen(ITextRenderer iTextRenderer) {

        }

        @Override
        public void preWrite(ITextRenderer iTextRenderer, int pageCount) {
            PdfWriter writer = iTextRenderer.getOutputDevice().getWriter();
            // openpdf 单一 setPageEvent，把多个事件合并为一个复合事件
            writer.setPageEvent(new CompositePageEvent(pageEvents));
        }

        @Override
        public void onClose(ITextRenderer renderer) {

        }
    }

    /**
     * 复合页面事件，把 openpdf 的各生命周期回调依次分发给多个子事件
     */
    private static class CompositePageEvent extends PdfPageEventHelper {

        private final PdfPageEventHelper[] pageEvents;

        CompositePageEvent(PdfPageEventHelper[] pageEvents) {
            this.pageEvents = pageEvents;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            for (PdfPageEventHelper pageEvent : pageEvents) {
                pageEvent.onOpenDocument(writer, document);
            }
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            for (PdfPageEventHelper pageEvent : pageEvents) {
                pageEvent.onStartPage(writer, document);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            for (PdfPageEventHelper pageEvent : pageEvents) {
                pageEvent.onEndPage(writer, document);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            for (PdfPageEventHelper pageEvent : pageEvents) {
                pageEvent.onCloseDocument(writer, document);
            }
        }
    }

    /**
     * openpdf 页面事件，在每页结束时绘制平铺水印文字
     */
    private static class WatermarkPageEvent extends PdfPageEventHelper {

        private final String watermarkText;

        WatermarkPageEvent(String watermarkText) {
            this.watermarkText = watermarkText;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                BaseFont font = BaseFont.createFont(WATERMARK_FONT,
                        BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
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
                        canvas.showTextAligned(Element.ALIGN_LEFT, watermarkText, x, y,
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

    /**
     * openpdf 页面事件，在每页底部居中绘制 "当前页 / 总页数"
     * <p>总页数在 onEndPage 时可能还未可知，利用 onEndPage 先画占位，
     * onCloseDocument 时总页数确定，用 PdfTemplate 模板回填
     */
    private static class PageNumberPageEvent extends PdfPageEventHelper {

        /**
         * 页码文字字体文件（fonts 目录内），页码含中文（"第 x 页 / 共 y 页"）
         */
        private static final String FONT_NAME = FontRegistrar.FONT_DIR + "LXGWWenKai-Regular.ttf";

        /**
         * 页码文字大小
         */
        private static final float FONT_SIZE = 9.0f;

        /**
         * 页码底部边距
         */
        private static final float BOTTOM_MARGIN = 24.0f;

        /**
         * 总页数占位模板，onCloseDocument 时回填
         */
        private PdfTemplate totalPageTemplate;

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                // 页面事件在 preWrite 阶段才注册，onOpenDocument 已错过，模板需在此惰性创建
                if (totalPageTemplate == null) {
                    totalPageTemplate = PdfTemplate.createTemplate(writer, 50, 16);
                }
                BaseFont font = BaseFont.createFont(FONT_NAME,
                        BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                PdfContentByte canvas = writer.getDirectContent();
                canvas.saveState();
                canvas.beginText();
                canvas.setFontAndSize(font, FONT_SIZE);
                canvas.setColorFill(new GrayColor(0.3f));
                float x = document.getPageSize().getWidth() / 2;
                float y = BOTTOM_MARGIN;
                // "第 x 页 / 共"
                String currentText = String.format("第 %d 页 / 共 ", document.getPageNumber());
                // 文字右端对齐到页面中心，右侧紧跟总页数模板
                canvas.showTextAligned(Element.ALIGN_RIGHT, currentText, x, y, 0);
                canvas.addTemplate(totalPageTemplate, x, y);
                canvas.endText();
                canvas.restoreState();
            } catch (Exception e) {
                throw new ToolRuntimeException(e);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            try {
                BaseFont font = BaseFont.createFont(FONT_NAME,
                        BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                // 文档即将关闭时页码为最后一页，即总页数
                String text = String.format("%d 页", document.getPageNumber() - 1);
                totalPageTemplate.beginText();
                totalPageTemplate.setFontAndSize(font, FONT_SIZE);
                totalPageTemplate.setColorFill(new GrayColor(0.3f));
                totalPageTemplate.showTextAligned(Element.ALIGN_LEFT, text, 0, 0, 0);
                totalPageTemplate.endText();
            } catch (Exception e) {
                throw new ToolRuntimeException(e);
            }
        }
    }
}
