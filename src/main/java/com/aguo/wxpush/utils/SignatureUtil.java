package com.aguo.wxpush.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 微信消息签名验证工具类
 * <p>
 * 微信服务器在发送消息回调时，会在 URL 上附带 signature、timestamp、nonce 参数。
 * 服务器需要验证 signature 是否等于 sha1(sort(token, timestamp, nonce))，
 * 以防止请求被伪造。
 * </p>
 */
public class SignatureUtil {

    /**
     * 验证微信服务器签名
     *
     * @param token     微信服务器配置的 Token（公众号后台设置）
     * @param signature 微信传来的签名
     * @param timestamp 微信传来的时间戳
     * @param nonce     微信传来的随机数
     * @return true 表示验证通过
     */
    public static boolean verify(String token, String signature, String timestamp, String nonce) {
        if (token == null || signature == null || timestamp == null || nonce == null) {
            return false;
        }

        // 1. 将 token、timestamp、nonce 按字典序排序
        String[] arr = {token, timestamp, nonce};
        Arrays.sort(arr);

        // 2. 拼接成一个字符串
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s);
        }

        // 3. SHA1 加密
        String sha1Result = sha1(sb.toString());

        // 4. 与 signature 比较（忽略大小写）
        return sha1Result != null && sha1Result.equalsIgnoreCase(signature);
    }

    /**
     * SHA1 加密
     */
    private static String sha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
