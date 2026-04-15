package com.aguo.wxpush.service.impl;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.entity.TextMessage;
import com.aguo.wxpush.service.AccessTokenService;
import com.aguo.wxpush.service.MessageAssembler;
import com.aguo.wxpush.service.SendService;
import com.aguo.wxpush.utils.HttpUtil;
import com.aguo.wxpush.utils.MessageUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 微信公众号消息推送实现类
 * 负责调度：获取Token → 组装消息 → 推送给每个用户
 */
@Slf4j
@Service
public class SendServiceImpl implements SendService {

    @Resource
    private AccessTokenService accessTokenService;

    @Resource
    private MessageAssembler messageAssembler;

    @Resource
    private WxConfigProperties wxConfig;

    @Override
    public String sendWeChatMsg() {
        String accessToken = accessTokenService.getAccessToken();
        if (!StringUtils.hasText(accessToken)) {
            log.error("token获取失败，请检查appId、appSecret配置");
            return buildResult(false, "token获取失败");
        }

        // 组装模板消息数据
        Map<String, Object> templateData = messageAssembler.assembleTemplateData();

        // 遍历所有用户推送
        List<JSONObject> errorList = new ArrayList<>();
        for (String openId : wxConfig.getOpenidList()) {
            try {
                sendTemplateMessage(accessToken, openId, templateData, errorList);
            } catch (Exception e) {
                log.error("推送失败, openId={}: {}", openId, e.getMessage(), e);
                JSONObject error = new JSONObject();
                error.put("openid", openId);
                error.put("errorMessage", e.getMessage());
                errorList.add(error);
            }
        }

        if (!errorList.isEmpty()) {
            log.warn("部分用户推送失败: {}", errorList);
            return buildResult(false, "部分推送失败", errorList);
        }

        log.info("消息推送成功！");
        return buildResult(true, "消息推送成功");
    }

    /**
     * 发送模板消息给指定用户
     */
    private void sendTemplateMessage(String accessToken, String openId,
                                      Map<String, Object> templateData,
                                      List<JSONObject> errorList) {
        JSONObject templateMsg = new JSONObject(new LinkedHashMap<>());
        templateMsg.put("touser", openId);
        templateMsg.put("template_id", wxConfig.getTemplateId());
        templateMsg.put("data", new JSONObject(templateData));

        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
        String response = HttpUtil.sendPost(url, templateMsg.toJSONString());

        if (!StringUtils.hasText(response)) {
            throw new RuntimeException("微信推送请求失败，响应为空");
        }

        JSONObject result = JSONObject.parseObject(response);
        String errcode = result.getString("errcode");
        if (!"0".equals(errcode)) {
            JSONObject error = new JSONObject();
            error.put("openid", openId);
            error.put("errorMessage", result.getString("errmsg"));
            errorList.add(error);
            log.error("微信推送失败, openId={}, errcode={}, errmsg={}", openId, errcode, result.getString("errmsg"));
        } else {
            log.info("推送成功, openId={}", openId);
        }
    }

    /**
     * 处理微信消息回调
     */
    @Override
    public String messageHandle(HttpServletRequest request, HttpServletResponse response) {
        response.setCharacterEncoding("utf-8");
        Map<String, String> resultMap = MessageUtil.parseXml(request);

        TextMessage textMessage = new TextMessage();
        textMessage.setToUserName(resultMap.get("FromUserName"));
        textMessage.setFromUserName(resultMap.get("ToUserName"));
        textMessage.setCreateTime(new Date().getTime());
        textMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_TEXT);

        if ("text".equals(resultMap.get("MsgType"))) {
            textMessage.setContent(resultMap.get("Content"));
        } else {
            textMessage.setContent("目前仅支持文本呦");
        }
        return textMessage.getContent();
    }

    private String buildResult(boolean success, String message) {
        JSONObject result = new JSONObject();
        result.put("result", message);
        return result.toJSONString();
    }

    private String buildResult(boolean success, String message, List<JSONObject> errorList) {
        JSONObject result = new JSONObject();
        result.put("result", message);
        result.put("errorData", errorList);
        return result.toJSONString();
    }
}
