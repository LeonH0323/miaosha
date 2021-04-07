package com.lesssoda.miaosha.service;

import com.lesssoda.miaosha.dataobject.UserDO;
import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.service.Model.UserModel;
import org.springframework.stereotype.Service;

/**
 * @author Lee
 * @since 2021/3/23 17:12
 */
public interface UserService {
    // 通过用户ID获取对象
    UserModel getUserById(Integer id);
    // 通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    void register(UserModel userModel) throws BusinessException;
    // 登录验证
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
