package com.lesssoda.miaosha.service.Impl;

import com.lesssoda.miaosha.dao.PromoDOMapper;
import com.lesssoda.miaosha.dataobject.PromoDO;
import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.error.EmBusinessError;
import com.lesssoda.miaosha.service.ItemService;
import com.lesssoda.miaosha.service.Model.ItemModel;
import com.lesssoda.miaosha.service.Model.PromoModel;
import com.lesssoda.miaosha.service.Model.UserModel;
import com.lesssoda.miaosha.service.PromoService;
import com.lesssoda.miaosha.service.UserService;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Lee
 * @since 2021/3/26 17:24
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;


    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        // 获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoModel == null)
            return null;
        // 判断当前时间是否秒杀活动即将开始或正在进行
        if (promoModel.getStartDate().isAfterNow())
            promoModel.setStatus(1);
        else if (promoModel.getEndDate().isBeforeNow())
            promoModel.setStatus(3);
        else
            promoModel.setStatus(2);
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {

        // 通过活动id获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO == null || promoDO.getItemId().intValue() == 0)
            return;

        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());

        // 将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());

        // 将大闸的限制数字设到redis内
        redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock().intValue() * 5);

    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) throws BusinessException {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoModel == null)
            return null;
        // 判断当前时间是否秒杀活动即将开始或正在进行
        if (promoModel.getStartDate().isAfterNow())
            promoModel.setStatus(1);
        else if (promoModel.getEndDate().isBeforeNow())
            promoModel.setStatus(3);
        else
            promoModel.setStatus(2);

        // 判断item信息是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null)
            return null;
        // 判断用户信息是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if(userModel == null)
            return null;

        // 获取秒杀大闸的count数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (result < 0)
            return null;

        // 生成token并且存入redis内并给一个5分钟的有效期
        String token = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set("promo_token_" + promoId + "_userid_" + userId + "_itemid_" + itemId, token);
        redisTemplate.expire("promo_token_" + promoId + "_userid_" + userId + "_itemid_" + itemId, 5, TimeUnit.MINUTES);

        return token;
    }

    private PromoModel convertFromDataObject(PromoDO promoDO){
        if(promoDO == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
