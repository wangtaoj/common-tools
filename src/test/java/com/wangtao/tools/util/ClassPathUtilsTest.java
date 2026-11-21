package com.wangtao.tools.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author wangtao
 * Created at 2026-11-21
 */
@Slf4j
public class ClassPathUtilsTest {

    @Test
    public void testListFileNames() {
        List<String> fileNames = ClassPathUtils.listFileNames("com/lowagie/text/pdf/fonts/", true);
        fileNames.forEach(log::info);
    }
}
