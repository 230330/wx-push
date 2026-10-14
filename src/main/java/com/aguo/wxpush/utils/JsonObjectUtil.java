package com.aguo.wxpush.utils;

import com.alibaba.fastjson.JSONObject;

/**
 * 微信模板消息字段工具类
 * 将单个字段打包为模板消息所需的 {value, color} 结构
 */
public class JsonObjectUtil {

    /**
     * 打包模板消息字段
     *
     * @param value 字段内容
     * @param color 字段颜色（如 #000000）
     * @return 含 value/color 的 JSONObject
     */
    public static JSONObject packJsonObject(String value, String color) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("value", value);
        jsonObject.put("color", color);
        return jsonObject;
    }
}
