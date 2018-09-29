package com.ulang.comp.router.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Weiminsir on 2018/9/27.
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface Args {

}
