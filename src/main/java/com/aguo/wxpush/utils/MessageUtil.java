package com.aguo.wxpush.utils;

import com.aguo.wxpush.entity.TextMessage;
import com.thoughtworks.xstream.XStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信消息工具类
 * 提供XML消息解析和消息类型常量
 */
public class MessageUtil {

    private static final Logger logger = LoggerFactory.getLogger(MessageUtil.class);

    // ========== 响应消息类型 ==========
    public static final String RESP_MESSAGE_TYPE_TEXT = "text";
    public static final String RESP_MESSAGE_TYPE_MUSIC = "music";
    public static final String RESP_MESSAGE_TYPE_NEWS = "news";
    public static final String RESP_MESSAGE_TYPE_IMAGE = "image";
    public static final String RESP_MESSAGE_TYPE_VOICE = "voice";
    public static final String RESP_MESSAGE_TYPE_VIDEO = "video";

    // ========== 请求消息类型 ==========
    public static final String REQ_MESSAGE_TYPE_TEXT = "text";
    public static final String REQ_MESSAGE_TYPE_IMAGE = "image";
    public static final String REQ_MESSAGE_TYPE_LINK = "link";
    public static final String REQ_MESSAGE_TYPE_LOCATION = "location";
    public static final String REQ_MESSAGE_TYPE_VOICE = "voice";
    public static final String REQ_MESSAGE_TYPE_VIDEO = "video";
    public static final String REQ_MESSAGE_TYPE_EVENT = "event";

    // ========== 事件类型 ==========
    public static final String EVENT_TYPE_SUBSCRIBE = "subscribe";
    public static final String EVENT_TYPE_UNSUBSCRIBE = "unsubscribe";
    public static final String EVENT_TYPE_CLICK = "CLICK";
    public static final String EVENT_TYPE_VIEW = "VIEW";
    public static final String EVENT_TYPE_LOCATION = "LOCATION";
    public static final String EVENT_TYPE_SCAN = "SCAN";

    /**
     * 解析微信发来的请求（XML格式）
     *
     * @param request HttpServletRequest
     * @return 解析后的键值对Map
     */
    public static Map<String, String> parseXml(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>(16);
        SAXReader reader = new SAXReader();

        try (InputStream inputStream = request.getInputStream()) {
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();
            List<Element> elementList = root.elements();
            for (Element element : elementList) {
                map.put(element.getName(), element.getStringValue());
            }
        } catch (DocumentException e) {
            logger.error("XML解析失败: {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("读取请求流失败: {}", e.getMessage(), e);
        }

        return map;
    }

    /**
     * 文本消息对象转换成XML
     *
     * @param textMessage 文本消息对象
     * @return XML格式字符串
     */
    public static String textMessageToXml(TextMessage textMessage) {
        XStream xStream = new XStream();
        xStream.alias("xml", textMessage.getClass());
        return xStream.toXML(textMessage);
    }
}
