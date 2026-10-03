package com.aguo.wxpush.utils;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 和风天气 QWeather API JWT 鉴权工具类
 * 参考官方文档: https://dev.qweather.com/docs/authentication/jwt/
 *
 * JWT 由三部分组成：
 * - Header:  {"alg":"EdDSA","kid":"<凭据ID>"}
 * - Payload: {"iss":"<开发者ID>","sub":"<项目ID>","iat":<签发时间>,"exp":<过期时间>}
 * - Signature: 使用 Ed25519(EdDSA) 私钥对 header.payload 签名
 *
 * 私钥支持两种格式：
 * - PKCS8 PEM 文本（含 -----BEGIN PRIVATE KEY----- 标记）
 * - Base64 编码的 PKCS8 字节（便于通过环境变量配置）
 */
public class QWeatherJwtUtil {

    private static final Logger log = LoggerFactory.getLogger(QWeatherJwtUtil.class);

    /** 私钥 PEM 标记 */
    private static final String PEM_BEGIN_MARKER = "-----BEGIN";

    /** Token 有效期（秒），官方要求不超过 86400 */
    private static final long TOKEN_EXPIRE_SECONDS = 900L;

    private QWeatherJwtUtil() {
    }

    /**
     * 生成和风天气 API 鉴权 JWT
     *
     * @param credentialId 凭据ID（kid，控制台 -> 项目管理 -> 凭据）
     * @param developerId  开发者ID（iss，控制台 -> 设置）
     * @param projectId    项目ID（sub，控制台 -> 项目管理）
     * @param privateKeyInput Ed25519 私钥（PEM 文本或 Base64）
     * @return JWT 字符串，失败返回 null
     */
    public static String createToken(String credentialId, String developerId,
                                     String projectId, String privateKeyInput) {
        // 入口校验：避免空私钥触发底层解码异常（如 ArrayIndexOutOfBoundsException）
        if (credentialId == null || credentialId.trim().isEmpty()
                || developerId == null || developerId.trim().isEmpty()
                || projectId == null || projectId.trim().isEmpty()
                || privateKeyInput == null || privateKeyInput.trim().isEmpty()) {
            log.error("生成和风天气 JWT 失败：鉴权参数缺失（凭据ID/开发者ID/项目ID/私钥均不能为空）");
            return null;
        }
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
        try {
            byte[] keyBytes = parsePrivateKey(privateKeyInput);
            EdDSAPrivateKey privateKey = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(keyBytes));

            long iat = System.currentTimeMillis() / 1000L - 30L;
            long exp = iat + TOKEN_EXPIRE_SECONDS;

            String headerJson = "{\"alg\":\"EdDSA\",\"kid\":\"" + credentialId + "\"}";
            String payloadJson = "{\"iss\":\"" + developerId + "\",\"sub\":\"" + projectId
                    + "\",\"iat\":" + iat + ",\"exp\":" + exp + "}";

            String headerEncoded = base64UrlEncode(headerJson);
            String payloadEncoded = base64UrlEncode(payloadJson);
            String data = headerEncoded + "." + payloadEncoded;

            Signature signer = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
            signer.initSign(privateKey);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            String signatureEncoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signer.sign());

            return data + "." + signatureEncoded;
        } catch (InvalidKeySpecException e) {
            log.error("生成和风天气 JWT 失败：私钥格式无效（需 PKCS8 PEM 文本或 Base64 编码）: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("生成和风天气 JWT 失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析私钥：兼容 PEM 文本与纯 Base64 两种格式
     */
    private static byte[] parsePrivateKey(String input) {
        String content = input.trim();
        if (content.contains(PEM_BEGIN_MARKER)) {
            content = content
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
        }
        return Base64.getDecoder().decode(content);
    }

    /**
     * Base64URL 编码（去填充，JWT 规范）
     */
    private static String base64UrlEncode(String content) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }
}
