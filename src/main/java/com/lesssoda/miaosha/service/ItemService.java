package com.lesssoda.miaosha.service;

import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.service.Model.ItemModel;

import java.util.List;

/**
 * @author Lee
 * @since 2021/3/25 15:27
 */
public interface ItemService {
    // 创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;


    // 商品列表浏览
    List<ItemModel> listItem();


    // item 及 promo model 缓存模型
    ItemModel getItemByIdInCache(Integer id);

    // 商品详情浏览
    ItemModel getItemById(Integer id);

    // 库存扣减
    boolean decreaseStock(Integer itemId, Integer amount);

    // 库存回补
    boolean increaseStock(Integer itemId, Integer amount);

    // 商品销量增加
    void increaseSales(Integer itemId, Integer amount);

    // 异步更新库存
    boolean asyncDecreaseStock(Integer itemId, Integer amount);

}
