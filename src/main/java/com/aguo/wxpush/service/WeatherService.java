package com.aguo.wxpush.service;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * <p>Title:获取天气信息接口</p>
 * <p>Description:</p>
 * @throws Exception
 * @author hezf
 * @date 2023/3/27
 */
public interface WeatherService {
    JSONObject getWeatherByCity();

    JSONObject getWeatherByIP();
    Map<String, String> getTheNextThreeDaysWeather();

}
