package com.aguo.wxpush.controller;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.service.SendService;
import com.aguo.wxpush.utils.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 微信消息推送控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx")
public class WxController {

    @Resource
    private WxConfigProperties wxConfig;

    @Resource
    private SendService sendService;

    /**
     * 手动触发推送
     */
    @GetMapping("/send")
    public String send() {
        log.info("手动触发消息推送...");
        try {
            return sendService.sendWeChatMsg();
        } catch (Exception e) {
            log.error("消息推送异常: {}", e.getMessage(), e);
            return "{\"result\":\"信息推送失败\"}";
        }
    }

    /**
     * 修改城市配置并立即推送
     */
    @GetMapping("/changeConfig")
    public void changeConfig(@RequestParam String city, HttpServletResponse response) throws IOException {
        log.info("修改城市配置: {}", city);
        String normalizedCity = WxConfigProperties.normalizeCity(city);
        wxConfig.setCity(normalizedCity);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");
        // XSS 防护：对用户输入做 HTML 转义
        String safeCity = HtmlUtils.htmlEscape(normalizedCity);
        response.getWriter().write("<h1>更新成功！当前城市: " + safeCity + "</h1>");
        sendService.sendWeChatMsg();
    }

    /**
     * 接收微信消息回调（GET：接入验证）
     */
    @GetMapping("/receiveMsg")
    public String verify(@RequestParam(required = false) String signature,
                         @RequestParam(required = false) String timestamp,
                         @RequestParam(required = false) String nonce,
                         @RequestParam(required = false) String echostr) {
        // 微信接入验证：先验证签名
        log.info("微信接入验证, signature={}, timestamp={}, nonce={}, echostr={}", signature, timestamp, nonce, echostr);

        if (!verifySignature(signature, timestamp, nonce)) {
            log.warn("微信签名验证失败，拒绝接入");
            return "signature verification failed";
        }

        return echostr;
    }

    @PostMapping("/receiveMsg")
    public String receiveMsg(@RequestParam(required = false) String signature,
                             @RequestParam(required = false) String timestamp,
                             @RequestParam(required = false) String nonce,
                             HttpServletRequest request, HttpServletResponse response) {
        log.info("接收微信消息回调");

        // 先验证签名
        if (!verifySignature(signature, timestamp, nonce)) {
            log.warn("微信消息签名验证失败，拒绝处理");
            return "signature verification failed";
        }

        String city = sendService.messageHandle(request, response);
        if (city != null && !city.isEmpty()) {
            String normalizedCity = WxConfigProperties.normalizeCity(city);
            wxConfig.setCity(normalizedCity);
            sendService.sendWeChatMsg();
        }
        return city;
    }

    /**
     * 验证微信服务器签名
     *
     * @return true 表示验证通过，false 表示验证失败（或参数缺失时跳过验证）
     */
    private boolean verifySignature(String signature, String timestamp, String nonce) {
        String verifyToken = wxConfig.getVerifyToken();
        // 未配置 verifyToken 时跳过验证（兼容开发环境）
        if (!StringUtils.hasText(verifyToken)) {
            log.warn("verifyToken 未配置，跳过签名验证（仅开发环境建议使用）");
            return true;
        }
        return SignatureUtil.verify(verifyToken, signature, timestamp, nonce);
    }
}
