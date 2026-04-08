package com.aguo.wxpush.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Title:公众号消息推送接口</p>
 * <p>Description:</p>
 * @throws Exception
 * @author hezf
 * @date 2023/3/27
 */
public interface SendService {
    String sendWeChatMsg();
    String messageHandle(HttpServletRequest request, HttpServletResponse response);
}
