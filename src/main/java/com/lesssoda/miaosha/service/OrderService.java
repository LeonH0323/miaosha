package com.lesssoda.miaosha.service;

import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.service.Model.OrderModel;

/**
 * @author Lee
 * @since 2021/3/26 15:17
 */
public interface OrderService {
    // 使用1,通过前端url上传过来秒杀活动id，然后下单接口内校验对应id是否属于对应商品且活动已开始
    // 2.直接在下单接口内判断对应的商品是否存在秒杀活动，若存在进行中的则以秒杀价格下单
    public OrderModel createOrder(Integer userId, Integer promoId, Integer itemId, Integer amount) throws BusinessException;
}