package com.lesssoda.miaosha.controller;

import com.lesssoda.miaosha.controller.viewobject.ItemVO;
import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.response.CommonReturnType;
import com.lesssoda.miaosha.service.CacheService;
import com.lesssoda.miaosha.service.ItemService;
import com.lesssoda.miaosha.service.Model.ItemModel;
import com.lesssoda.miaosha.service.PromoService;
import org.checkerframework.checker.units.qual.A;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Lee
 * @since 2021/3/25 16:02
 */
@RestController
@RequestMapping("/item")
@CrossOrigin(allowCredentials="true", allowedHeaders = "*")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;


    // 创建商品
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType createItem(@RequestParam(name = "title")String title,
                                       @RequestParam(name = "description")String description,
                                       @RequestParam(name = "price")BigDecimal price,
                                       @RequestParam(name = "stock")Integer stock,
                                       @RequestParam(name = "imgUrl")String imgUrl) throws BusinessException {

        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel item = itemService.createItem(itemModel);
        ItemVO itemVO = convertFromItemModel(item);
        return CommonReturnType.create(itemVO);
    }
    //商品详情页浏览
    @RequestMapping(value = "/get",method = {RequestMethod.GET})
    public CommonReturnType getItem(@RequestParam(name = "id")Integer id){

        ItemModel itemModel = null;

        // 先在本地缓存取
        itemModel = (ItemModel) cacheService.getCommonCache("item_" + id);

        if (itemModel == null){
            // 根据商品的id到redis内获取
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);

            if (itemModel == null){
                itemModel = itemService.getItemById(id);
                redisTemplate.opsForValue().set("item_" + id, itemModel);
                redisTemplate.expire("item_" + id, 10, TimeUnit.MINUTES);
            }
            // 添加本地缓存
            cacheService.setCommonCache("item_" + id, itemModel);
        }



        ItemVO itemVO = convertFromItemModel(itemModel);

        return CommonReturnType.create(itemVO);

    }

    // 发布活动，将库存存入redis
    @RequestMapping(value = "/publishPromo", method = {RequestMethod.GET})
    public CommonReturnType publishPromo(@RequestParam("promoId") Integer promoId){
        promoService.publishPromo(promoId);
        return CommonReturnType.create(null);
    }

    // 商品列表
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    public CommonReturnType listItem(){
        List<ItemModel> itemModels = itemService.listItem();
        List<ItemVO> itemVOList = itemModels.stream().map(itemModel -> {
            ItemVO itemVO = convertFromItemModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }

    private ItemVO convertFromItemModel(ItemModel itemModel){
        if (itemModel == null)
            return null;
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if(itemModel.getPromoModel() != null){
            //有正在进行或即将进行的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }
}
