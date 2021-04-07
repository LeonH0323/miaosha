package com.lesssoda.miaosha.service.Model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Lee
 * @since 2021/3/26 15:07
 */
// 用户下单的交易模型
@Data
public class OrderModel {

    private String id;

    // 用户id
    private Integer userId;

    // 商品id
    private Integer itemId;

    // 若非空，则表示以秒杀商品方式下单
    private Integer promoId;

    // 商品的单价, 若promoId非空，则表示秒杀商品价格
    private BigDecimal itemPrice;

    // 商品数量
    private Integer amount;

    // 购买金额, 若promoId非空，则表示秒杀商品价格
    private BigDecimal orderPrice;
}
