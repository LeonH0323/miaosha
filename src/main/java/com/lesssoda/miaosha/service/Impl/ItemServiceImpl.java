package com.lesssoda.miaosha.service.Impl;

import com.lesssoda.miaosha.dao.ItemDOMapper;
import com.lesssoda.miaosha.dao.ItemStockDOMapper;
import com.lesssoda.miaosha.dao.StockLogDOMapper;
import com.lesssoda.miaosha.dataobject.ItemDO;
import com.lesssoda.miaosha.dataobject.ItemStockDO;
import com.lesssoda.miaosha.dataobject.StockLogDO;
import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.error.EmBusinessError;
import com.lesssoda.miaosha.mq.MqProducer;
import com.lesssoda.miaosha.service.ItemService;
import com.lesssoda.miaosha.service.Model.ItemModel;
import com.lesssoda.miaosha.service.Model.PromoModel;
import com.lesssoda.miaosha.service.PromoService;
import com.lesssoda.miaosha.validator.ValidationResult;
import com.lesssoda.miaosha.validator.ValidatorImpl;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Lee
 * @since 2021/3/25 15:28
 */

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {

        // 校验入参
        ValidationResult validationResult = validator.validate(itemModel);
        if (validationResult.isHasErrors())
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        // 转换类型 ItemModel -> DataObject
        ItemDO itemDO = convertFormItemModel(itemModel);

        // 写入数据库
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());

        ItemStockDO itemStockDO = convertItemStockDOFromItemModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);

        //返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = convertModelFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + id);
        if (itemModel == null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id, itemModel);
            redisTemplate.expire("item_validate_" + id, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    @Override
    @Transactional
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null)
            return null;

        // 获取获取库存数量
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        // 将dataobject -> model
        ItemModel itemModel = convertModelFromDataObject(itemDO, itemStockDO);

        // 获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if (promoModel != null && promoModel.getStatus() != 3)
            itemModel.setPromoModel(promoModel);

        return itemModel;
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
//        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
//        if (affectedRow > 0 )
//            return true;
//        return false;

        // redis操作
        Long result = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue() * -1);
        if (result > 0){
            return true;
        }else if (result == 0){
            //库存已售罄标识
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");
            return true;
        }else{
            increaseStock(itemId, amount);
            return false;
        }
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        return true;
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemDOMapper.increaseSales(itemId, amount);
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        Boolean mqResult = mqProducer.asyncReduceStock(itemId, amount);
        return mqResult;
    }

    // 初始化订单流水
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setAmount(amount);
        stockLogDO.setItemId(itemId);
        stockLogDO.setStatus(1);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replaceAll("-",""));

        stockLogDOMapper.insertSelective(stockLogDO);

        return stockLogDO.getStockLogId();
    }

    private ItemModel convertModelFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO){
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemStockDO, itemModel);
        BeanUtils.copyProperties(itemDO, itemModel);

        return itemModel;
    }

    private ItemDO convertFormItemModel(ItemModel itemModel){
        if (itemModel == null)
            return null;
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        return itemDO;
    }
    private ItemStockDO convertItemStockDOFromItemModel(ItemModel itemModel){
        if (itemModel == null)
            return null;
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }
}
