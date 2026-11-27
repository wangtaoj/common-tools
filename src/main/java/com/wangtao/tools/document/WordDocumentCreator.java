package com.wangtao.tools.document;

import com.wangtao.tools.exception.ToolRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.STHint;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.TblGrid;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner.TcBorders;
import org.docx4j.wml.Tr;

import javax.xml.bind.JAXBElement;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 docx4j 的 Word 文档创建器：html 转 docx（Open XML），生成真正的 Word 文档
 * <p>html 由 {@link AbstractDocumentCreator} 渲染，再经 docx4j 的 XHTML importer 转换，
 * 支持 h1-h6、段落、表格（含样式）、图片（含 base64 data URI）、列表等
 * <p>在模板 CSS font-family 中指定后，docx 内会引用对应字体名称（需目标机器安装该字体，docx4j 不内嵌字体）
 *
 * @author wangtao
 * Created at 2026-11-21
 */
@Slf4j
public class WordDocumentCreator extends AbstractDocumentCreator {

    /**
     * 常用中文字体（Windows / macOS
     */
    private static final String[] COMMON_FONT_FAMILIES = {
            // Windows 宋体（含别名、新宋体）
            "SimSun", "宋体", "NSimSun", "新宋体",
            // Windows 微软雅黑
            "Microsoft YaHei", "微软雅黑",
            // 其它常用 Windows 中文字体
            "SimHei", "黑体", "KaiTi", "楷体", "FangSong", "仿宋", "DengXian", "等线",
            // macOS 宋体
            "Songti SC", "STSong", "华文宋体",
            // 本项目内置默认字体（classpath fonts 目录）
            "LXGW WenKai", "霞鹜文楷"
    };

    private static final WordDocumentCreator INSTANCE = new WordDocumentCreator();

    private WordDocumentCreator() {
        /*
         * 注册字体映射
         * XHTMLImporterImpl 在解析 CSS font-family 时，会按顺序去内部的 fontFamilyToFont 映射表中查找对应的 Word RFonts 对象
         */
        for (String family : COMMON_FONT_FAMILIES) {
            RFonts rFonts = Context.getWmlObjectFactory().createRFonts();
            rFonts.setAscii(family);
            rFonts.setHAnsi(family);
            // 中文走 eastAsia
            rFonts.setEastAsia(family);
            // 提示 Word 对模糊字符（如标点）优先使用东亚字体
            rFonts.setHint(STHint.EAST_ASIA);
            // 注意这里线程不安全, 底层是静态的hashmap
            XHTMLImporterImpl.addFontMapping(family, rFonts);
        }
    }

    /**
     * @return 单例实例
     */
    public static WordDocumentCreator getInstance() {
        return INSTANCE;
    }

    /**
     * @return 当前创建器对应的文档类型
     */
    @Override
    protected DocumentType documentType() {
        return DocumentType.WORD;
    }

    /**
     * 会忽略水印
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void create(String templatePath, Map<String, Object> data, String baseUri, String watermarkText, OutputStream out) {
        String html = eval(templatePath, data);
        try {
            // 空白 docx 包(A4大小, 页边距为1英寸)，列表编号支持需挂载默认 numbering 定义
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            // 针对<ul>以及<ol>定义编号规则
            NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
            wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
            ndp.unmarshalDefaultNumbering();

            // html 转换为 docx 内容元素并挂到主文档
            XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
            wordMLPackage.getMainDocumentPart().getContent()
                    .addAll(importer.convert(html, baseUri));

            // 修正表格：统一单元格边框到表级（兼容 WPS 渲染）、修正固定布局表格列宽
            normalizeTables(wordMLPackage);

            // 序列化输出；write 只写不关闭，out 的关闭由调用方负责
            wordMLPackage.save(out);
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
    }

    /**
     * 规范化表格结构：
     * <ol>
     *   <li>单元格边框统一时上移到表级 tblBorders——docx4j 给每个单元格都写 tcBorders，
     *       相邻单元格边框重叠，Word 会合并渲染而 WPS 会各自绘制，边框较粗时出现单元格间隙</li>
     *   <li>修正固定布局（table-layout: fixed）表格列宽——docx4j 按自身布局宽度计算列宽，
     *       tblGrid 可能超出 Word 页面内容宽，固定布局表格会溢出页面不可见；
     *       超出时按比例缩放到内容宽（保留列宽比例）</li>
     * </ol>
     */
    private void normalizeTables(WordprocessingMLPackage wordMLPackage) {
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
        // 单元格边框统一时上移到表级，避免 WPS 相邻单元格边框重复绘制产生间隙
        normalizeCellBorders(tbl);
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
     * 单元格边框上移到表级 tblBorders：
     * docx4j 为每个单元格写四边 tcBorders，相邻单元格边框位置重叠，
     * Word 按边框冲突规则合并为一条线，WPS 各自绘制，边框变粗后会出现单元格间隙。
     * <p>取出现次数最多的边框组合作为表级边框（外框 + 内部横竖线），
     * 仅移除与该组合一致的单元格边框，个别自定义边框的单元格保留自身 tcBorders 覆盖表级设置
     */
    private void normalizeCellBorders(Tbl tbl) {
        if (tbl.getTblPr() == null) {
            return;
        }
        // border-collapse: separate 时单元格间有间距（tblCellSpacing > 0），保持单元格边框不动
        if (tbl.getTblPr().getTblCellSpacing() != null
                && tbl.getTblPr().getTblCellSpacing().getW() != null
                && tbl.getTblPr().getTblCellSpacing().getW().intValue() > 0) {
            return;
        }
        List<Tr> rows = getRows(tbl);
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, TcBorders> borderGroups = new LinkedHashMap<>();
        for (Tr row : rows) {
            for (Tc cell : getCells(row)) {
                TcPr tcPr = cell.getTcPr();
                TcBorders borders = tcPr == null ? null : tcPr.getTcBorders();
                if (borders == null || borders.getTop() == null || borders.getLeft() == null
                        || borders.getBottom() == null || borders.getRight() == null) {
                    continue;
                }
                String key = borderKey(borders);
                counts.merge(key, 1, Integer::sum);
                borderGroups.putIfAbsent(key, borders);
            }
        }
        String majorityKey = null;
        int majorityCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > majorityCount) {
                majorityKey = entry.getKey();
                majorityCount = entry.getValue();
            }
        }
        // 至少两个单元格共享同一边框组合才值得上移
        if (majorityKey == null || majorityCount < 2) {
            return;
        }
        TcBorders reference = borderGroups.get(majorityKey);
        // 表级边框：外框取单元格四边，内部横线取上/下边框、内部竖线取左/右边框
        TblBorders tblBorders = Context.getWmlObjectFactory().createTblBorders();
        tblBorders.setTop(copyBorder(reference.getTop()));
        tblBorders.setLeft(copyBorder(reference.getLeft()));
        tblBorders.setBottom(copyBorder(reference.getBottom()));
        tblBorders.setRight(copyBorder(reference.getRight()));
        tblBorders.setInsideH(copyBorder(reference.getBottom()));
        tblBorders.setInsideV(copyBorder(reference.getRight()));
        tbl.getTblPr().setTblBorders(tblBorders);
        // 仅移除与多数组合一致的单元格边框，其它单元格保留自身设置覆盖表级边框
        for (Tr row : rows) {
            for (Tc cell : getCells(row)) {
                TcPr tcPr = cell.getTcPr();
                if (tcPr == null || tcPr.getTcBorders() == null) {
                    continue;
                }
                if (majorityKey.equals(borderKey(tcPr.getTcBorders()))) {
                    tcPr.setTcBorders(null);
                }
            }
        }
        // 零单元格间距显式移除，避免部分阅读器（WPS）将其解析为间隙
        if (tbl.getTblPr().getTblCellSpacing() != null
                && tbl.getTblPr().getTblCellSpacing().getW() != null
                && tbl.getTblPr().getTblCellSpacing().getW().intValue() == 0) {
            tbl.getTblPr().setTblCellSpacing(null);
        }
        log.debug("表格单元格边框已上移到表级 tblBorders（{}/{} 单元格）", majorityCount, counts.values().stream().mapToInt(Integer::intValue).sum());
    }

    /**
     * 边框组合的标识 key（四边样式、颜色、粗细）
     */
    private String borderKey(TcBorders borders) {
        return borderKey(borders.getTop()) + "|" + borderKey(borders.getLeft()) + "|"
                + borderKey(borders.getBottom()) + "|" + borderKey(borders.getRight());
    }

    /**
     * 单条边框的标识 key
     */
    private String borderKey(CTBorder border) {
        if (border == null) {
            return "null";
        }
        return border.getVal() + ":" + border.getColor() + ":" + border.getSz();
    }

    /**
     * 复制边框设置
     */
    private CTBorder copyBorder(CTBorder source) {
        CTBorder border = Context.getWmlObjectFactory().createCTBorder();
        border.setVal(source.getVal());
        border.setColor(source.getColor());
        border.setSz(source.getSz());
        border.setSpace(source.getSpace());
        return border;
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
