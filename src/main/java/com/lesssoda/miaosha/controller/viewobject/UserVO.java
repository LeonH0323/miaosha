package com.lesssoda.miaosha.controller.viewobject;

import lombok.Data;

/**
 * @author Lee
 * @since 2021/3/23 17:43
 */
@Data
public class UserVO {
    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;
}
