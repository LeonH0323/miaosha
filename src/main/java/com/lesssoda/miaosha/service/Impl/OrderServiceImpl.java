package com.lesssoda.miaosha.service.Impl;

import com.lesssoda.miaosha.dao.OrderDOMapper;
import com.lesssoda.miaosha.dao.SequenceDOMapper;
import com.lesssoda.miaosha.dao.StockLogDOMapper;
import com.lesssoda.miaosha.dataobject.OrderDO;
import com.lesssoda.miaosha.dataobject.SequenceDO;
import com.lesssoda.miaosha.dataobject.StockLogDO;
import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.error.EmBusinessError;
import com.lesssoda.miaosha.service.ItemService;
import com.lesssoda.miaosha.service.Model.ItemModel;
import com.lesssoda.miaosha.service.Model.OrderModel;
import com.lesssoda.miaosha.service.Model.UserModel;
import com.lesssoda.miaosha.service.OrderService;
import com.lesssoda.miaosha.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Lee
 * @since 2021/3/26 15:19
 */

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer promoId, Integer itemId, Integer amount, String stockLogId) throws BusinessException {
        // 1. 校验下单状态， 商品是否存在， 用户是否合法， 购买数量是否正确
//        ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null)
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");


//        UserModel userModel = userService.getUserById(userId);
//        UserModel userModel = userService.getUserByIdInCache(userId);
//        if(userModel == null)
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
        if(amount <= 0 || amount > 99)
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");

        //校验活动信息
//        if (promoId != null){
//            if (promoId != itemModel.getPromoModel().getId())
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
//            else if (itemModel.getPromoModel().getStatus() != 2)
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动还未开始");
//        }

        // 2. 落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result)
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);

        // 3. 订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);

        if (promoId != null)
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        else
            orderModel.setItemPrice(itemModel.getPrice());

        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        orderModel.setPromoId(promoId);

        //生成交易流水号
        orderModel.setId(generateOrderNo());

        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        // 设置商品销量
        itemService.increaseSales(itemId, amount);

        // 设置库存流水状态为成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDO == null)
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        // 异步更新库存

//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @Override
//            public void afterCommit() {
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
////                if (mqResult == false){
////                    itemService.increaseStock(itemId, amount);
////                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });


        // 4. 返回前端
        return null;
    }
    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        return orderDO;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String generateOrderNo(){
        // 订单号设置为16位
        StringBuilder stringBuilder = new StringBuilder();
        // 前八位是时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.BASIC_ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        //中间六位是自增序列，确保订单唯一性
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.selectByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequence + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for(int i = 0; i < 6-sequenceStr.length();i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后2位为分库分表位,暂时写死
        stringBuilder.append("00");

        return stringBuilder.toString();
    }

}
