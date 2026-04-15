package com.aguo.wxpush.service.impl;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.service.WeatherService;
import com.aguo.wxpush.utils.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 天气信息服务实现类
 */
@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    private WxConfigProperties wxConfig;

    private static final String WEATHER_API_HOST = "v1.yiketianqi.com";

    @Override
    public JSONObject getWeatherByCity() {
        String params = "appid=" + wxConfig.getWeatherAppId()
                + "&appsecret=" + wxConfig.getWeatherAppSecret()
                + "&city=" + wxConfig.getCity()
                + "&unescape=1";

        String response = HttpUtil.sendGet("https://" + WEATHER_API_HOST + "/free/day", params);
        if (!StringUtils.hasText(response)) {
            log.error("天气API响应为空");
            return null;
        }
        return JSONObject.parseObject(response);
    }

    @Override
    public Map<String, String> getTheNextThreeDaysWeather() {
        String params = "appid=" + wxConfig.getWeatherAppId()
                + "&appsecret=" + wxConfig.getWeatherAppSecret()
                + "&city=" + wxConfig.getCity();

        String response = HttpUtil.sendGet("https://" + WEATHER_API_HOST + "/free/week", params);
        if (!StringUtils.hasText(response)) {
            log.error("获取未来天气失败，检查配置文件");
            return new HashMap<>();
        }

        JSONObject jsonObject = JSONObject.parseObject(response);
        if (jsonObject.containsKey("errmsg")) {
            log.error("天气API返回错误: {}", jsonObject.getString("errmsg"));
            return new HashMap<>();
        }

        // 封装今天、明天、后天的日期映射
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        LocalDate now = LocalDate.now(zoneId);
        Map<Integer, String> dayLabelMap = new HashMap<>(4);
        dayLabelMap.put(now.getDayOfMonth(), "今");
        dayLabelMap.put(now.plusDays(1L).getDayOfMonth(), "明");
        dayLabelMap.put(now.plusDays(2L).getDayOfMonth(), "后");

        return jsonObject.getJSONArray("data").stream()
                .peek(o -> {
                    String date = getStringFromJson(o, "date");
                    if (date != null && date.length() > 8) {
                        ((JSONObject) o).put("date", date.substring(8));
                    }
                })
                .filter(o -> dayLabelMap.containsKey(getIntegerFromJson(o, "date")))
                .collect(Collectors.toMap(
                        key -> dayLabelMap.get(getIntegerFromJson(key, "date")),
                        value -> getStringFromJson(value, "wea")));
    }

    @Override
    public JSONObject getWeatherByIP() {
        String params = "appid=" + wxConfig.getWeatherAppId()
                + "&appsecret=" + wxConfig.getWeatherAppSecret()
                + "&unescape=1";
        String response = HttpUtil.sendGet("https://" + WEATHER_API_HOST + "/free/day", params);
        if (!StringUtils.hasText(response)) {
            return null;
        }
        return JSONObject.parseObject(response);
    }

    private String getStringFromJson(Object obj, String key) {
        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).getString(key);
        }
        return null;
    }

    private Integer getIntegerFromJson(Object obj, String key) {
        if (obj instanceof JSONObject) {
            try {
                return Integer.valueOf(((JSONObject) obj).getString(key));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
