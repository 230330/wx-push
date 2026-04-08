package com.aguo.wxpush.service;

/**
 * <p>Title:每日推送名言警句接口</p>
 * <p>Description:</p>
 * @param
 * @return
 * @throws Exception
 * @author hezf
 * @date 2023/3/27
 */
public interface ProverbService {
    /**
     * 这个接口很奇怪，可能会返回奇奇怪怪的句子。但是这个无需注册申请免费用
     *
     * @return
     */
    String getOneProverbRandom();

    String translateToEnglish(String sentence);

    /**
     * 得到正常的句子，需要注册申请。
     * @return
     */
    String getOneNormalProverb();
}
