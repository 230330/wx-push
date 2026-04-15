package com.aguo.wxpush.service;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.exception.WxPushException;
import com.aguo.wxpush.utils.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 微信 AccessToken 管理服务
 * 缓存 token，避免每次推送都重新获取（token 有效期2小时）
 */
@Slf4j
@Service
public class AccessTokenService {

    @Resource
    private WxConfigProperties wxConfig;

    /** 缓存的 accessToken */
    private volatile String cachedToken;

    /** token 获取时间戳（毫秒） */
    private volatile long tokenTimestamp = 0L;

    /** token 有效期：7000秒（官方7200秒，提前200秒刷新） */
    private static final long TOKEN_EXPIRE_MILLIS = 7000_000L;

    /**
     * 获取有效的 AccessToken，自动缓存和刷新
     */
    public String getAccessToken() {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedToken) && (now - tokenTimestamp) < TOKEN_EXPIRE_MILLIS) {
            return cachedToken;
        }
        synchronized (this) {
            // 双重检查
            if (StringUtils.hasText(cachedToken) && (System.currentTimeMillis() - tokenTimestamp) < TOKEN_EXPIRE_MILLIS) {
                return cachedToken;
            }
            return refreshAccessToken();
        }
    }

    /**
     * 强制刷新 AccessToken
     */
    public String refreshAccessToken() {
        String grantType = "client_credential";
        String params = "grant_type=" + grantType
                + "&secret=" + wxConfig.getAppSecret()
                + "&appid=" + wxConfig.getAppId();

        String response = HttpUtil.sendGet("https://api.weixin.qq.com/cgi-bin/token", params);
        if (!StringUtils.hasText(response)) {
            throw new WxPushException("微信token请求失败，请检查网络连接");
        }

        JSONObject jsonObject = JSONObject.parseObject(response);
        log.info("微信token响应: {}", jsonObject);

        if (jsonObject.containsKey("access_token")) {
            cachedToken = jsonObject.getString("access_token");
            tokenTimestamp = System.currentTimeMillis();
            log.info("AccessToken 刷新成功");
            return cachedToken;
        }

        String errcode = jsonObject.getString("errcode");
        String errmsg = jsonObject.getString("errmsg");
        log.error("AccessToken 获取失败: errcode={}, errmsg={}", errcode, errmsg);
        throw new WxPushException("token获取失败，请检查appId和appSecret配置");
    }
}
