package com.fxmaxlove.xzr.rxbus;

import com.fxmaxlove.xzr.rxbus.annotation.RxSubscribe;
import com.fxmaxlove.xzr.rxbus.util.EventThread;
import com.jakewharton.rxrelay2.PublishRelay;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.schedulers.Schedulers;

/**
 * 最常用，订阅后，接收事件
 * http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/subjects/PublishSubject.html
 */
public class RxBus extends BaseBus {
    private static class Holder {
        private static final RxBus BUS = new RxBus();
    }
    private static volatile RxBus defaultBus;

    private Map<Object, CompositeDisposable> subscriptions = new HashMap<>();
    private final Map<Class<?>, List<Object>> stickyEventMap;

    /**
     * 获取RxBus单例
     *
     */
    public static RxBus getInstance() {
        if (defaultBus == null) {
            synchronized (RxBus.class) {
                if (defaultBus == null) {
                    defaultBus = Holder.BUS;
                }
            }
        }
        return defaultBus;
    }

    public RxBus(PublishRelay<Object> publishRelay) {
        super(publishRelay);
        stickyEventMap = new ConcurrentHashMap<>();
    }

    /**
     * 只接收订阅后发送的事件
     * {@link PublishRelay}
     */
    public RxBus() {
        this(PublishRelay.create());
    }

    /**
     * 发粘滞事件
     *
     * @param event 粘滞事件
     */
    public void postSticky(@NonNull Object event) {
        ObjectHelper.requireNonNull(event, "event == null");
        synchronized (stickyEventMap) {
            List<Object> stickyEvents = stickyEventMap.get(event.getClass());
            boolean isStickEventListInMap = true;
            if (stickyEvents == null) {
                stickyEvents = new ArrayList<>();
                isStickEventListInMap = false;
            }
            stickyEvents.add(event);
            if (!isStickEventListInMap) {
                stickyEventMap.put(event.getClass(), stickyEvents);
            }
        }
        post(event);
    }

    /**
     * 获取粘滞事件列表
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> List<T> getSticky(Class<T> eventType) {
        synchronized (stickyEventMap) {
            List<T> list = (List<T>) stickyEventMap.get(eventType);
            return list == null ? null : Collections.unmodifiableList(list);
        }
    }

    /**
     * 移除粘滞事件
     *
     */
    public void removeSticky(@NonNull Object event) {
        ObjectHelper.requireNonNull(event, "event == null");
        synchronized (stickyEventMap) {
            List<Object> stickyEvents = stickyEventMap.get(event.getClass());
            if (stickyEvents != null) {
                stickyEvents.remove(event);
            }
        }
    }

    /**
     * 移除某个类型的所有粘滞事件
     */
    public void removeSticky(Class<?> eventType) {
        synchronized (stickyEventMap) {
            stickyEventMap.remove(eventType);
        }
    }

    /**
     * 移除所有粘滞事件
     */
    public void clearSticky() {
        synchronized (stickyEventMap) {
            stickyEventMap.clear();
        }
    }

    /**
     * 获取某一类型粘滞事件的被观察者
     */
    public <T> Observable<T> ofStickyType(Class<T> eventType) {
        synchronized (stickyEventMap) {
            @SuppressWarnings("unchecked")
            List<T> stickyEvents = (List<T>) stickyEventMap.get(eventType);
            if (stickyEvents != null && stickyEvents.size() > 0) {
                return Observable.fromIterable(stickyEvents)
                        .mergeWith(ofType(eventType));
            }
        }
        return ofType(eventType);
    }

    /**
     * 解除订阅所有的事件，清空所有粘滞事件
     */
    public void reset() {
        Observable.fromIterable(subscriptions.values())
                .filter(new Predicate<CompositeDisposable>() {
                    @Override
                    public boolean test(CompositeDisposable compositeDisposable) throws Exception {
                        return compositeDisposable != null && !compositeDisposable.isDisposed();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<CompositeDisposable>() {
                    @Override
                    public void accept(CompositeDisposable compositeDisposable) throws Exception {
                        compositeDisposable.clear();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        LoggerUtil.error(throwable, "Dispose subscription");
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        stickyEventMap.clear();
                        subscriptions.clear();
                    }
                });
    }

    /**
     * 判断是否注册
     */
    public synchronized boolean isRegistered(@NonNull Object subscriber) {
        ObjectHelper.requireNonNull(subscriber, "subscriber == null");
        return subscriptions.containsKey(subscriber.hashCode());
    }

    /**
     * 注解方式的注册方式
     */
    public void register(@NonNull final Object subscriber) {
        ObjectHelper.requireNonNull(subscriber, "subscriber == null");
        Observable.just(subscriber)
                .filter(new Predicate<Object>() {
                    @Override
                    public boolean test(Object obj) throws Exception {
                        boolean registered = isRegistered(obj);
                        if (registered) {
                            LoggerUtil.warning("%s has already registered", obj);
                        }
                        return !registered;
                    }
                })
                .flatMap(new Function<Object, ObservableSource<Method>>() {
                    @Override
                    public ObservableSource<Method> apply(Object obj) throws Exception {
                        LoggerUtil.debug("start to analyze subscriber: %s", obj);
                        return Observable.fromArray(obj.getClass().getDeclaredMethods());
                    }
                })
                .map(new Function<Method, Method>() {
                    @Override
                    public Method apply(Method method) throws Exception {
                        LoggerUtil.debug("Set method can accessible: %s ", method);
                        method.setAccessible(true);
                        return method;
                    }
                })
                .filter(new Predicate<Method>() {
                    @Override
                    public boolean test(Method method) throws Exception {
                        boolean isOK = method.isAnnotationPresent(RxSubscribe.class) && method.getParameterTypes() != null && method.getParameterTypes().length > 0;
                        LoggerUtil.debug("%s is has RxSubscribe annotation: %s", method, isOK);
                        return isOK;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<Method>() {
                    @Override
                    public void accept(Method method) throws Exception {
                        LoggerUtil.debug("now start add subscription method: %s", method);
                        addSubscriptionMethod(subscriber, method);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        LoggerUtil.error(throwable, "%s fail register", subscriber);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        LoggerUtil.debug("%s register complete", subscriber);
                    }
                });
    }

    private void addSubscriptionMethod(final Object subscriber, final Method method) {
        Disposable subscribe =
                Observable.just(method.getParameterTypes()[0])
                        .doOnNext(new Consumer<Class<?>>() {
                            @Override
                            public void accept(Class<?> type) throws Exception {
                                LoggerUtil.debug("Origin: [method: %s ] , param[0] type: %s", method, type);
                            }
                        })
                        .map(new Function<Class<?>, Class<?>>() {
                            @Override
                            public Class<?> apply(Class<?> type) throws Exception {
                                Class<?> eventType = getEventType(type);
                                LoggerUtil.debug("Listen event type: %s", eventType);
                                return eventType;
                            }
                        })
                        .flatMap(new Function<Class<?>, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(Class<?> type) throws Exception {
                                RxSubscribe rxAnnotation = method.getAnnotation(RxSubscribe.class);
                                LoggerUtil.debug("%s RxSubscribe Annotation: %s", method, rxAnnotation.observeOnThread());
                                Observable<?> observable = rxAnnotation.isSticky() ? ofStickyType(type) : ofType(type);
                                observable.observeOn(EventThread.getScheduler(rxAnnotation.observeOnThread()));
                                return observable;
                            }
                        })
                        .subscribe(
                                new Consumer<Object>() {
                                    @Override
                                    @SuppressWarnings("all")
                                    public void accept(Object obj) throws Exception {
                                        LoggerUtil.debug("Subscriber:%s invoke Method:%s", subscriber, method);
                                        try {
                                            method.invoke(subscriber, obj);
                                        } catch (IllegalAccessException e) {
                                            LoggerUtil.error(e, "%s invoke error", method);
                                        } catch (InvocationTargetException e) {
                                            LoggerUtil.error(e, "%s invoke error", method);
                                        }
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        LoggerUtil.error(throwable, "%s can't invoke %s", subscriber, method);
                                    }
                                });
        CompositeDisposable compositeDisposable = subscriptions.get(subscriber.hashCode());
        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(subscribe);
        subscriptions.put(subscriber.hashCode(), compositeDisposable);
        LoggerUtil.debug("Registered %s", method);
    }

    /**
     * 解除注册
     */
    public void unregister(@NonNull final Object subscriber) {
        ObjectHelper.requireNonNull(subscriber, "subscriber == null");
        Flowable.just(subscriber)
                .map(new Function<Object, CompositeDisposable>() {
                    @Override
                    public CompositeDisposable apply(Object subscriber) throws Exception {
                        return subscriptions.get(subscriber.hashCode());
                    }
                })
                .filter(new Predicate<CompositeDisposable>() {
                    @Override
                    public boolean test(CompositeDisposable compositeDisposable) throws Exception {
                        return compositeDisposable != null && !compositeDisposable.isDisposed();
                    }
                })
                .subscribe(new Subscriber<CompositeDisposable>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(CompositeDisposable compositeDisposable) {
                        compositeDisposable.dispose();
                        subscriptions.remove(subscriber.hashCode());
                        LoggerUtil.debug("remove subscription of %s", subscriber);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LoggerUtil.error(t, "%s unregister RxBus", subscriber);
                    }

                    @Override
                    public void onComplete() {
                        LoggerUtil.debug("%s unregister RxBus completed!", subscriber);
                    }
                });
    }

    private Class<?> getEventType(Class<?> cls) {
        String clsName = cls.getName();
        if (clsName.equals(int.class.getName())) {
            cls = Integer.class;
        } else if (clsName.equals(double.class.getName())) {
            cls = Double.class;
        } else if (clsName.equals(float.class.getName())) {
            cls = Float.class;
        } else if (clsName.equals(long.class.getName())) {
            cls = Long.class;
        } else if (clsName.equals(byte.class.getName())) {
            cls = Byte.class;
        } else if (clsName.equals(short.class.getName())) {
            cls = Short.class;
        } else if (clsName.equals(boolean.class.getName())) {
            cls = Boolean.class;
        } else if (clsName.equals(char.class.getName())) {
            cls = Character.class;
        }
        return cls;
    }

}
