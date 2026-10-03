package com.aguo.wxpush.service.impl;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.service.WeatherService;
import com.aguo.wxpush.utils.HttpUtil;
import com.aguo.wxpush.utils.QWeatherJwtUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 天气信息服务实现类 - 和风天气 QWeather API（https://dev.qweather.com）
 *
 * 接口说明：
 * - 城市查询: GET /geo/v2/city/lookup?location={城市}&range=cn
 * - 实时天气: GET /weather/v1/current/{lat}/{lon}
 * - 逐天预报: GET /weather/v1/daily/{lat}/{lon}?days=N
 *
 * 鉴权说明（二选一，自动识别）：
 * - 配置 weatherApiKey 时使用 API KEY（请求头 X-QW-Api-Key）
 * - 否则使用 JWT（Authorization: Bearer，Ed25519 私钥签名）
 */
@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    private WxConfigProperties wxConfig;

    /** 城市查询接口路径 */
    private static final String GEO_LOOKUP_PATH = "/geo/v2/city/lookup";
    /** 实时天气接口路径 */
    private static final String CURRENT_WEATHER_PATH = "/weather/v1/current";
    /** 逐天预报接口路径 */
    private static final String DAILY_WEATHER_PATH = "/weather/v1/daily";

    /** 风向代码 -> 中文描述映射 */
    private static final Map<String, String> COMPASS_CN = new HashMap<>();

    static {
        COMPASS_CN.put("n", "北风");
        COMPASS_CN.put("nne", "北东北风");
        COMPASS_CN.put("ne", "东北风");
        COMPASS_CN.put("ene", "东东北风");
        COMPASS_CN.put("e", "东风");
        COMPASS_CN.put("ese", "东东南风");
        COMPASS_CN.put("se", "东南风");
        COMPASS_CN.put("sse", "南东南风");
        COMPASS_CN.put("s", "南风");
        COMPASS_CN.put("ssw", "南西南风");
        COMPASS_CN.put("sw", "西南风");
        COMPASS_CN.put("wsw", "西西南风");
        COMPASS_CN.put("w", "西风");
        COMPASS_CN.put("wnw", "西西北风");
        COMPASS_CN.put("nw", "西北风");
        COMPASS_CN.put("nnw", "北西北风");
        COMPASS_CN.put("vrb", "不定向风");
        COMPASS_CN.put("none", "无风");
    }

    /** 城市坐标内存缓存（避免每次推送重复调用 GeoAPI） */
    private volatile String cachedCity;
    private volatile Location cachedLocation;

    @Override
    public JSONObject getWeatherByCity() {
        Location location = resolveLocation(wxConfig.getCity());
        if (location == null) {
            log.error("无法定位城市 [{}]，请检查城市名配置", wxConfig.getCity());
            return null;
        }

        // 返回旧版扁平结构（city/wea/tem_night/tem_day/win/win_speed/humidity），保持 MessageAssembler 不变
        JSONObject result = new JSONObject();
        result.put("city", location.name);

        // 实时天气：天气现象、湿度、风向风力
        String currentResp = request(CURRENT_WEATHER_PATH + "/" + location.lat + "/" + location.lon + "?lang=zh");
        if (currentResp != null) {
            JSONObject current = JSONObject.parseObject(currentResp);
            JSONObject condition = current.getJSONObject("condition");
            if (condition != null) {
                result.put("wea", condition.getString("text"));
            }
            if (current.containsKey("humidity")) {
                // 湿度为 0~1 的小数，转为百分数整数
                result.put("humidity", String.valueOf(Math.round(current.getDoubleValue("humidity") * 100)));
            }
            JSONObject wind = current.getJSONObject("wind");
            if (wind != null) {
                JSONObject direction = wind.getJSONObject("direction");
                if (direction != null) {
                    result.put("win", compassToChinese(direction.getString("compass")));
                }
                JSONObject speed = wind.getJSONObject("speed");
                if (speed != null && speed.containsKey("scale")) {
                    result.put("win_speed", speed.getInteger("scale") + "级");
                } else if (speed != null && speed.containsKey("value")) {
                    result.put("win_speed", formatNumber(speed.getDoubleValue("value")) + "m/s");
                }
            }
        }

        // 逐天预报：今日最高温（白天）/最低温（夜间）
        String dailyResp = request(DAILY_WEATHER_PATH + "/" + location.lat + "/" + location.lon + "?days=1&lang=zh");
        if (dailyResp != null) {
            JSONObject daily = JSONObject.parseObject(dailyResp);
            JSONArray days = daily.getJSONArray("days");
            if (days != null && !days.isEmpty()) {
                JSONObject today = days.getJSONObject(0);
                JSONObject tempMax = today.getJSONObject("temperatureMax");
                if (tempMax != null && tempMax.containsKey("value")) {
                    result.put("tem_day", formatNumber(tempMax.getDoubleValue("value")));
                }
                JSONObject tempMin = today.getJSONObject("temperatureMin");
                if (tempMin != null && tempMin.containsKey("value")) {
                    result.put("tem_night", formatNumber(tempMin.getDoubleValue("value")));
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getTheNextThreeDaysWeather() {
        Location location = resolveLocation(wxConfig.getCity());
        if (location == null) {
            return new HashMap<>();
        }

        String response = request(DAILY_WEATHER_PATH + "/" + location.lat + "/" + location.lon + "?days=3&lang=zh");
        if (response == null) {
            log.error("获取未来三天天气失败，检查和风天气 API 配置");
            return new HashMap<>();
        }

        JSONObject json = JSONObject.parseObject(response);
        JSONArray days = json.getJSONArray("days");
        if (days == null || days.isEmpty()) {
            log.warn("未来天气预报数据为空");
            return new HashMap<>();
        }

        // 依次映射为 今/明/后 三天的白天天气现象
        String[] labels = {"今", "明", "后"};
        Map<String, String> result = new LinkedHashMap<>(4);
        for (int i = 0; i < days.size() && i < labels.length; i++) {
            JSONObject daytime = days.getJSONObject(i).getJSONObject("daytime");
            if (daytime != null) {
                JSONObject condition = daytime.getJSONObject("condition");
                if (condition != null && StringUtils.hasText(condition.getString("text"))) {
                    result.put(labels[i], condition.getString("text"));
                }
            }
        }
        return result;
    }

    @Override
    public JSONObject getWeatherByIP() {
        // 新版 QWeather API 不提供 IP 定位接口，使用配置城市查询
        return getWeatherByCity();
    }

    /**
     * 城市名 -> 经纬度（带内存缓存）
     *
     * @return 城市坐标信息，查询失败返回 null
     */
    private Location resolveLocation(String city) {
        if (!StringUtils.hasText(city)) {
            log.warn("未配置天气查询城市");
            return null;
        }
        Location location = cachedLocation;
        if (location != null && city.equals(cachedCity)) {
            return location;
        }

        String param;
        try {
            param = "location=" + URLEncoder.encode(city, "UTF-8") + "&range=cn&lang=zh";
        } catch (Exception e) {
            log.error("城市名称编码失败: {}", e.getMessage(), e);
            return null;
        }
        String response = request(GEO_LOOKUP_PATH + "?" + param);
        if (response == null) {
            return null;
        }

        JSONObject geo = JSONObject.parseObject(response);
        if (!"200".equals(geo.getString("code"))) {
            log.error("城市查询失败: {}", geo.getString("code"));
            return null;
        }
        JSONArray list = geo.getJSONArray("location");
        if (list == null || list.isEmpty()) {
            log.warn("未查询到城市 [{}] 的坐标信息", city);
            return null;
        }

        JSONObject first = list.getJSONObject(0);
        Location resolved = new Location(first.getString("name"), first.getString("lat"), first.getString("lon"));
        cachedCity = city;
        cachedLocation = resolved;
        return resolved;
    }

    /**
     * 发起和风天气 API 请求：自动拼接 API Host 并携带鉴权请求头
     */
    private String request(String pathAndQuery) {
        String url = "https://" + wxConfig.getWeatherApiHost() + pathAndQuery;
        String response = HttpUtil.sendGet(url, null, buildAuthHeaders());
        if (!StringUtils.hasText(response)) {
            log.error("和风天气 API 请求失败: {}", pathAndQuery);
            return null;
        }
        return response;
    }

    /**
     * 构建鉴权请求头：
     * - 配置了 API KEY：使用 X-QW-Api-Key（简单方式）
     * - 否则使用 JWT：Authorization: Bearer <token>（推荐方式）
     */
    private Map<String, String> buildAuthHeaders() {
        Map<String, String> headers = new HashMap<>(2);
        if (StringUtils.hasText(wxConfig.getWeatherApiKey())) {
            headers.put("X-QW-Api-Key", wxConfig.getWeatherApiKey());
            return headers;
        }

        // JWT 方式：先校验配置完整性，避免空私钥触发底层解码异常
        if (!StringUtils.hasText(wxConfig.getWeatherPrivateKey())
                || !StringUtils.hasText(wxConfig.getWeatherCredentialId())
                || !StringUtils.hasText(wxConfig.getWeatherDeveloperId())
                || !StringUtils.hasText(wxConfig.getWeatherProjectId())) {
            log.error("和风天气 API 鉴权配置缺失：未配置 API KEY（WEATHER_API_KEY），"
                    + "且 JWT 配置不完整（需 WEATHER_PRIVATE_KEY/WEATHER_CREDENTIAL_ID/WEATHER_DEV_ID/WEATHER_PROJECT_ID）");
            return headers;
        }

        String token = QWeatherJwtUtil.createToken(
                wxConfig.getWeatherCredentialId(),
                wxConfig.getWeatherDeveloperId(),
                wxConfig.getWeatherProjectId(),
                wxConfig.getWeatherPrivateKey());
        if (StringUtils.hasText(token)) {
            headers.put("Authorization", "Bearer " + token);
        } else {
            log.error("和风天气 JWT 生成失败，请检查 JWT 配置");
        }
        return headers;
    }

    /**
     * 风向代码转中文描述
     */
    private String compassToChinese(String compass) {
        if (compass == null) {
            return "";
        }
        return COMPASS_CN.getOrDefault(compass, compass);
    }

    /**
     * 数值四舍五入为整数字符串（与旧版温度显示风格一致）
     */
    private String formatNumber(double value) {
        return String.valueOf(Math.round(value));
    }

    /**
     * 城市坐标信息
     */
    private static class Location {
        private final String name;
        private final String lat;
        private final String lon;

        Location(String name, String lat, String lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }
}
