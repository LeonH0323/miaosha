package com.lesssoda.miaosha.service.Impl;

import com.lesssoda.miaosha.dao.UserDOMapper;
import com.lesssoda.miaosha.dao.UserPasswordDOMapper;
import com.lesssoda.miaosha.dataobject.UserDO;
import com.lesssoda.miaosha.dataobject.UserPasswordDO;
import com.lesssoda.miaosha.error.BusinessException;
import com.lesssoda.miaosha.error.EmBusinessError;
import com.lesssoda.miaosha.service.Model.UserModel;
import com.lesssoda.miaosha.service.UserService;
import com.lesssoda.miaosha.validator.ValidationResult;
import com.lesssoda.miaosha.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author Lee
 * @since 2021/3/23 17:13
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;
    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;
    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private RedisTemplate redisTemplate;



    @Override
    public UserModel getUserById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);

        if (userDO == null)
            return  null;

        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(id);
        return convertFromDataObject(userDO, userPasswordDO);
    }

    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_" + id);
        if (userModel == null){
            userModel = this.getUserById(id);
            redisTemplate.opsForValue().set("user_validate_" + id, userModel);
            redisTemplate.expire("user_validate_" + id, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        if (userModel == null)
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);

        ValidationResult result = validator.validate(userModel);
        if (result.isHasErrors())
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());


        UserDO userDO = convertFromModel(userModel);
        try {
            userDOMapper.insertSelective(userDO);
        } catch (DuplicateKeyException  ex){
            throw  new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号已重复");
        }

        userModel.setId(userDO.getId());

        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);

        return;
    }

    @Override
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException {
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if (userDO == null)
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);

        if (!StringUtils.equals(encrptPassword, userModel.getEncrptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        return userModel;
    }


    private UserPasswordDO convertPasswordFromModel(UserModel userModel){
        if (userModel == null)
            return null;
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }

    private UserDO convertFromModel(UserModel userModel){
        if (userModel == null)
            return null;
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }

    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO){
        if (userDO == null || userPasswordDO == null)
            return null;
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);
        userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        return userModel;
    }
}
