package com.lesssoda.miaosha.validator;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * @author Lee
 * @since 2021/3/25 14:40
 */
@Data
public class ValidationResult {
    // 校验结果是否有错
    private boolean hasErrors = false;
    // 存放错误信息的map
    private HashMap<String, String> errorMsgMap = new HashMap<>();

    public boolean isHasErrors(){
        return hasErrors;
    }

    // 实现通过的格式化字符串信息获取错误结果的msg方法
    public String getErrMsg(){
        return StringUtils.join(errorMsgMap.values(),",");
    }
}
