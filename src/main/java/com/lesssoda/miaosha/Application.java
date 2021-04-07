package com.lesssoda.miaosha;

import com.lesssoda.miaosha.dao.UserDOMapper;
import com.lesssoda.miaosha.dataobject.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author Lee
 * @since 2021/3/22 22:29
 */
@SpringBootApplication(scanBasePackages = {"com.lesssoda.miaosha"})
@MapperScan("com.lesssoda.miaosha.dao")
public class Application {

    @Autowired
    private UserDOMapper userDOMapper;
//
//    @RequestMapping("/")
//    public String hello(){
//        UserDO userDO = userDOMapper.selectByPrimaryKey(1);
//        if (userDO == null){
//            return "用户对象不存在";
//        }else{
//            return userDO.getName();
//        }
//    }
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
