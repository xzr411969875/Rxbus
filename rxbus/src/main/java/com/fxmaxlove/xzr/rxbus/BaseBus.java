package com.fxmaxlove.xzr.rxbus;

import com.fxmaxlove.xzr.rxbus.util.EventThread;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * bus基类
 */
public class BaseBus implements Bus {


    /**
     * 初始化配置
     * @param mainScheduler 事件接收线程
     */
    public static void config(@NonNull Scheduler mainScheduler) {
        ObjectHelper.requireNonNull(mainScheduler, "mainScheduler == null ");
        EventThread.setMainThreadScheduler(mainScheduler);
    }


    /**
     * 设置事件接收的线程
     */
    public static void setMainScheduler(@NonNull Scheduler mainScheduler) {
        EventThread.setMainThreadScheduler(mainScheduler);
    }

    private Relay<Object> relay;

    public BaseBus(Relay<Object> relay) {
        this.relay = relay.toSerialized();
    }

    @Override
    public void post(@NonNull Object event) {
        ObjectHelper.requireNonNull(event, "event == null");
        if (hasObservers()) {
            relay.accept(event);
        }
    }

    @Override @SuppressWarnings("unchecked")
    public <T> Observable<T> ofType(Class<T> eventType) {
        if (eventType.equals(Object.class)) {
            return (Observable<T>) relay;
        }
        return relay.ofType(eventType);
    }

    @Override
    public boolean hasObservers() {
        return relay.hasObservers();
    }

}
