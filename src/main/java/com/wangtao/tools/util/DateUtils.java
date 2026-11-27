package com.wangtao.tools.util;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author wangtao
 * Created at 2023-09-25
 */
public final class DateUtils {

    public static final String STANDARD_DATE = "yyyy-MM-dd";

    public static final String SHORT_DATE = "yyyyMMdd";

    public static final String STANDARD_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    public static final String STANDARD_TIME = "HH:mm:ss";

    private static final DateTimeFormatter STANDARD_DATE_FORMATTER = DateTimeFormatter.ofPattern(STANDARD_DATE);

    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern(SHORT_DATE);

    private static final DateTimeFormatter STANDARD_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(STANDARD_DATE_TIME);

    private static final DateTimeFormatter STANDARD_TIME_FORMATTER = DateTimeFormatter.ofPattern(STANDARD_TIME);

    private DateUtils() {

    }

    public static LocalDate parseDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr, STANDARD_DATE_FORMATTER);
    }

    public static LocalDate parseDate(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDate parseShortDate(Integer date) {
        if (date == null) {
            return null;
        }
        return parseShortDate(String.valueOf(date));
    }

    public static LocalDate parseShortDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr, SHORT_DATE_FORMATTER);
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (StringUtils.isBlank(dateTimeStr)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, STANDARD_DATE_TIME_FORMATTER);
    }

    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (StringUtils.isBlank(dateTimeStr)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalTime parseTime(String timeStr) {
        if (StringUtils.isBlank(timeStr)) {
            return null;
        }
        return LocalTime.parse(timeStr, STANDARD_TIME_FORMATTER);
    }

    public static LocalTime parseTime(String timeStr, String pattern) {
        if (StringUtils.isBlank(timeStr)) {
            return null;
        }
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(STANDARD_DATE_FORMATTER);
    }

    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatShortDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(SHORT_DATE_FORMATTER);
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(STANDARD_DATE_TIME_FORMATTER);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(STANDARD_TIME_FORMATTER);
    }

    public static String format(LocalTime time, String pattern) {
        if (time == null) {
            return null;
        }
        return time.format(DateTimeFormatter.ofPattern(pattern));
    }
}
