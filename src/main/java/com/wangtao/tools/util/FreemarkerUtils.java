package com.wangtao.tools.util;

import com.wangtao.tools.exception.ToolRuntimeException;
import freemarker.cache.ClassTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2026-11-20
 */
public final class FreemarkerUtils {

    private static final Configuration CFG;

    static {
        // 1. 初始化 FreeMarker 配置
        CFG = new Configuration(Configuration.VERSION_2_3_32);

        // 设置模板加载器 (从 classpath 的 template 目录加载)
        CFG.setTemplateLoader(new ClassTemplateLoader(ClassLoader.getSystemClassLoader(), "template"));

        // 设置默认编码
        CFG.setDefaultEncoding("UTF-8");

        CFG.setNumberFormat("computer");

        CFG.setDateFormat("yyyy-MM-dd");

        CFG.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");

        // 循环变量为null时直接返回，不要向更高层级作用域继续取值
        CFG.setFallbackOnNullLoopVariable(false);

        // 设置模板异常处理策略，直接抛出由调用方处理
        CFG.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        // 包装 UncheckedException，当获取用户参数发生异常时使用 TemplateException 包装，这样有助于...
        CFG.setWrapUncheckedExceptions(true);

        // 不要记录日志，异常会被直接抛出，由调用方处理
        CFG.setLogTemplateExceptions(false);

        // 禁止模板中实例化任何类，防止模板注入攻击
        CFG.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
    }

    private FreemarkerUtils() {

    }

    /**
     * 渲染html模板
     * @param templatePath html模板路径
     * @param data 模板数据
     * @return html
     */
    public static String eval(String templatePath, Map<String, Object> data) {
        Template template;
        try {
            template = CFG.getTemplate(templatePath);
        } catch (IOException e) {
            throw new ToolRuntimeException(e);
        }
        try (StringWriter out = new StringWriter()) {
            template.process(data, out);
            return out.toString();
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
    }
}

