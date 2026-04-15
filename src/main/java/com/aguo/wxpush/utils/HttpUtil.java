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

    private static final int DEFAULT_CONNECT_TIMEOUT = 10;
    private static final int DEFAULT_READ_TIMEOUT = 30;
    private static final int DEFAULT_WRITE_TIMEOUT = 30;

    private static final OkHttpClient defaultClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    /**
     * GET请求（默认超时）
     */
    public static String sendGet(String url, String param) {
        return sendGet(url, param, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * GET请求（自定义超时）
     */
    public static String sendGet(String url, String param, int connectTimeout, int readTimeout) {
        if (url == null || url.trim().isEmpty()) {
            logger.error("GET请求失败：URL不能为空");
            return null;
        }

        String fullUrl = url;
        if (param != null && !param.trim().isEmpty()) {
            fullUrl = url + "?" + param;
        }

        OkHttpClient client = defaultClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .addHeader("accept", "*/*")
                .addHeader("connection", "Keep-Alive")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

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
            logger.error("GET请求IO异常：{}, 错误：{}", fullUrl, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("GET请求未知异常：{}, 错误：{}", fullUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * POST请求 - JSON格式（默认超时）
     */
    public static String sendPost(String url, String param) {
        return sendPost(url, param, DEFAULT_CONNECT_TIMEOUT, DEFAULT_WRITE_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * POST请求 - JSON格式（自定义超时）
     */
    public static String sendPost(String url, String param, int connectTimeout, int writeTimeout, int readTimeout) {
        if (url == null || url.trim().isEmpty()) {
            logger.error("POST请求失败：URL不能为空");
            return null;
        }
        if (param == null) {
            param = "";
        }

        OkHttpClient client = defaultClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();

        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        RequestBody body = RequestBody.create(mediaType, param);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("Accept", "application/json;charset=UTF-8")
                .addHeader("Accept-Charset", "UTF-8")
                .addHeader("connection", "Keep-Alive")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

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
            logger.error("POST请求IO异常：{}, 错误：{}", url, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("POST请求未知异常：{}, 错误：{}", url, e.getMessage(), e);
            return null;
        }
    }

    /**
     * POST请求 - 支持自定义ContentType和额外Headers
     *
     * @param url        请求URL
     * @param body       请求体内容
     * @param contentType 内容类型，如 "application/x-www-form-urlencoded"
     * @param headers    额外的请求头，格式为二维数组 {{"key", "value"}, ...}
     * @return 响应结果，失败返回null
     */
    public static String sendPostWithHeaders(String url, String body, String contentType, String[][] headers) {
        if (url == null || url.trim().isEmpty()) {
            logger.error("POST请求失败：URL不能为空");
            return null;
        }
        if (body == null) {
            body = "";
        }

        MediaType mediaType = MediaType.parse(contentType + ";charset=UTF-8");
        RequestBody requestBody = RequestBody.create(mediaType, body);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", "*/*")
                .addHeader("connection", "Keep-Alive");

        if (headers != null) {
            for (String[] header : headers) {
                if (header.length == 2) {
                    builder.addHeader(header[0], header[1]);
                }
            }
        }

        Request request = builder.build();

        try (Response response = defaultClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("POST请求失败：{}, HTTP状态码：{}", url, response.code());
                return null;
            }
            ResponseBody respBody = response.body();
            if (respBody == null) {
                logger.warn("POST请求响应body为空：{}", url);
                return null;
            }
            return respBody.string();
        } catch (IOException e) {
            logger.error("POST请求IO异常：{}, 错误：{}", url, e.getMessage(), e);
            return null;
        }
    }

    public static OkHttpClient getDefaultClient() {
        return defaultClient;
    }

    public static OkHttpClient createCustomClient(int connectTimeout, int readTimeout, int writeTimeout) {
        return defaultClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
    }
}
