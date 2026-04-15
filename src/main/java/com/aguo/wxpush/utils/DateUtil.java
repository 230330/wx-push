package com.aguo.wxpush.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期工具类（统一使用 java.time API）
 */
public class DateUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] WEEK_DAYS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    /**
     * 将LocalDate转换为指定格式的字符串
     *
     * @param date   LocalDate对象
     * @param format 格式化字符串
     * @return 格式化后的日期字符串
     */
    public static String formatDate(LocalDate date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }

    /**
     * 根据日期获取星期
     *
     * @param date LocalDate对象
     * @return 星期几的中文表示
     */
    public static String getWeekOfDate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return WEEK_DAYS[dayOfWeek.getValue() % 7];
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return 天数差 +1（包含当天）
     */
    public static String daysBetween(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        return String.valueOf(ChronoUnit.DAYS.between(start, end) + 1);
    }

    /**
     * 计算距离生日的天数
     *
     * @param startDate 当前日期，格式 yyyy-MM-dd
     * @param endDate   目标日期，格式 yyyy-MM-dd
     * @return 天数差
     */
    public static String birthDayBetween(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        return String.valueOf(ChronoUnit.DAYS.between(start, end));
    }

    /**
     * 计算两个日期之间的月数差
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return 月数差 +1
     */
    public static String monthsBetween(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        return String.valueOf(ChronoUnit.MONTHS.between(start, end) + 1);
    }

    /**
     * 计算两个日期之间的年数差
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return 年数差
     */
    public static String yearsBetween(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        return String.valueOf(ChronoUnit.YEARS.between(start, end));
    }
}
