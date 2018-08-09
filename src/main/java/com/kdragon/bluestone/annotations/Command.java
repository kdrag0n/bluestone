package com.kdragon.bluestone.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    String name();
    String desc() default "I'm a command with no info!";
    String usage() default "";
    boolean hidden() default false;
    boolean guildOnly() default false;
    String[] aliases() default {};
    boolean thread() default false;
    boolean reportErrors() default true;
}
