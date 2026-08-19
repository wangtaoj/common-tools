package com.wangtao.tools.util;

import org.apache.commons.lang3.StringUtils;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 占位符0, #
 * <ul>
 *   <li>规则1: 整数部分位数超出时, 不截断, 原样返回; 小数部分位数超出时会丢弃, 按照舍入规则进位</li>
 *   <li>规则2: 位数不足, 占位符0用0补齐, #则不添加任何字符补齐</li>
 *   <li>规则3: 舍入规则, 默认为HALF_EVEN</li>
 * </ul>
 * 举例:
 * 对于数字12.342
 * 0        -> 12  整数位超出, 原样返回
 * #        -> 12
 * 000      -> 012 用0补齐
 * ###      -> 12  不补齐
 * 000.00   -> 012.34 小数位不论是占位符0还是#, 超出的都丢弃
 * ###.00   -> 12.34
 * ###.##   -> 12.34
 * ###.0000 -> 12.3420 用0补齐
 * ###.#### -> 12.342  不补齐
 *
 * @author wangtao
 * Created at 2024-01-05
 */
public final class DecimalFormatUtils {

    private DecimalFormatUtils() {}

    /**
     * 保留2位小数, 四舍五入
     * @param number 数字
     * @return 格式化后的字符串
     */
    public static String format(Object number) {
        return format(number, 2);
    }

    /**
     * 保留n位小数, 四舍五入
     * @param number 数字
     * @param scale 保留的小数位数
     * @return 格式化后的字符串
     */
    public static String format(Object number, int scale) {
        return format(number, scale, RoundingMode.HALF_UP);
    }

    /**
     * 保留小数
     * @param number 数字
     * @param scale 保留的小数位数
     * @param roundingMode 舍入规则
     * @return 格式化后的字符串
     */
    public static String format(Object number, int scale, RoundingMode roundingMode) {
        String pattern = getPattern(scale, false);
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        decimalFormat.setRoundingMode(roundingMode);
        return decimalFormat.format(number);
    }

    /**
     * 保留2位小数, 并且去掉末尾多余的0, 四舍五入
     * @param number 数字
     * @return 格式化后的字符串
     */
    public static String formatTrimZero(Object number) {
        return formatTrimZero(number, 2);
    }

    /**
     * 保留小数, 并且去掉末尾多余的0, 四舍五入
     * @param number 数字
     * @param scale 保留的小数位数
     * @return 格式化后的字符串
     */
    public static String formatTrimZero(Object number, int scale) {
        return formatTrimZero(number, scale, RoundingMode.HALF_UP);
    }

    /**
     * 保留小数, 并且去掉末尾多余的0
     * @param number 数字
     * @param scale 保留的小数位数
     * @param roundingMode 舍入规则
     * @return 格式化后的字符串
     */
    public static String formatTrimZero(Object number, int scale, RoundingMode roundingMode) {
        String pattern = getPattern(scale, true);
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        decimalFormat.setRoundingMode(roundingMode);
        return decimalFormat.format(number);
    }

    /**
     * 金额千分位展示, 保留2位小数, 四舍五入
     * @param number 数字
     * @return 格式化后的字符串
     */
    public static String formatMoney(Object number) {
        return formatMoney(number, 2, RoundingMode.HALF_UP);
    }

    /**
     * 金额千分位展示, 保留n位小数, 四舍五入
     * @param number 数字
     * @param scale 保留的小数位数
     * @return 格式化后的字符串
     */
    public static String formatMoney(Object number, int scale) {
        return formatMoney(number, scale, RoundingMode.HALF_UP);
    }

    /**
     * 金额千分位展示, 保留n位小数
     * @param number 数字
     * @param scale 保留的小数位数
     * @param roundingMode 舍入规则
     * @return 格式化后的字符串
     */
    public static String formatMoney(Object number, int scale, RoundingMode roundingMode) {
        String decimalPattern;
        switch (scale) {
            case 0:
                decimalPattern = "";
                break;
            case 1:
                decimalPattern = "0";
                break;
            case 2:
                decimalPattern = "00";
                break;
            default:
                decimalPattern = StringUtils.repeat('0', scale);
        }
        DecimalFormat decimalFormat = new DecimalFormat("#,###." + decimalPattern);
        decimalFormat.setRoundingMode(roundingMode);
        return decimalFormat.format(number);
    }

    /**
     * 格式化成百分比
     * @param number 数字
     * @param scale 保留的小数位数
     * @return 格式化后的字符串
     */
    public static String formatPercent(Object number, int scale) {
       return formatPercent(number, scale, false);
    }

    /**
     * 格式化成百分比
     * @param number 数字
     * @param scale 保留的小数位数
     * @param trimZeros 是否去掉末尾多余的0
     * @return 格式化后的字符串
     */
    public static String formatPercent(Object number, int scale, boolean trimZeros) {
        String pattern = getPattern(scale, trimZeros) + '%';
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        return decimalFormat.format(number);
    }

    /**
     * @param scale 小数位数
     * @param trimZeros 小数部分位数不足时是否需要用0补齐
     * @return 模式
     */
    private static String getPattern(int scale, boolean trimZeros) {
        switch (scale) {
            case 0:
                return "#";
            case 1:
                return "#." + (trimZeros ? "#" : "0");
            case 2:
                return "#." + (trimZeros ? "##" : "00");
            case 3:
                return "#." + (trimZeros ? "###" : "000");
            case 4:
                return "#." + (trimZeros ? "####" : "0000");
            default:
                return "#." + StringUtils.repeat(trimZeros ? '#' : '0', scale);
        }
    }
}
