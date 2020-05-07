# Rxbus
基于RxJava2和RxRelay的事件总线

1.支持注解方式订阅事件。 2.支持粘滞事件。 3.支持3种Bus
# Rxbus使⽤ 
Application中初始化BaseBus.config(AndroidSchedulers.mainThread());
## 发送事件:
普通事件:RxBus.getDefault().post(event); 发粘滞事件:RxBus.getDefault().postSticky(event)
⼿手动订阅:
在类⾥里里创建⼀一个对象:
```
CompositeDisposable mCompositeDisposable = new CompositeDisposable();
```


普通事件举例例:
```
Disposable subscribe = RxBus.getDefault().toObservable(String.class).subscribe(new Consumer<String>() {
@Override
public void accept(String s) throws Exception { }
}); 
mCompositeDisposable.add(subscribe);
```
粘滞事件:
```
Disposable subscribe = RxBus.getDefault().toStickyObservable(String.class).subscribe(new Consumer<String>() {
@Override
public void accept(String s) throws Exception { }
});
mCompositeDisposable.add(subscribe);
```

类⽣生命周期结束时解除订阅
```
@Override
protected void onDestroy() {
super.onDestroy();
if (mCompositeDisposable != null) {
mCompositeDisposable.clear(); }
}
```

## 注解⽅方式订阅:
普通事件:
@RxSubscribe(observeOnThread = EventThread.MAIN) public void listenRxIntegerEvent(int code) {
}
粘滞事件:
@RxSubscribe(observeOnThread = EventThread.IO,isSticky = true) public void listenRxStringEvent(String event) {
}
## 注册和取消注册订阅事件:
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
super.onCreate(savedInstanceState); setContentView(R.layout.activity_rx_bus); RxBus.getDefault().register(this);
} @Override
protected void onDestroy() { super.onDestroy();
RxBus.getDefault().unregister(this); }
## 混淆:
 -keep class com.fxmaxlove.xzr.** { *; }
 -keepclasseswithmembers class * {
 @com.fxmaxlove.xzr.rxbus.annotation.RxSubscr
 ibe <methods>;
 
