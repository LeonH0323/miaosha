package com.lesssoda.miaosha.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.error.EmBusinessError;
import com.lesssoda.miaosha.mq.MqProducer;
import com.lesssoda.miaosha.response.CommonReturnType;
import com.lesssoda.miaosha.service.ItemService;
import com.lesssoda.miaosha.service.Model.OrderModel;
import com.lesssoda.miaosha.service.Model.UserModel;
import com.lesssoda.miaosha.service.OrderService;
import com.lesssoda.miaosha.service.PromoService;
import com.lesssoda.miaosha.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

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

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);

        orderCreateRateLimiter = RateLimiter.create(100);
    }


    @RequestMapping(value = "/generateverifycode", method = {RequestMethod.POST,RequestMethod.GET})
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        // ??????token??????????????????
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"??????????????????????????????????????????");
        }
        // ???????????????????????????
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"?????????????????????????????????");
        }
        Map<String, Object> map = CodeUtil.generateCodeAndPic();

        redisTemplate.opsForValue().set("verify_code_" + userModel.getId(), map.get("code"));
        redisTemplate.expire("verify_code_" + userModel.getId(), 10, TimeUnit.MINUTES);

        ImageIO.write((RenderedImage)map.get("codePic"), "jpeg", response.getOutputStream());
        System.out.println("???????????????" + map.get("code"));

    }

    @RequestMapping(value = "generatetoken", method = {RequestMethod.POST},consumes={CONTENT_TYPE_FORMED})
    public CommonReturnType generateToken(@RequestParam(name = "itemId")Integer itemId,
                                          @RequestParam(name = "promoId")Integer promoId,
                                          @RequestParam(name = "verifyCode")String verifyCode) throws BusinessException {
        // ??????token??????????????????
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"?????????????????????????????????");
        }
        // ???????????????????????????
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"?????????????????????????????????");
        }

        // ??????verifyCode????????????????????????
        String redisVerifyCode = (String)redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (StringUtils.isEmpty(redisVerifyCode))
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "????????????");
        if (!StringUtils.equalsIgnoreCase(redisVerifyCode, verifyCode))
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "????????????,???????????????");


        // ????????????????????????
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if (promoToken == null)
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "??????????????????");
        return CommonReturnType.create(promoToken);
    }

    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes={CONTENT_TYPE_FORMED})
    public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "amount")Integer amount,
                                        @RequestParam(name = "promoId", required = false)Integer promoId,
                                        @RequestParam(name = "promoToken", required = false)String promoToken) throws BusinessException {



        if (!orderCreateRateLimiter.tryAcquire())
            throw new BusinessException(EmBusinessError.RATELIMIT);


        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token))
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"?????????????????????????????????");
        // ???????????????????????????
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null)
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"?????????????????????????????????");

        // ??????????????????????????????
        if (promoId != null) {
            String inRedisPromoToken = (String)redisTemplate.opsForValue().get("promo_token_" + promoId + "_userid_" + userModel.getId() + "_itemid_" + itemId);
            if (inRedisPromoToken == null)
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "????????????????????????");

            if (!StringUtils.equals(inRedisPromoToken, promoToken))
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "????????????????????????");
        }

//        UserModel login_user = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

//        OrderModel orderModel = orderService.createOrder(userModel.getId(), promoId, itemId, amount);

        // ????????????????????????submit??????
        // ???????????????20???????????????????????????????????????
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // ??????????????????init??????
                String stockLogId = itemService.initStockLog(itemId, amount);

                // ????????????????????????????????????????????????
                if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), promoId, itemId, amount, stockLogId)) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "????????????");
                }
                return null;
            }
        });


        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }


        return CommonReturnType.create(null);
    }
}
