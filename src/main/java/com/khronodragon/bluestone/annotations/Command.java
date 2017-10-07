package com.khronodragon.bluestone.annotations;

import net.dv8tion.jda.core.Permission;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    String name();
    String desc() default "I'm a command with no info!";
    String usage() default "";
    boolean hidden() default false;
    Permission[] perms() default {};
    boolean guildOnly() default false;
    String[] aliases() default {};
    boolean thread() default false;
    boolean reportErrors() default true;
}
