package com.lesssoda.miaosha.controller;

import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.error.EmBusinessError;
import com.lesssoda.miaosha.mq.MqProducer;
import com.lesssoda.miaosha.response.CommonReturnType;
import com.lesssoda.miaosha.service.ItemService;
import com.lesssoda.miaosha.service.Model.OrderModel;
import com.lesssoda.miaosha.service.Model.UserModel;
import com.lesssoda.miaosha.service.OrderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Lee
 * @since 2021/3/26 16:07
 */
@RestController
@RequestMapping("/order")
@CrossOrigin(allowCredentials="true", allowedHeaders = "*")
public class OrderController extends BaseController{

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes={CONTENT_TYPE_FORMED})
    public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "amount")Integer amount,
                                        @RequestParam(name = "promoId", required = false)Integer promoId) throws BusinessException {

//        Boolean is_login = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");

        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token))
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);

        if (userModel == null)
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
//        UserModel login_user = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

//        OrderModel orderModel = orderService.createOrder(userModel.getId(), promoId, itemId, amount);

        // 判断是否库存售罄，若对应的售罄key存在，则直接返回下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        // 加入库存流水init状态
        String stockLogId = itemService.initStockLog(itemId, amount);

        // 再去完成对应的下单事务性消息机制
        if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), promoId, itemId, amount, stockLogId)){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
        }

        return CommonReturnType.create(null);
    }
}
