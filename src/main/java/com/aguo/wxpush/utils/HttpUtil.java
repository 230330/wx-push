package com.aguo.wxpush.utils;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP工具类 - 基于OkHttp实现
 * 提供GET和POST请求方法，支持超时配置和完善的错误处理
 */
public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    
    // 默认超时配置（秒）
    private static final int DEFAULT_CONNECT_TIMEOUT = 10;
    private static final int DEFAULT_READ_TIMEOUT = 30;
    private static final int DEFAULT_WRITE_TIMEOUT = 30;
    
    // 单例OkHttpClient，提高性能
    private static final OkHttpClient defaultClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    /**
     * 向指定URL发送GET方法的请求（使用默认超时配置）
     *
     * @param url   发送请求的URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式
     * @return URL所代表远程资源的响应结果，失败返回null
     */
    public static String sendGet(String url, String param) {
        return sendGet(url, param, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }
    
    /**
     * 向指定URL发送GET方法的请求（自定义超时配置）
     *
     * @param url            发送请求的URL
     * @param param          请求参数，格式：name1=value1&name2=value2
     * @param connectTimeout 连接超时时间（秒）
     * @param readTimeout    读取超时时间（秒）
     * @return URL所代表远程资源的响应结果，失败返回null
     */
    public static String sendGet(String url, String param, int connectTimeout, int readTimeout) {
        if (url == null || url.trim().isEmpty()) {
            logger.error("GET请求失败：URL不能为空");
            return null;
        }
        
        // 构建完整URL
        String fullUrl = url;
        if (param != null && !param.trim().isEmpty()) {
            fullUrl = url + "?" + param;
        }
        
        // 创建自定义超时的客户端
        OkHttpClient client = defaultClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();
        
        // 构建请求
        Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .addHeader("accept", "*/*")
                .addHeader("connection", "Keep-Alive")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        
        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("GET请求失败：{}, HTTP状态码：{}", fullUrl, response.code());
                return null;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                logger.warn("GET请求响应body为空：{}", fullUrl);
                return null;
            }
            
            String result = body.string();
            logger.debug("GET请求成功：{}", fullUrl);
            return result;
            
        } catch (IOException e) {
            logger.error("发送GET请求出现IO异常：{}, 错误信息：{}", fullUrl, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("发送GET请求出现未知异常：{}, 错误信息：{}", fullUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 向指定URL发送POST方法的请求（使用默认超时配置）
     *
     * @param url   发送请求的URL
     * @param param 请求参数，JSON格式字符串
     * @return 所代表远程资源的响应结果，失败返回null
     */
    public static String sendPost(String url, String param) {
        return sendPost(url, param, DEFAULT_CONNECT_TIMEOUT, DEFAULT_WRITE_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }
    
    /**
     * 向指定URL发送POST方法的请求（自定义超时配置）
     *
     * @param url            发送请求的URL
     * @param param          请求参数，JSON格式字符串
     * @param connectTimeout 连接超时时间（秒）
     * @param writeTimeout   写入超时时间（秒）
     * @param readTimeout    读取超时时间（秒）
     * @return 所代表远程资源的响应结果，失败返回null
     */
    public static String sendPost(String url, String param, int connectTimeout, int writeTimeout, int readTimeout) {
        if (url == null || url.trim().isEmpty()) {
            logger.error("POST请求失败：URL不能为空");
            return null;
        }
        
        if (param == null) {
            param = "";
        }
        
        // 创建自定义超时的客户端
        OkHttpClient client = defaultClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();
        
        // 构建请求体
        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        RequestBody body = RequestBody.create(mediaType, param);
        
        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("Accept", "application/json;charset=UTF-8")
                .addHeader("Accept-Charset", "UTF-8")
                .addHeader("connection", "Keep-Alive")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        
        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("POST请求失败：{}, HTTP状态码：{}", url, response.code());
                return null;
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                logger.warn("POST请求响应body为空：{}", url);
                return null;
            }
            
            String result = responseBody.string();
            logger.debug("POST请求成功：{}", url);
            return result;
            
        } catch (IOException e) {
            logger.error("发送POST请求出现IO异常：{}, 错误信息：{}", url, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("发送POST请求出现未知异常：{}, 错误信息：{}", url, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取默认的OkHttpClient实例
     * 可用于需要自定义请求的场景
     *
     * @return OkHttpClient实例
     */
    public static OkHttpClient getDefaultClient() {
        return defaultClient;
    }
    
    /**
     * 创建自定义超时配置的OkHttpClient
     *
     * @param connectTimeout 连接超时（秒）
     * @param readTimeout    读取超时（秒）
     * @param writeTimeout   写入超时（秒）
     * @return 自定义配置的OkHttpClient
     */
    public static OkHttpClient createCustomClient(int connectTimeout, int readTimeout, int writeTimeout) {
        return defaultClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
    }
}
