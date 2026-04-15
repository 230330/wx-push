package com.aguo.wxpush.exception;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WxPushException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleWxPushException(WxPushException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        JSONObject result = new JSONObject();
        result.put("code", 500);
        result.put("msg", e.getMessage());
        return result.toJSONString();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        JSONObject result = new JSONObject();
        result.put("code", 500);
        result.put("msg", "系统内部错误");
        return result.toJSONString();
    }
}
