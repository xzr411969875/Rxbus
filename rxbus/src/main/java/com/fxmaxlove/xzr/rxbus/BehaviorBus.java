package com.fxmaxlove.xzr.rxbus;

import com.jakewharton.rxrelay2.BehaviorRelay;

/**
 * 先发送事件后订阅，只能接收到最近的一个事件，后面发送的都能接收
 *  http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/subjects/BehaviorSubject.html
 */

public class BehaviorBus extends BaseBus {

    private static volatile BehaviorBus defaultBus;

    public static BehaviorBus getDefault() {
        if (defaultBus == null) {
            synchronized (BehaviorBus.class) {
                if (defaultBus == null) {
                    defaultBus = new BehaviorBus();
                }
            }
        }
        return defaultBus;
    }

    public BehaviorBus() {
        this(BehaviorRelay.create());
    }

    public BehaviorBus(Object defaultItem) {
        this(BehaviorRelay.createDefault(defaultItem));
    }

    public BehaviorBus(BehaviorRelay<Object> behaviorRelay) {
        super(behaviorRelay);
    }
}
