package com.lesssoda.miaosha.service;

import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.service.Model.PromoModel;

/**
 * @author Lee
 * @since 2021/3/26 17:23
 */
public interface PromoService {
    //根据itemId获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);

    // 活动发布
    void publishPromo(Integer promoId);

    // 生成秒杀用的令牌
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) throws BusinessException;
}
