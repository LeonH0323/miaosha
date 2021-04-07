package com.lesssoda.miaosha.controller;

import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.error.EmBusinessError;
import com.lesssoda.miaosha.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Lee
 * @since 2021/3/23 19:37
 */
public class BaseController {

    public static final String CONTENT_TYPE_FORMED = "application/x-www-form-urlencoded";

//
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.OK)
//    public Object handlerException(HttpServletRequest request, Exception e){
//        HashMap<String, Object> responseData = new HashMap<>();
//        if (e instanceof BusinessException){
//            BusinessException businessException = (BusinessException) e;
//            responseData.put("errCode", businessException.getErrCode());
//            responseData.put("errMsg", businessException.getErrMsg());
//        }else {
//            responseData.put("errCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
//            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrMsg());
//        }
//        return CommonReturnType.create(responseData, "fail");
//    }
}
