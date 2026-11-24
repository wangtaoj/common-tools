package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ThymeleafUtils;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblGrid;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;

import javax.xml.bind.JAXBElement;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 docx4j 的 Word 文档创建器：html 转 docx（Open XML），生成真正的 Word 文档
 * <p>html 由 {@link ThymeleafUtils} 渲染，再经 docx4j 的 XHTML importer 转换，
 * 支持 h1-h6、段落、表格（含样式）、图片（含 base64 data URI）、列表等
 * <p>注册到 fonts 目录的字体（见 {@link FontRegistrar}）在模板 CSS font-family 中指定后，
 * docx 内会引用对应字体名称（需目标机器安装该字体，docx4j 不内嵌字体）
 *
 * @author wangtao
 * Created at 2026-11-21
 */
@Slf4j
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
        try {
            // 空白 docx 包，列表编号支持需挂载默认 numbering 定义
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            // 针对<ul>以及<ol>定义编号规则
            NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
            wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
            ndp.unmarshalDefaultNumbering();

            // html 转换为 docx 内容元素并挂到主文档
            XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
            wordMLPackage.getMainDocumentPart().getContent()
                    .addAll(importer.convert(html, null));

            // 修正固定布局表格的列宽（docx4j 按自身布局宽度计算，可能超出 Word 页面内容宽）
            normalizeFixedTableWidths(wordMLPackage);

            // 序列化输出；write 只写不关闭，out 的关闭由调用方负责
            wordMLPackage.save(out);
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
    }

    /**
     * 修正固定布局（table-layout: fixed）表格的列宽：
     * docx4j 按自身布局宽度计算列宽，生成的 tblGrid 可能超出 Word 页面内容宽，
     * 而 Word 对固定布局表格严格按 tblGrid 渲染，导致表格溢出页面不可见。
     * <p>此处将超出内容宽的固定布局表格按比例缩放到页面内容宽（保留列宽比例）
     */
    private void normalizeFixedTableWidths(WordprocessingMLPackage wordMLPackage) {
        SectPr sectPr = wordMLPackage.getMainDocumentPart().getJaxbElement().getBody().getSectPr();
        int contentWidth = getContentWidth(sectPr);
        if (contentWidth <= 0) {
            return;
        }
        for (Object content : wordMLPackage.getMainDocumentPart().getContent()) {
            normalizeTable(content, contentWidth);
        }
    }

    /**
     * 页面内容宽度（twips）= 页面宽度 - 左右页边距
     */
    private int getContentWidth(SectPr sectPr) {
        if (sectPr == null || sectPr.getPgSz() == null || sectPr.getPgSz().getW() == null) {
            return 0;
        }
        int width = sectPr.getPgSz().getW().intValue();
        SectPr.PgMar pageMar = sectPr.getPgMar();
        if (pageMar != null) {
            if (pageMar.getLeft() != null) {
                width -= pageMar.getLeft().intValue();
            }
            if (pageMar.getRight() != null) {
                width -= pageMar.getRight().intValue();
            }
        }
        return width;
    }

    /**
     * 处理单个内容对象中的表格（兼容 JAXBElement 包装）
     */
    private void normalizeTable(Object content, int contentWidth) {
        Object value = unwrap(content);
        if (value instanceof Tbl) {
            normalizeTable((Tbl) value, contentWidth);
        }
    }

    /**
     * 取出表格的行（docx4j 8.x 使用内容模型，Tr 在 content 列表中）
     */
    private List<Tr> getRows(Tbl tbl) {
        List<Tr> rows = new ArrayList<>();
        for (Object o : tbl.getContent()) {
            Object value = unwrap(o);
            if (value instanceof Tr) {
                rows.add((Tr) value);
            }
        }
        return rows;
    }

    /**
     * 取出行的单元格（docx4j 8.x 使用内容模型，Tc 在 content 列表中）
     */
    private List<Tc> getCells(Tr row) {
        List<Tc> cells = new ArrayList<>();
        for (Object o : row.getContent()) {
            Object value = unwrap(o);
            if (value instanceof Tc) {
                cells.add((Tc) value);
            }
        }
        return cells;
    }

    /**
     * 去除 JAXBElement 包装
     */
    private Object unwrap(Object content) {
        return content instanceof JAXBElement ? ((JAXBElement<?>) content).getValue() : content;
    }

    /**
     * 按比例缩放固定布局表格：tblGrid 列宽与各单元格 tcW 同步缩放，保持列宽比例
     */
    private void normalizeTable(Tbl tbl, int contentWidth) {
        // 先处理单元格内嵌套的表格
        List<Tr> rows = getRows(tbl);
        for (Tr row : rows) {
            for (Tc cell : getCells(row)) {
                if (cell.getContent() != null) {
                    for (Object cellContent : cell.getContent()) {
                        normalizeTable(cellContent, contentWidth);
                    }
                }
            }
        }
        // 仅处理固定布局表格（自动布局由 Word 自行适配，无需处理）
        if (tbl.getTblPr() == null || tbl.getTblPr().getTblLayout() == null
                || !"fixed".equals(tbl.getTblPr().getTblLayout().getType().value())) {
            return;
        }
        TblGrid grid = tbl.getTblGrid();
        if (grid == null || grid.getGridCol().isEmpty()) {
            return;
        }
        int totalWidth = 0;
        for (TblGridCol gridCol : grid.getGridCol()) {
            if (gridCol.getW() != null) {
                totalWidth += gridCol.getW().intValue();
            }
        }
        if (totalWidth <= contentWidth) {
            return;
        }
        double ratio = (double) contentWidth / totalWidth;
        // 逐列按比例缩放（向下取整），余量补到最后一列，避免四舍五入后再次超出内容宽
        List<TblGridCol> gridCols = grid.getGridCol();
        int[] colWidths = new int[gridCols.size()];
        int scaledTotal = 0;
        for (int i = 0; i < gridCols.size(); i++) {
            TblGridCol gridCol = gridCols.get(i);
            if (gridCol.getW() == null) {
                continue;
            }
            int scaled = i == gridCols.size() - 1
                    ? contentWidth - scaledTotal
                    : (int) Math.floor(gridCol.getW().longValue() * ratio);
            scaled = Math.max(1, scaled);
            gridCol.setW(BigInteger.valueOf(scaled));
            colWidths[i] = scaled;
            scaledTotal += scaled;
        }
        // 单元格宽度直接取缩放后的网格列宽：colspan 即连续多列之和，
        // 不再按比例二次换算，保证 tcW 与 tblGrid 完全一致
        for (Tr row : rows) {
            int colIndex = 0;
            for (Tc cell : getCells(row)) {
                int span = getGridSpan(cell);
                if (cell.getTcPr() != null && cell.getTcPr().getTcW() != null) {
                    // colspan 单元格宽度为所跨列宽之和；统一为 dxa（docx4j 对 colspan 单元格写的是 auto）
                    int cellWidth = sumColumnWidths(colWidths, colIndex, span);
                    TblWidth tcW = cell.getTcPr().getTcW();
                    tcW.setType(TblWidth.TYPE_DXA);
                    tcW.setW(BigInteger.valueOf(Math.max(1, cellWidth)));
                }
                colIndex += span;
            }
        }
        log.debug("固定布局表格宽度 {} 超出页面内容宽 {}，已按比例缩放", totalWidth, contentWidth);
    }

    /**
     * 单元格跨列数（colspan），默认 1
     */
    private int getGridSpan(Tc cell) {
        if (cell.getTcPr() != null && cell.getTcPr().getGridSpan() != null
                && cell.getTcPr().getGridSpan().getVal() != null) {
            return cell.getTcPr().getGridSpan().getVal().intValue();
        }
        return 1;
    }

    /**
     * 从起始列开始累加 span 列的宽度
     */
    private int sumColumnWidths(int[] colWidths, int start, int span) {
        int sum = 0;
        for (int i = start; i < Math.min(start + span, colWidths.length); i++) {
            sum += colWidths[i];
        }
        return sum;
    }
}
