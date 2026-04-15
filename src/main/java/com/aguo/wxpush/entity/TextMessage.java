package com.aguo.wxpush.entity;

import lombok.Data;

/**
 * 微信文本消息实体
 */
@Data
public class TextMessage {

    /** 接收方 openid */
    private String toUserName;

    /** 发送方 openid */
    private String fromUserName;

    /** 消息创建时间戳 */
    private Long createTime;

    /** 消息类型 */
    private String msgType;

    /** 消息内容 */
    private String content;
}
