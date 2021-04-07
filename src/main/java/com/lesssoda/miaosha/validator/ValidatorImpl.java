package com.lesssoda.miaosha.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * @author Lee
 * @since 2021/3/25 14:50
 */
@Component
public class ValidatorImpl implements InitializingBean {

    private Validator validator;

    public ValidationResult validate(Object bean){
        final ValidationResult validationResult = new ValidationResult();
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(bean);
        if (constraintViolations.size() > 0) {
            validationResult.setHasErrors(true);
            constraintViolations.forEach(constraintViolation -> {
                String errMsg = constraintViolation.getMessage();
                String propertyname = constraintViolation.getPropertyPath().toString();
                validationResult.getErrorMsgMap().put(propertyname, errMsg);
            });
        }
        return validationResult;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 将hibernate validator通过工厂的初始化方式使其实例化
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

}
