package com.wangtao.tools.document;

import com.lowagie.text.pdf.BaseFont;
import com.wangtao.tools.exception.ToolRuntimeException;
import com.wangtao.tools.util.ClassPathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.xhtmlrenderer.pdf.ITextFontResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 字体注册器，注册 classpath fonts 目录下的字体文件到 flying-saucer 字体解析器
 * <p>html 模板 CSS font-family 写字体族名（从字体内部 name 表读取，如 "LXGW WenKai"）即可命中
 * <p>使用 openpdf 的相对路径加载机制：文件系统不可读时自动回退从 classpath 查找，
 * jar 包部署与本地文件系统均兼容，无需读取字体文件到内存或写临时文件；
 * BaseFont 实例由 openpdf 内部缓存管理
 *
 * @author wangtao
 * Created at 2026-11-20
 */
@Slf4j
public final class FontRegistrar {

    /**
     * classpath 字体目录
     */
    public static final String FONT_DIR = "fonts/";

    /**
     * 支持的字体文件扩展名
     */
    private static final String[] FONT_SUFFIXES = {".ttf", ".otf", ".ttc"};

    /**
     * fonts 目录下的字体文件名缓存，避免每次注册都扫描目录；
     * volatile 保证初始化完成后对其它线程立即可见，列表仅初始化时写入一次，之后只读
     */
    private static volatile List<String> FONT_FILES;

    private FontRegistrar() {

    }

    /**
     * 将 fonts 目录下的字体文件注册到 flying-saucer 字体解析器
     * @param resolver flying-saucer 字体解析器
     */
    public static void registerTo(ITextFontResolver resolver) {
        List<String> fontFiles = getFontFiles();
        if (CollectionUtils.isEmpty(fontFiles)) {
            return;
        }
        for (String fileName : fontFiles) {
            try {
                // 相对路径由 openpdf 自动回退 classpath 加载，jar 包与本地文件系统均兼容；
                // 同一 resolver 重复注册同一文件时 openpdf 内部按族名覆盖，BaseFont 命中其内部缓存
                resolver.addFont(FONT_DIR + fileName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                log.debug("注册字体到 flying-saucer: {}", fileName);
            } catch (Exception e) {
                throw new ToolRuntimeException(e);
            }
        }
    }

    /**
     * 获取 fonts 目录下的字体文件名（首次调用时扫描目录并缓存）
     */
    private static List<String> getFontFiles() {
        if (FONT_FILES == null) {
            synchronized (FontRegistrar.class) {
                if (FONT_FILES == null) {
                    FONT_FILES = listFontFiles();
                }
            }
        }
        return FONT_FILES;
    }

    /**
     * 扫描 classpath fonts 目录下所有字体文件名，过滤出字体文件
     */
    private static List<String> listFontFiles() {
        List<String> fontFiles = new ArrayList<>();
        for (String fileName : ClassPathUtils.listFileNames(FONT_DIR)) {
            if (isFontFile(fileName)) {
                fontFiles.add(fileName);
            }
        }
        return fontFiles;
    }

    /**
     * 判断是否为支持的字体文件
     */
    private static boolean isFontFile(String name) {
        String lower = name.toLowerCase();
        for (String suffix : FONT_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
