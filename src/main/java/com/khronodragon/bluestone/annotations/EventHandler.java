package com.khronodragon.bluestone.annotations;

import net.dv8tion.jda.core.events.Event;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    Class<? extends Event> event();
    boolean threaded() default false;
}
