package com.fxmaxlove.xzr.rxbus;

import com.jakewharton.rxrelay2.ReplayRelay;

/**
 * 所有观察者都能接收到事件（无论先发送事件还是先订阅观察者或者订阅多个观察者都能同样接收）
 * http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/subjects/ReplaySubject.html
 *
 */

public class ReplayBus extends BaseBus {

    private static volatile ReplayBus defaultBus;

    public static ReplayBus getDefault() {
        if (defaultBus == null) {
            synchronized (ReplayBus.class) {
                if (defaultBus == null) {
                    defaultBus = new ReplayBus();
                }
            }
        }
        return defaultBus;
    }

    public ReplayBus(ReplayRelay<Object> replayRelay) {
        super(replayRelay);
    }

    public ReplayBus() {
        this(ReplayRelay.create());
    }

}
