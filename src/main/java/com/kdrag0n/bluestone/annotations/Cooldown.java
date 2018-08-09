package com.kdrag0n.bluestone.annotations;

import com.kdrag0n.bluestone.enums.BucketType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cooldown {
    BucketType scope() default BucketType.CHANNEL;
    int invocations() default 1;
    float delay() default 10.0f;
}

