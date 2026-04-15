package com.aguo.wxpush.service;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.utils.DateUtil;
import com.aguo.wxpush.utils.JsonObjectUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息组装服务
 * 负责将天气、生日、纪念日、名言等数据组装为微信模板消息格式
 */
@Slf4j
@Service
public class MessageAssembler {

    @Resource
    private WxConfigProperties wxConfig;

    @Resource
    private WeatherService weatherService;

    @Resource
    private ProverbService proverbService;

    /**
     * 组装完整的模板消息数据
     *
     * @return 模板消息 data 映射
     */
    public Map<String, Object> assembleTemplateData() {
        Map<String, Object> data = new HashMap<>(16);

        // 1. 日期头部
        assembleDateHeader(data);

        // 2. 天气信息
        assembleWeather(data);

        // 3. 生日倒计时
        assembleBirthday(data);

        // 4. 在一起时间
        assembleTogetherDays(data);

        // 5. 每日一句
        assembleDailyProverb(data);

        // 6. 自定义消息
        data.put("message", JsonObjectUtil.packJsonObject(wxConfig.getMessage(), "#000000"));

        return data;
    }

    private void assembleDateHeader(Map<String, Object> data) {
        String date = DateUtil.formatDate(LocalDate.now(), "yyyy-MM-dd");
        String week = DateUtil.getWeekOfDate(LocalDate.now());
        JSONObject first = JsonObjectUtil.packJsonObject(date + " " + week, "#EED016");
        data.put("first", first);
        log.debug("日期头部: {}", first);
    }

    private void assembleWeather(Map<String, Object> data) {
        JSONObject weatherResult = weatherService.getWeatherByCity();
        if (weatherResult == null) {
            log.warn("天气数据获取为空");
            return;
        }

        data.put("city", JsonObjectUtil.packJsonObject(weatherResult.getString("city"), "#60AEF2"));
        data.put("weather", JsonObjectUtil.packJsonObject(weatherResult.getString("wea"), "#b28d0a"));
        data.put("minTemperature", JsonObjectUtil.packJsonObject(weatherResult.getString("tem_night") + "°", "#0ace3c"));
        data.put("maxTemperature", JsonObjectUtil.packJsonObject(weatherResult.getString("tem_day") + "°", "#dc1010"));
        data.put("wind", JsonObjectUtil.packJsonObject(
                weatherResult.getString("win") + "," + weatherResult.getString("win_speed"), "#6e6e6e"));
        data.put("wet", JsonObjectUtil.packJsonObject(weatherResult.getString("humidity"), "#1f95c5"));

        // 未来三天天气
        Map<String, String> threeDaysWeather = weatherService.getTheNextThreeDaysWeather();
        if (threeDaysWeather != null && !threeDaysWeather.isEmpty()) {
            data.put("day1_wea", JsonObjectUtil.packJsonObject(threeDaysWeather.get("今"), getWeatherColor(threeDaysWeather.get("今"))));
            data.put("day2_wea", JsonObjectUtil.packJsonObject(threeDaysWeather.get("明"), getWeatherColor(threeDaysWeather.get("明"))));
            data.put("day3_wea", JsonObjectUtil.packJsonObject(threeDaysWeather.get("后"), getWeatherColor(threeDaysWeather.get("后"))));
        } else {
            log.warn("未来三天天气获取失败");
        }
    }

    private void assembleBirthday(Map<String, Object> data) {
        String date = DateUtil.formatDate(LocalDate.now(), "yyyy-MM-dd");

        JSONObject birthDate1 = buildBirthdayJson(wxConfig.getBirthday1(), date);
        data.put("birthDate1", birthDate1);
        log.debug("生日1: {}", birthDate1);

        JSONObject birthDate2 = buildBirthdayJson(wxConfig.getBirthday2(), date);
        data.put("birthDate2", birthDate2);
        log.debug("生日2: {}", birthDate2);

        // 当天生日的祝福
        if ("0天".equals(birthDate1.getString("value"))) {
            data.put("zhufuyu", JsonObjectUtil.packJsonObject("生日快乐，宝贝！", "#6EEDE2"));
        }
    }

    private void assembleTogetherDays(Map<String, Object> data) {
        String date = DateUtil.formatDate(LocalDate.now(), "yyyy-MM-dd");
        String startDate = wxConfig.getTogetherDate();

        try {
            String togetherYear = "第" + DateUtil.yearsBetween(startDate, date) + "年";
            String togetherMonth = "第" + DateUtil.monthsBetween(startDate, date) + "月";
            String togetherDay = "第" + DateUtil.daysBetween(startDate, date) + "天";

            data.put("togetherYear", JsonObjectUtil.packJsonObject(togetherYear, "#FEABB5"));
            data.put("togetherMonth", JsonObjectUtil.packJsonObject(togetherMonth, "#FEABB5"));
            data.put("togetherDate", JsonObjectUtil.packJsonObject(togetherDay, "#FEABB5"));
            log.debug("纪念日: {}年 {}月 {}天", togetherYear, togetherMonth, togetherDay);
        } catch (DateTimeParseException e) {
            log.error("纪念日计算失败: {}", e.getMessage(), e);
        }
    }

    private void assembleDailyProverb(Map<String, Object> data) {
        if (!wxConfig.isEnableDaily() || !StringUtils.hasText(wxConfig.getToken())) {
            return;
        }

        String noteZh = null;
        try {
            noteZh = proverbService.getOneNormalProverb();
            data.put("note_Zh", JsonObjectUtil.packJsonObject(noteZh, "#879191"));
            log.debug("中文名言: {}", noteZh);
        } catch (Exception e) {
            log.warn("名言警句获取失败，检查ApiSpace的token是否正确或套餐是否过期");
        }

        if (StringUtils.hasText(noteZh)) {
            try {
                String noteEn = proverbService.translateToEnglish(noteZh);
                data.put("note_En", JsonObjectUtil.packJsonObject(noteEn, "#879191"));
                log.debug("英文翻译: {}", noteEn);
            } catch (Exception e) {
                log.warn("名言翻译失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 构建生日倒计时 JSON
     */
    private JSONObject buildBirthdayJson(String birthday, String currentDate) {
        try {
            int year = LocalDate.now().getYear();
            String targetDate = year + "-" + birthday;
            long days = Long.parseLong(DateUtil.birthDayBetween(currentDate, targetDate));
            if (days < 0) {
                days += 365;
            }
            return JsonObjectUtil.packJsonObject(days + "天", "#6EEDE2");
        } catch (DateTimeParseException e) {
            log.error("生日计算失败: {}", e.getMessage(), e);
            return JsonObjectUtil.packJsonObject("无法识别", "#6EEDE2");
        }
    }

    /**
     * 根据天气是否含"雨"返回对应颜色
     */
    private String getWeatherColor(String weather) {
        return (weather != null && weather.contains("雨")) ? "#1f95c5" : "#b28d0a";
    }
}
