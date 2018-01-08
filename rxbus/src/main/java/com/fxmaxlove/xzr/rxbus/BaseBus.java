package com.fxmaxlove.xzr.rxbus;

import com.fxmaxlove.xzr.rxbus.util.EventThread;
import com.fxmaxlove.xzr.rxbus.util.Logger;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * bus基类
 */
public class BaseBus implements Bus {

    private static Logger sLogger;

    /**
     * 初始化配置
     * @param mainScheduler 事件接收线程
     * @param logger 日志工具
     */
    public static void config(@NonNull Scheduler mainScheduler,@Nullable Logger logger) {
        ObjectHelper.requireNonNull(mainScheduler, "mainScheduler == null ");
        EventThread.setMainThreadScheduler(mainScheduler);
        sLogger = logger;
    }

    /**
     * 设置打印日志的工具
     */
    public static void setLogger(@Nullable Logger logger) {
        sLogger = logger;
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
            LoggerUtil.debug("post event: %s", event);
            relay.accept(event);
        } else {
            LoggerUtil.warning("no observers,event will be discard:%s",event);
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

    /**
     * Util for record log
     */
    static class LoggerUtil {

        private static boolean isLoggable() {
            return sLogger != null;
        }

        static void verbose(String message, Object... args) {
            if (isLoggable()) {
                sLogger.verbose(message, args);
            }
        }

        static void debug(Object msg) {
            if (isLoggable()) {
                sLogger.debug(msg);
            }
        }

        static void debug(String message, Object... args) {
            if (isLoggable()) {
                sLogger.debug(message, args);
            }
        }

        static void info(String message, Object... args) {
            if (isLoggable()) {
                sLogger.info(message, args);
            }
        }

        static void warning(String message, Object... args) {
            if (isLoggable()) {
                sLogger.warning(message, args);
            }
        }

        static void error(String message, Object... args) {
            if (isLoggable()) {
                sLogger.error(message, args);
            }
        }

        static void error(Throwable throwable, String message, Object... args) {
            if (isLoggable()) {
                sLogger.error(throwable, message, args);
            }
        }
    }
}
