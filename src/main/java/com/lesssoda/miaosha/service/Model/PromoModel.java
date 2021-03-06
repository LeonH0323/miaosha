package com.lesssoda.miaosha.service.Model;

import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Lee
 * @since 2021/3/26 16:50
 */
@Data
public class PromoModel implements Serializable {

    private Integer id;

    // 秒杀活动状态 1表示还未开始， 2表示进行中， 3表示已经结束
    private Integer status;

    // 秒杀活动名称
    private String promoName;

    // 秒杀活动的开始时间
    private DateTime startDate;

    // 秒杀活动的结束时间
    private DateTime endDate;

    // 秒杀活动的适用商品
    private Integer itemId;

    // 秒杀活动的商品价格
    private BigDecimal promoItemPrice;
}
