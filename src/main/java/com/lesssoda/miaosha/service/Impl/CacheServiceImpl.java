package com.lesssoda.miaosha.service.Impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lesssoda.miaosha.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author Lee
 * @since 2021/4/2 16:53
 */
@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String, Object> commonCache = null;

    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                //设置缓存容器的初始容量大小为10
                .initialCapacity(10)
                //设置缓存中最大可以存储100个KEY,超过100个之后会按照LRU的策略移除缓存项
                .maximumSize(100)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
