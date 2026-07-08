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
import java.io.IOException;
import java.io.PrintWriter;
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
     * 自动处理 token 过期（errcode=40001）并重试一次
     */
    private void sendTemplateMessage(String accessToken, String openId,
                                      Map<String, Object> templateData,
                                      List<JSONObject> errorList) {
        JSONObject templateMsg = new JSONObject(new LinkedHashMap<>());
        templateMsg.put("touser", openId);
        templateMsg.put("template_id", wxConfig.getTemplateId());
        templateMsg.put("data", new JSONObject(templateData));

        JSONObject result = doSendTemplateMessage(accessToken, templateMsg);

        if (result == null) {
            throw new RuntimeException("微信推送请求失败，响应为空");
        }

        String errcode = result.getString("errcode");
        if ("40001".equals(errcode) || "42001".equals(errcode)) {
            // Token 过期或失效，刷新后重试一次
            log.warn("AccessToken 失效(errcode={})，刷新后重试", errcode);
            accessToken = accessTokenService.refreshAccessToken();
            result = doSendTemplateMessage(accessToken, templateMsg);

            if (result == null) {
                throw new RuntimeException("微信推送重试失败，响应为空");
            }
            errcode = result.getString("errcode");
        }

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
     * 执行一次模板消息发送，返回响应 JSON
     */
    private JSONObject doSendTemplateMessage(String accessToken, JSONObject templateMsg) {
        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
        String response = HttpUtil.sendPost(url, templateMsg.toJSONString());

        if (!StringUtils.hasText(response)) {
            return null;
        }
        return JSONObject.parseObject(response);
    }

    /**
     * 处理微信消息回调
     * 解析用户消息 -> 构建回复 XML -> 写入响应 -> 返回消息内容
     */
    @Override
    public String messageHandle(HttpServletRequest request, HttpServletResponse response) {
        response.setCharacterEncoding("utf-8");
        Map<String, String> resultMap = MessageUtil.parseXml(request);

        String msgType = resultMap.get("MsgType");
        String fromUser = resultMap.get("FromUserName");
        String toUser = resultMap.get("ToUserName");
        String content = resultMap.get("Content");

        // 构建微信回复消息
        TextMessage replyMessage = new TextMessage();
        replyMessage.setToUserName(fromUser);
        replyMessage.setFromUserName(toUser);
        replyMessage.setCreateTime(System.currentTimeMillis() / 1000);
        replyMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_TEXT);

        if ("text".equals(msgType)) {
            String city = content != null ? content.trim() : "";
            replyMessage.setContent("已切换城市到「" + city + "」，正在为您推送天气信息...");

            // 将回复 XML 写入响应
            writeXmlResponse(response, MessageUtil.textMessageToXml(replyMessage));

            return city;
        } else {
            replyMessage.setContent("目前仅支持文本消息，请直接发送城市名称（如「北京」）");
            writeXmlResponse(response, MessageUtil.textMessageToXml(replyMessage));
            return null;
        }
    }

    /**
     * 将 XML 内容写入 HttpServletResponse
     */
    private void writeXmlResponse(HttpServletResponse response, String xml) {
        response.setContentType("application/xml;charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(xml);
        } catch (IOException e) {
            log.error("写入回复消息失败: {}", e.getMessage(), e);
        }
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
