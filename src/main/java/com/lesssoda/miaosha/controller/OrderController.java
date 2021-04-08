package com.lesssoda.miaosha.controller;

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

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);
    }


    @RequestMapping(value = "/generateverifycode", method = {RequestMethod.POST,RequestMethod.GET})
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        // 根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能生成验证码");
        }
        // 获取用户的登陆信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }
        Map<String, Object> map = CodeUtil.generateCodeAndPic();

        redisTemplate.opsForValue().set("verify_code_" + userModel.getId(), map.get("code"));
        redisTemplate.expire("verify_code_" + userModel.getId(), 10, TimeUnit.MINUTES);

        ImageIO.write((RenderedImage)map.get("codePic"), "jpeg", response.getOutputStream());
        System.out.println("验证码为：" + map.get("code"));

    }

    @RequestMapping(value = "generatetoken", method = {RequestMethod.POST},consumes={CONTENT_TYPE_FORMED})
    public CommonReturnType generateToken(@RequestParam(name = "itemId")Integer itemId,
                                          @RequestParam(name = "promoId")Integer promoId,
                                          @RequestParam(name = "verifyCode")String verifyCode) throws BusinessException {
        // 根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }
        // 获取用户的登陆信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }

        // 同过verifyCode验证验证码有效性
        String redisVerifyCode = (String)redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (StringUtils.isEmpty(redisVerifyCode))
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "非法请求");
        if (!StringUtils.equalsIgnoreCase(redisVerifyCode, verifyCode))
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "非法请求,验证码错误");


        // 获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if (promoToken == null)
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        return CommonReturnType.create(promoToken);
    }

    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes={CONTENT_TYPE_FORMED})
    public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "amount")Integer amount,
                                        @RequestParam(name = "promoId", required = false)Integer promoId,
                                        @RequestParam(name = "promoToken", required = false)String promoToken) throws BusinessException {




        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token))
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        // 获取用户的登录信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null)
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");

        // 校验秒杀令牌是否正确
        if (promoId != null) {
            String inRedisPromoToken = (String)redisTemplate.opsForValue().get("promo_token_" + promoId + "_userid_" + userModel.getId() + "_itemid_" + itemId);
            if (inRedisPromoToken == null)
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");

            if (!StringUtils.equals(inRedisPromoToken, promoToken))
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
        }

//        UserModel login_user = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

//        OrderModel orderModel = orderService.createOrder(userModel.getId(), promoId, itemId, amount);

        // 同步调用线程池的submit方法
        // 拥塞窗口为20的等待队列，用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // 加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                // 再去完成对应的下单事务性消息机制
                if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), promoId, itemId, amount, stockLogId)) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
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
