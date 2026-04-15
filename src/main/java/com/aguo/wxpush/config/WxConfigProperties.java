package com.aguo.wxpush.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信推送服务配置属性
 * 对应 application.yml 中 wx.config / weather.config / message.config / ApiSpace 前缀
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.config")
public class WxConfigProperties {

    /** 微信公众号 appId */
    private String appId;

    /** 微信公众号 appSecret */
    private String appSecret;

    /** 模板消息ID */
    private String templateId;

    /** 推送目标用户 openid 列表 */
    private List<String> openidList = new ArrayList<>();

    /** 天气API - appid */
    private String weatherAppId;

    /** 天气API - appSecret */
    private String weatherAppSecret;

    /** 天气查询城市 */
    private String city;

    /** 纪念日（在一起日期），格式 yyyy-MM-dd */
    private String togetherDate;

    /** 生日1，格式 MM-dd */
    private String birthday1;

    /** 生日2，格式 MM-dd */
    private String birthday2;

    /** 自定义推送消息 */
    private String message;

    /** 是否启用每日一句 */
    private boolean enableDaily = true;

    /** ApiSpace token（每日一句接口） */
    private String token;
}
