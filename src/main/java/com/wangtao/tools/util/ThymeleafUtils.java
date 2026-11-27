package com.wangtao.tools.util;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.expression.AbstractStandardConversionService;
import org.thymeleaf.standard.expression.StandardConversionService;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        String cacheFlg = System.getProperty("thymeleaf.cache.enabled", "true");
        // 开发期可通过jvm启动参数禁用模板缓存, 修改html模板文件后只需编译一下不用重启项目就能生效
        templateResolver.setCacheable(Boolean.parseBoolean(cacheFlg));

        StandardDialect standardDialect = new StandardDialect();
        // 设置自定义的类型转换服务
        standardDialect.setConversionService(new CustomConversionService());

        ENGINE = new TemplateEngine();
        ENGINE.setTemplateResolver(templateResolver);
        // 设置方言
        ENGINE.setDialect(standardDialect);
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

    /**
     * 自定义类型转换器, 增加LocalDateTime、LocalDate、LocalTime、Decimal的类型转换
     * 默认的{@link StandardConversionService}实现是通过调用toString方法进行转换
     * 在模板中使用${{}}语法就会进行类型转换, 将对象转成字符串
     * 默认的日期格式不满足要求时, 通过如下方法转换
     * ${#temporals.format(reportDate, 'yyyyMMdd')}
     */
    private static class CustomConversionService extends AbstractStandardConversionService {

        @Override
        protected String convertToString(IExpressionContext context, Object object) {
            if (object instanceof LocalDateTime) {
                return DateUtils.format((LocalDateTime) object);
            }
            if (object instanceof LocalDate) {
                return DateUtils.format((LocalDate) object);
            }
            if (object instanceof LocalTime) {
                return DateUtils.format((LocalTime) object);
            }
            if (object instanceof BigDecimal) {
                return ((BigDecimal) object).stripTrailingZeros().toPlainString();
            }
            return super.convertToString(context, object);
        }
    }
}
