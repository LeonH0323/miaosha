package com.lesssoda.miaosha.response;

import lombok.Data;

/**
 * @author Lee
 * @since 2021/3/23 17:50
 */
@Data
public class CommonReturnType {
    private String status;
    private Object data;

    public static CommonReturnType create(Object result){
        return CommonReturnType.create(result, "success");
    }
    public static CommonReturnType create(Object result, String status){
        CommonReturnType type = new CommonReturnType();
        type.setStatus(status);
        type.setData(result);
        return type;
    }
}
