package com.lesssoda.miaosha.error;

/**
 * @author Lee
 * @since 2021/3/23 18:00
 */
public interface CommonError {
    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errMsg);
}
