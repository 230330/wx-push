package com.aguo.wxpush.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类
 */
public class DateUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] WEEK_DAYS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    /**
     * 将Date对象转换为指定格式的字符串
     *
     * @param date   Date对象
     * @param format 格式化字符串
     * @return 格式化后的日期字符串
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    /**
     * 根据日期获取星期
     */
    public static String getWeekOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) {
            w = 0;
        }
        return WEEK_DAYS[w];
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return 天数差 +1（包含当天）
     */
    public static String daysBetween(String startDate, String endDate) throws ParseException {
        long nd = 1000L * 24 * 60 * 60;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date newStartDate = sdf.parse(startDate);
        Date newEndDate = sdf.parse(endDate);
        long diff = newEndDate.getTime() - newStartDate.getTime();
        return String.valueOf(diff / nd + 1);
    }

    /**
     * 计算距离生日的天数
     *
     * @param startDate 当前日期，格式 yyyy-MM-dd
     * @param endDate   目标日期，格式 yyyy-MM-dd
     * @return 天数差
     */
    public static String birthDayBetween(String startDate, String endDate) throws ParseException {
        long nd = 1000L * 24 * 60 * 60;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date newStartDate = sdf.parse(startDate);
        Date newEndDate = sdf.parse(endDate);
        long diff = newEndDate.getTime() - newStartDate.getTime();
        return String.valueOf(diff / nd);
    }

    /**
     * 计算两个日期之间的月数差
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return 月数差 +1
     */
    public static String monthsBetween(String startDate, String endDate) throws ParseException {
        LocalDate startTime = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate endTime = LocalDate.parse(endDate, DATE_FORMATTER);
        return String.valueOf(ChronoUnit.MONTHS.between(startTime, endTime) + 1);
    }

    /**
     * 计算两个日期之间的年数差
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return 年数差
     */
    public static String yearsBetween(String startDate, String endDate) throws ParseException {
        LocalDate startTime = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate endTime = LocalDate.parse(endDate, DATE_FORMATTER);
        return String.valueOf(ChronoUnit.YEARS.between(startTime, endTime));
    }
}
