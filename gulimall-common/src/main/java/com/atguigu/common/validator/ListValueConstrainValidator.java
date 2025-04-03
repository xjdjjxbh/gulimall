package com.atguigu.common.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;


/**
 * 自定义校验器，第一个泛型是表示校验哪个注解，第二个形参是表示校验什么数据类型
 */
public class ListValueConstrainValidator implements ConstraintValidator<ListValue,Integer> {

    HashSet<Integer> set = new HashSet<Integer>();

    /**初始化方法
     * @param constraintAnnotation   可以从这个形参中获取所有被允许的值
     */
    @Override
    public void initialize(ListValue constraintAnnotation) {

        int[] values = constraintAnnotation.values();
        for (int value : values) {
            set.add(value);
        }
    }

    /**
     *
     * @param value   需要校验的值
     * @param constraintValidatorContext
     * @return
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
        //判断初始化set中是否包含value，如果包含，则代表用户传入的值是被允许的
        return set.contains(value);
    }
}
