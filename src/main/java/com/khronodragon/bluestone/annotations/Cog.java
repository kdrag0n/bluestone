package com.khronodragon.bluestone.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cog {
    String name();
    String description() default "I'm a cog!";
    boolean hidden() default false;
}
