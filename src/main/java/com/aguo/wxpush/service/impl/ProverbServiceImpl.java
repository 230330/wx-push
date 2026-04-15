package com.aguo.wxpush.service.impl;

import com.aguo.wxpush.config.WxConfigProperties;
import com.aguo.wxpush.service.ProverbService;
import com.aguo.wxpush.utils.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Random;

/**
 * 每日推送名言警句实现类
 */
@Slf4j
@Service
public class ProverbServiceImpl implements ProverbService {

    @Resource
    private WxConfigProperties wxConfig;

    private static final Random RANDOM = new Random();

    @Override
    public String getOneProverbRandom() {
        String proverb;
        do {
            proverb = null;
            try {
                String response = HttpUtil.sendGet("https://api.xygeng.cn/one", null);
                if (!StringUtils.hasText(response)) {
                    log.warn("随机名言API响应为空");
                    continue;
                }
                JSONObject jsonObject = JSONObject.parseObject(response);
                JSONObject content = jsonObject.getJSONObject("data");
                proverb = content.getString("content");
            } catch (Exception e) {
                log.error("获取随机名言失败: {}", e.getMessage(), e);
            }
        } while (proverb != null && proverb.length() > 25);
        return proverb;
    }

    @Override
    public String translateToEnglish(String sentence) {
        if (!StringUtils.hasText(sentence)) {
            return null;
        }
        try {
            String response = HttpUtil.sendGet(
                    "https://fanyi.youdao.com/translate",
                    "&doctype=json&type=AUTO&i=" + sentence);
            if (!StringUtils.hasText(response)) {
                log.warn("翻译API响应为空");
                return null;
            }
            JSONObject jsonObject = JSONObject.parseObject(response);
            return jsonObject.getJSONArray("translateResult")
                    .getJSONArray(0)
                    .getJSONObject(0)
                    .getString("tgt");
        } catch (Exception e) {
            log.error("翻译失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getOneNormalProverb() {
        try {
            // 构建POST请求体
            String body = "titleID=" + RANDOM.nextInt(9);
            // 使用自定义POST，需添加 ApiSpace 头部
            String response = HttpUtil.sendPostWithHeaders(
                    "https://eolink.o.apispace.com/myjj/common/aphorism/getAphorismList",
                    body,
                    "application/x-www-form-urlencoded",
                    buildApiSpaceHeaders());

            if (!StringUtils.hasText(response)) {
                log.warn("ApiSpace名言API响应为空");
                return null;
            }

            JSONObject jsonObject = JSONObject.parseObject(response);
            JSONArray resultArray = jsonObject.getJSONArray("result");
            if (resultArray == null || resultArray.isEmpty()) {
                log.warn("ApiSpace名言返回结果为空");
                return null;
            }

            // 取出全部句子
            String wordsJson = (String) resultArray.getJSONObject(0).get("words");
            JSONArray allProverb = JSONObject.parseArray(wordsJson);
            if (allProverb == null || allProverb.isEmpty()) {
                return null;
            }

            // 随机取出一条句子，去除序号前缀
            String raw = (String) allProverb.get(RANDOM.nextInt(allProverb.size()));
            return raw.replaceAll("^.*、", "");
        } catch (Exception e) {
            log.error("获取ApiSpace名言失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建 ApiSpace 请求头
     */
    private String[][] buildApiSpaceHeaders() {
        return new String[][]{
                {"X-APISpace-Token", wxConfig.getToken()},
                {"Authorization-Type", "apikey"}
        };
    }
}
