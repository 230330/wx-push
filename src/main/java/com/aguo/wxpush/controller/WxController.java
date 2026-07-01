package com.aguo.wxpush.controller;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.service.SendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

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
     * 每日定时推送 + 手动触发推送
     * cron: 每天早上8:30执行
     */
    @Scheduled(cron = "0 30 8 ? * *")
    @GetMapping("/send")
    public String send() {
        log.info("开始执行消息推送...");
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
        String normalizedCity = normalizeCity(city);
        wxConfig.setCity(normalizedCity);

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");
        response.getWriter().write("<h1>更新成功！当前城市: " + normalizedCity + "</h1>");
        sendService.sendWeChatMsg();
    }

    /**
     * 接收微信消息回调
     */
    @GetMapping("/receiveMsg")
    public String verify(@RequestParam(required = false) String echostr) {
        // 微信接入验证
        log.info("微信接入验证, echostr={}", echostr);
        return echostr;
    }

    @PostMapping("/receiveMsg")
    public String receiveMsg(HttpServletRequest request, HttpServletResponse response) {
        log.info("接收微信消息回调");
        String city = sendService.messageHandle(request, response);
        if (city != null && !city.isEmpty()) {
            String normalizedCity = normalizeCity(city);
            wxConfig.setCity(normalizedCity);
            sendService.sendWeChatMsg();
        }
        return city;
    }

    /**
     * 去除城市名称中的省/市/区/县后缀
     */
    private String normalizeCity(String city) {
        if (city == null || city.isEmpty()) {
            return city;
        }
        if (city.contains("省") || city.contains("市") || city.contains("区") || city.contains("县")) {
            return city.substring(0, city.length() - 1);
        }
        return city;
    }
}
