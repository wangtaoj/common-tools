package com.wangtao.tools.util;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.Writer;
import java.util.Map;

/**
 * Thymeleaf 模板渲染工具类
 *
 * @author wangtao
 * Created at 2026-11-20
 */
public final class ThymeleafUtils {

    private static final TemplateEngine ENGINE;

    static {
        // 从 classpath 的 template 目录加载 html 模板
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("template/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");

        ENGINE = new TemplateEngine();
        ENGINE.setTemplateResolver(templateResolver);
        // 注册 JSR310 方言，模板中可通过 #temporals 格式化 LocalDate/LocalDateTime 等
        ENGINE.addDialect(new Java8TimeDialect());
    }

    private ThymeleafUtils() {

    }

    /**
     * 渲染html模板
     * @param templatePath html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data 模板数据
     * @return html
     */
    public static String eval(String templatePath, Map<String, Object> data) {
        Context context = new Context();
        if (data != null) {
            context.setVariables(data);
        }
        return ENGINE.process(templatePath, context);
    }

    /**
     * 渲染html模板
     * @param templatePath html模板路径（相对 classpath 的 template 目录，不含前缀与 .html 后缀）
     * @param data 模板数据
     * @param writer 输出流
     */
    public static void eval(String templatePath, Map<String, Object> data, Writer writer) {
        Context context = new Context();
        if (data != null) {
            context.setVariables(data);
        }
        ENGINE.process(templatePath, context, writer);
    }
}
