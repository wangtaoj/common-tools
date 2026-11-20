package com.wangtao.tools.util;

import com.wangtao.tools.exception.ToolRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangtao
 * Created at 2026-11-20
 */
public class ThymeleafUtilsTest {

    @Test
    public void testEval() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Thymeleaf");
        data.put("name", "wangtao");
        data.put("localDateTime", LocalDateTime.of(2026, 11, 20, 10, 30, 45));

        String result = ThymeleafUtils.eval("test", data);
        Assertions.assertTrue(result.contains("<h1>Thymeleaf</h1>"));
        Assertions.assertTrue(result.contains("hello, wangtao"));
        // 验证 JSR310 支持（Java8TimeDialect）
        Assertions.assertTrue(result.contains("2026-11-20 10:30:45"));
    }

    @Test
    public void testEvalTemplateNotFound() {
        Assertions.assertThrows(ToolRuntimeException.class,
                () -> ThymeleafUtils.eval("not-exists", null));
    }
}
