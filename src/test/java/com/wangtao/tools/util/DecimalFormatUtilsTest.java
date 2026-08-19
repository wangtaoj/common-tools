package com.wangtao.tools.util;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * @author wangtao
 * Created at 2024-01-05
 */
public class DecimalFormatUtilsTest {

    @Test
    public void testFormat() {
        BigDecimal number = new BigDecimal("12.345");
        Assertions.assertEquals("12.35", DecimalFormatUtils.format(number));
        Assertions.assertEquals("12.3450", DecimalFormatUtils.format(number, 4));

        Assertions.assertEquals("12.35", DecimalFormatUtils.formatTrimZero(number));
        Assertions.assertEquals("12.345", DecimalFormatUtils.formatTrimZero(number, 4));

        Assertions.assertEquals("500,000,000.46", DecimalFormatUtils.formatMoney(new BigDecimal("500000000.45678")));
        Assertions.assertEquals("20.46", DecimalFormatUtils.formatMoney(new BigDecimal("20.45678")));
        Assertions.assertEquals("500,000,000.00", DecimalFormatUtils.formatMoney(new BigDecimal("500000000")));

        Assertions.assertEquals("12.46%", DecimalFormatUtils.formatPercent(new BigDecimal("0.1245678"), 2));
        Assertions.assertEquals("98.00%", DecimalFormatUtils.formatPercent(new BigDecimal("0.98"), 2));
        Assertions.assertEquals("98%", DecimalFormatUtils.formatPercent(new BigDecimal("0.98"), 2, true));
    }
}
