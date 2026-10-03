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

    /** 微信服务器配置的 Token，用于签名验证 */
    private String verifyToken;

    /** 模板消息ID */
    private String templateId;

    /** 推送目标用户 openid 列表 */
    private List<String> openidList = new ArrayList<>();

    /** 天气API - 和风天气 API Host（专属域名） */
    private String weatherApiHost;

    /** 天气API - 开发者ID（JWT payload 的 iss，控制台 -> 设置 中查看） */
    private String weatherDeveloperId;

    /** 天气API - 项目ID（JWT payload 的 sub，控制台 -> 项目管理 中查看） */
    private String weatherProjectId;

    /** 天气API - 凭据ID（JWT header 的 kid，控制台 -> 项目管理 -> 凭据 中查看） */
    private String weatherCredentialId;

    /** 天气API - Ed25519 私钥（PKCS8 PEM 文本或 Base64 编码，JWT 签名用） */
    private String weatherPrivateKey;

    /** 天气API - API KEY（可选，配置后优先使用 API KEY 鉴权，无需私钥） */
    private String weatherApiKey;

    /** 天气查询城市（volatile 保证多线程可见性） */
    private volatile String city;

    /** 纪念日（在一起日期），格式 yyyy-MM-dd */
    private String togetherDate;

    /** 生日1，格式 MM-dd */
    private String birthday1;

    /** 生日1姓名/昵称 */
    private String birthday1Name;

    /** 生日1祝福语（生日当天替换默认message） */
    private String birthday1Message;

    /** 生日2，格式 MM-dd */
    private String birthday2;

    /** 生日2姓名/昵称 */
    private String birthday2Name;

    /** 生日2祝福语（生日当天替换默认message） */
    private String birthday2Message;

    /** 自定义推送消息 */
    private String message;

    /** 是否启用每日一句 */
    private boolean enableDaily = true;

    /** ApiSpace token（每日一句接口） */
    private String token;

    /**
     * 标准化城市名称：去除省/市/区/县后缀
     *
     * @param city 原始城市名称
     * @return 标准化后的城市名称
     */
    public static String normalizeCity(String city) {
        if (city == null || city.isEmpty()) {
            return city;
        }
        if (city.contains("省") || city.contains("市") || city.contains("区") || city.contains("县")) {
            return city.substring(0, city.length() - 1);
        }
        return city;
    }
}
