package com.lesssoda.miaosha.service;

/**
 * @author Lee
 * @since 2021/4/2 16:51
 */
//封装本地缓存操作类
public interface CacheService {
    //存方法
    void setCommonCache(String key, Object value);

    //取方法
    Object getCommonCache(String key);
}
