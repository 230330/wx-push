package com.aguo.wxpush.exception;

/**
 * 微信推送服务业务异常
 */
public class WxPushException extends RuntimeException {

    public WxPushException(String message) {
        super(message);
    }

    public WxPushException(String message, Throwable cause) {
        super(message, cause);
    }
}
