package com.fxmaxlove.xzr.rxbus.annotation;


import com.fxmaxlove.xzr.rxbus.util.EventThread;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by threshold on 2017/1/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RxSubscribe {
    EventThread observeOnThread() default EventThread.MAIN;
    boolean isSticky() default false;
}
