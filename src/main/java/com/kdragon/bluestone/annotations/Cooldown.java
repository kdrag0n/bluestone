package com.kdragon.bluestone.annotations;

import com.kdragon.bluestone.enums.BucketType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cooldown {
    BucketType scope() default BucketType.CHANNEL;
    int invocations() default 1;
    float delay() default 10.0f;
}

