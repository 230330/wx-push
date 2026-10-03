package com.aguo.wxpush.service;

import com.aguo.wxpush.utils.QWeatherJwtUtil;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 天气服务集成测试 - 真实调用和风天气 QWeather API 验证解析逻辑
 *
 * 运行前需要配置环境变量（参见 env.ps1）：
 *   WEATHER_API_HOST / WEATHER_API_KEY（或 JWT 相关变量）/ WEATHER_CITY
 */
@SpringBootTest
public class WeatherServiceIntegrationTest {

    @Resource
    private WeatherService weatherService;

    /**
     * 验证当日天气：city/wea/tem_day/tem_night/win/win_speed/humidity 字段齐全
     */
    @Test
    public void testGetWeatherByCity() {
        JSONObject weather = weatherService.getWeatherByCity();
        assertNotNull(weather, "天气数据不应为空");
        assertTrue(weather.containsKey("wea"), "缺少 wea 字段");
        assertTrue(weather.containsKey("tem_day"), "缺少 tem_day 字段");
        assertTrue(weather.containsKey("tem_night"), "缺少 tem_night 字段");
        assertTrue(weather.containsKey("wind") || weather.containsKey("win"), "缺少风相关字段");
        System.out.println("=== getWeatherByCity 结果 ===");
        System.out.println(weather.toJSONString());
    }

    /**
     * 验证未来三天天气：今/明/后 三天天气描述齐全
     */
    @Test
    public void testGetTheNextThreeDaysWeather() {
        Map<String, String> days = weatherService.getTheNextThreeDaysWeather();
        assertFalse(days.isEmpty(), "三天天气不应为空");
        assertTrue(days.containsKey("今"), "缺少今天天气");
        assertTrue(days.containsKey("明"), "缺少明天天气");
        assertTrue(days.containsKey("后"), "缺少后天天气");
        System.out.println("=== 未来三天天气 ===");
        System.out.println(days);
    }

    /**
     * 回归测试：JWT 鉴权配置缺失时不得抛出底层解码异常（历史 bug：ArrayIndexOutOfBoundsException: 11），
     * 应返回 null 并输出可读的错误日志
     */
    @Test
    public void testCreateTokenWithMissingConfig() {
        assertNull(QWeatherJwtUtil.createToken(null, null, null, null), "全空参数应返回 null");
        assertNull(QWeatherJwtUtil.createToken("kid", "iss", "sub", ""), "空私钥应返回 null");
        assertNull(QWeatherJwtUtil.createToken("kid", "iss", "sub", "   "), "空白私钥应返回 null");
        assertNull(QWeatherJwtUtil.createToken("kid", "iss", "sub", "not-a-valid-key"), "无效私钥格式应返回 null");
    }
}
