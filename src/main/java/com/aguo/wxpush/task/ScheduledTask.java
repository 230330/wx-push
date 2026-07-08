package com.aguo.wxpush.task;

import com.aguo.wxpush.service.SendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时任务调度
 * 将定时任务从 Controller 中独立出来，遵循单一职责原则
 */
@Slf4j
@Component
public class ScheduledTask {

    @Resource
    private SendService sendService;

    /**
     * 每日定时推送
     * cron: 每天早上 8:30 执行
     */
    @Scheduled(cron = "0 30 8 ? * *")
    public void dailyPush() {
        log.info("定时任务触发：开始每日消息推送...");
        try {
            String result = sendService.sendWeChatMsg();
            log.info("定时推送结果: {}", result);
        } catch (Exception e) {
            log.error("定时推送异常: {}", e.getMessage(), e);
        }
    }
}
