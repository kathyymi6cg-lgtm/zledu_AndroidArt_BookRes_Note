# 第8章 理解Window和WindowManager

Window表示一个窗口的概念，在日常开发中直接接触Window的机会并不多，但是在某些特殊时候我们需要在桌面上显示一个类似悬浮窗的东西，那么这种效果就需要用到Window来实现。Window是一个抽象类，它的具体实现是PhoneWindow。创建一个Window是很简单的事，只需要通过WindowManager即可完成。WindowManager是外界访问Window的入口，Window的具体实现位于WindowManagerService中，WindowManager和WindowManagerService的交互是一个IPC过程。Android中所有的视图都是通过Window来呈现的，不管是Activity、Dialog还是Toast，它们的视图实际上都是附加在Window上的，因此Window实际是View的直接管理者。从第4章中所讲述的View的事件分发机制也可以知道，单击事件由Window传递给DecorView，然后再由DecorView传递给我们的View，就连Activity的设置视图的方法setContentView在底层也是通过Window来完成的。

## 8.1 Window和WindowManager

为了分析Window的工作机制，我们需要先了解如何使用WindowManager添加一个Window。下面的代码演示了通过WindowManager添加Window的过程，是不是很简单呢？

```
mFloatingButton = new Button(this);
mFloatingButton.setText("button") ;
mLayoutParams = new WindowManager.LayoutParams(
         LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0,
         PixelFormat.TRANSPARENT) ;
mLayoutParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
         | LayoutParams.FLAG_NOT_FOCUSABLE
         | LayoutParams. FLAG_SHOW_WHEN_LOCKED
mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
mLayoutParams.x = 100;
mLayoutParams.y = 300;
mWindowManager.addView(mFloatingButton, mLayoutParams);
```

上述代码可以将一个Button添加到屏幕坐标为（100，300）的位置上。WindowManager.LayoutParams中的flags和type这两个参数比较重要，下面对其进行说明。

Flags参数表示Window的属性，它有很多选项，通过这些选项可以控制Window的显示特性，这里主要介绍几个比较常用的选项，剩下的请查看官方文档。

FLAG_NOT_FOCUSABLE

表示Window不需要获取焦点，也不需要接收各种输入事件，此标记会同时启用FLAG_NOT_TOUCH_MODAL，最终事件会直接传递给下层的具有焦点的Window。

FLAG_NOT_TOUCH_MODAL

在此模式下，系统会将当前Window区域以外的单击事件传递给底层的Window，当前Window区域以内的单击事件则自己处理。这个标记很重要，一般来说都需要开启此标记，否则其他Window将无法收到单击事件。

FLAG_SHOW_WHEN_LOCKED

开启此模式可以让Window显示在锁屏的界面上。

Type参数表示Window的类型，Window有三种类型，分别是应用Window、子Window和系统Window。应用类Window对应着一个Activity。子Window不能单独存在，它需要附属在特定的父Window之中，比如常见的一些Dialog就是一个子Window。系统Window是需要声明权限在能创建的Window，比如Toast和系统状态栏这些都是系统Window。

Window是分层的，每个Window都有对应的z-ordered，层级大的会覆盖在层级小的Window的上面，这和HTML中的z-index的概念是完全一致的。在三类Window中，应用Window的层级范围是1~99，子Window的层级范围是1000～1999，系统Window的层级范围是2000～2999，这些层级范围对应着WindowManager.LayoutParams的type参数。如果想要Window位于所有Window的最顶层，那么采用较大的层级即可。很显然系统Window的层级是最大的，而且系统层级有很多值，一般我们可以选用TYPE_SYSTEM_OVERLAY或者TYPE_SYSTEM_ERROR，如果采用TYPE_SYSTEM_ERROR，只需要为type参数指定这个层级即可：mLayoutParams.type=LayoutParams.TYPE_SYSTEM_ERROR；同时声明权限：`<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>`。因为系统类型的Window是需要检查权限的，如果不在AndroidManifest中使用相应的权限，那么创建Window的时候就会报错，错误如下所示。

```
 E/AndroidRuntime(8071): Caused by: android.view.WindowManager$BadTokenException: Unable to add window android.view.ViewRootImpl$W@42882fe8 --
permission denied for this window type
 E/AndroidRuntime(8071) : at android.view.ViewRootImpl.setView(ViewRootImpl. java:677)
 E/AndroidRuntime(8071) : at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:326)
 E/AndroidRuntime(8071) : at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:224)
 E/AndroidRuntime(8071) : at android.view.WindowManagerImpl$CompatModeWrapper.addView(WindowManagerImpl.java:149)
 E/AndroidRuntime(8071) : at android.view.Window$LocalWindowManager.addView
 (Window. java:558)
 E/AndroidRuntime(8071): at com.ryg.chapter_8.TestActivity.onButtonClick
 (TestActivity.java:60)
 E/AndroidRuntime(8071): ... 14 more
W/ActivityManager( 514): Force finishing activity com.ryg.chapter_8/.
TestActivity
```

WindowManager所提供的功能很简单，常用的只有三个方法，即添加View、更新View和删除View，这三个方法定义在ViewManager中，而WindowManager继承了ViewManager。

```
public interface ViewManager
 {
    public void addView(View view, ViewGroup.LayoutParams params);
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) ;
    public void removeView(View view);
 }
```

对开发者来说，WindowManager常用的就只有这三个功能而已，但是这三个功能已经足够我们使用了。它可以创建一个Window并向其添加View，还可以更新Window中的View，另外如果想要删除一个Window，那么只需要删除它里面的View即可。由此来看，WindowManager操作Window的过程更像是在操作Window中的View。我们时常见到那种可以拖动的Window效果，其实是很好实现的，只需要根据手指的位置来设定LayoutParams中的x和y的值即可改变Window的位置。首先给View设置onTouchListener：mFloatingButton.setOnTouchListener（this）。然后在onTouch方法中不断更新View的位置即可：

```
 public boolean onTouch(View v, MotionEvent event) {
     int rawX = (int) event.getRawX();
     int rawY = (int) event.getRawY();
     switch(event.getAction()) {
     case MotionEvent.ACTION_MOVE: {
         mLayoutParams.x = rawX;
         mLayoutParams.y = rawY;
         mWindowManager .updateViewLayout(mFloatingButton, mLayoutParams) ;
         break;
     }
     default:
         break;
     }

     return false;
 }
```

## 8.2 Window的内部机制

Window是一个抽象的概念，每一个Window都对应着一个View和一个ViewRootImpl，Window和View通过ViewRootImpl来建立联系，因此Window并不是实际存在的，它是以View的形式存在。这点从WindowManager的定义也可以看出，它提供的三个接口方法addView、updateViewLayout以及removeView都是针对View的，这说明View才是Window存在的实体。在实际使用中无法直接访问Window，对Window的访问必须通过WindowManager。为了分析Window的内部机制，这里从Window的添加、删除以及更新说起。

### 8.2.1 Window的添加过程

Window的添加过程需要通过WindowManager的addView来实现，WindowManager是一个接口，它的真正实现是WindowManagerImpl类。在WindowManagerImpl中Window的三大操作的实现如下：

```
 @Override
 public void addView(View view, ViewGroup.LayoutParams params) {
     mGlobal.addView(view, params, mDisplay, mParentWindow) ;
 }

 @Override
public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
     mGlobal.updateViewLayout(view, params);
 }

 @Override
public void removeView(View view) {
    mGlobal.removeView(view, false);
 }
```

可以发现，WindowManagerImpl并没有直接实现Window的三大操作，而是全部交给了WindowManagerGlobal来处理，WindowManagerGlobal以工厂的形式向外提供自己的实例，在WindowManagerGlobal中有如下一段代码：private final WindowManagerGlobal mGlobal=WindowManagerGlobal.getInstance()。WindowManagerImpl这种工作模式是典型的桥接模式，将所有的操作全部委托给WindowManagerGlobal来实现。WindowManagerGlobal的addView方法主要分为如下几步。

1.检查参数是否合法，如果是子Window那么还需要调整一些布局参数

```
 if(view == null) {
     throw new IllegalArgumentException("view must not be null");
 }
 if(display == null) {
     throw new IllegalArgumentException("display must not be null");
 }
 if(! (params instanceof WindowManager.LayoutParams)) {
     throw new IllegalArgumentException("Params must be WindowManager.LayoutParams") ;
 }

 final WindowManager.LayoutParams wparams = (WindowManager.LayoutParams)
params;
 if(parentWindow != null) {
    parentWindow.adjustLayoutParamsForSubWindow(wparams) ;
 }
```

2.创建ViewRootImpl并将View添加到列表中

在WindowManagerGlobal内部有如下几个列表比较重要：

```
    private final ArrayList<View> mViews = new ArrayList<View>();
    private final ArrayList<ViewRootImpl> mRoots = new ArrayList
    <ViewRootImpl>() ;
    private final ArrayList<WindowManager.LayoutParams> mParams =
           new ArrayList<WindowManager.LayoutParams>() ;
private final ArraySet<View> mDyingViews = new ArraySet<View>();
```

在上面的声明中，mViews存储的是所有Window所对应的View，mRoots存储的是所有Window所对应的ViewRootImpl，mParams存储的是所有Window所对应的布局参数，而mDyingViews则存储了那些正在被删除的View对象，或者说是那些已经调用removeView方法但是删除操作还未完成的Window对象。在addView中通过如下方式将Window的一系列对象添加到列表中：

```
 root = new ViewRootImpl(view.getContext(), display);
 view.setLayoutParams(wparams) ;

mViews.add(view) ;
mRoots.add(root) ;
mParams.add(wparams) ;
```

3.通过ViewRootImpl来更新界面并完成Window的添加过程

这个步骤由ViewRootImpl的setView方法来完成，从第4章可以知道，View的绘制过程是由ViewRootImpl来完成的，这里当然也不例外，在setView内部会通过requestLayout来完成异步刷新请求。在下面的代码中，scheduleTraversals实际是View绘制的入口：

```
 public void requestLayout() {
     if(!mHandlingLayoutInLayoutRequest) {
         checkThread() ;
         mLayoutRequested = true;
         scheduleTraversals() ;
     }
 }
```

接着会通过WindowSession最终来完成Window的添加过程。在下面的代码中，mWindowSession的类型是IWindowSession，它是一个Binder对象，真正的实现类是Session，也就是Window的添加过程是一次IPC调用。

```
 try {
     mOrigWindowType = mWindowAttributes.type;
     mAttachInfo.mRecomputeGlobalAttributes = true;
     collectViewAttributes();
     res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
             getHostVisibility(), mDisplay.getDisplayId(),
             mAttachInfo.mContentInsets, mInputChannel);
 } catch(RemoteException e) {
    mAdded = false;
     mView = null;
    mAttachInfo.mRootView = null;
    mInputChannel = null;
    mFallbackEventHandler.setView(null);
     unscheduleTraversals() ;
     setAccessibilityFocus(null, null);
     throw new RuntimeException("Adding window failed", e);
 }
```

在Session内部会通过WindowManagerService来实现Window的添加，代码如下所示。

```
 public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams
 attrs,
         int viewVisibility, int displayId, Rect outContentInsets,
         InputChannel outInputChannel) {
     return mService.addWindow(this, window, seq, attrs, viewVisibility,
     displayId,
             outContentInsets, outInputChannel);
 }
```

如此一来，Window的添加请求就交给WindowManagerService去处理了，在WindowManagerService内部会为每一个应用保留一个单独的Session。具体Window在WindowManagerService内部是怎么添加的，本章不对其进行进一步的分析，这是因为到此为止我们对Window的添加这一流程已经清楚了，而在WindowManagerService内部主要是代码细节，深入进去没有太大的意义，读者可以自行阅读源码或者参考相关的源码分析书籍，本书对源码的分析侧重的是整体流程，会尽量避免出现深入代码逻辑无法自拔的情形。

### 8.2.2 Window的删除过程

Window的删除过程和添加过程一样，都是先通过WindowManagerImpl后，再进一步通过WindowManagerGlobal来实现的。下面是WindowManagerGlobal的removeView的实现：

```
 public void removeView(View view, boolean immediate) {
     if(view == null) {
         throw new IllegalArgumentException("view must not be null");
     }

     synchronized(mLock) {
         int index = findViewLocked(view, true);
         View curView = mRoots.get(index) .getView();
         removeViewLocked(index, immediate);
         if(curView == view) {
             return;
         }

         throw new IllegalStateException("Calling with view " + view
                 + " but the ViewAncestor is attached to " + curView);
     }
 }
```

removeView的逻辑很清晰，首先通过findViewLocked来查找待删除的View的索引，这个查找过程就是建立的数组遍历，然后再调用removeViewLocked来做进一步的删除，如下所示。

```
private void removeViewLocked(int index, boolean immediate) {
     ViewRootImpl root = mRoots.get(index);
    View view = root.getView() ;
     if(view != null) {
         InputMethodManager imm = InputMethodManager.getInstance() ;
         if(imm != null) {
             imm.windowDismissed(mViews.get(index) .getWindowToken()) ;
         }
     }
     boolean deferred = root.die(immediate);
     if(view != null) {
         view.assignParent(null) ;
         if(deferred) {
             mDyingViews.add(view) ;
         }
     }
 }
```

removeViewLocked是通过ViewRootImpl来完成删除操作的。在WindowManager中提供了两种删除接口removeView和removeViewImmediate，它们分别表示异步删除和同步删除，其中removeViewImmediate使用起来需要特别注意，一般来说不需要使用此方法来删除Window以免发生意外的错误。这里主要说异步删除的情况，具体的删除操作由ViewRootImpl的die方法来完成。在异步删除的情况下，die方法只是发送了一个请求删除的消息后就立刻返回了，这个时候View并没有完成删除操作，所以最后会将其添加到mDyingViews中，mDyingViews表示待删除的View列表。ViewRootImpl的die方法如下所示。

```
boolean die(boolean immediate) {
     // Make sure we do execute immediately if we are in the middle of a traversal or the damage
     // done by dispatchDetachedFromWindow will cause havoc on return.
     if(immediate && !mIsInTraversal) {
         doDie() ;
         return false;
     }

     if(!mIsDrawing) {
         destroyHardwareRenderer() ;
     } else {
         Log.e(TAG, "Attempting to destroy the window while drawing!\n" +
                 " window=" + this + ", title=" + mWindowAttributes.
                 getTitle());
     }
    mHandler.sendEmptyMessage(MSG_DIE) ;
     return true;
 }
```

在die方法内部只是做了简单的判断，如果是异步删除，那么就发送一个MSG_DIE的消息，ViewRootImpl中的Handler会处理此消息并调用doDie方法，如果是同步删除（立即删除），那么就不发消息直接调用doDie方法，这就是这两种删除方式的区别。在doDie内部会调用dispatchDetachedFromWindow方法，真正删除View的逻辑在dispatchDetachedFromWindow方法的内部实现。dispatchDetachedFromWindow方法主要做四件事：

（1）垃圾回收相关的工作，比如清除数据和消息、移除回调。

（2）通过Session的remove方法删除Window：mWindowSession.remove（mWindow)，这同样是一个IPC过程，最终会调用WindowManagerService的removeWindow方法。

（3）调用View的dispatchDetachedFromWindow方法，在内部会调用View的onDetachedFromWindow()以及onDetachedFromWindowInternal()。对于onDetachedFromWindow()大家一定不陌生，当View从Window中移除时，这个方法就会被调用，可以在这个方法内部做一些资源回收的工作，比如终止动画、停止线程等。

（4）调用WindowManagerGlobal的doRemoveView方法刷新数据，包括mRoots、mParams以及mDyingViews，需要将当前Window所关联的这三类对象从列表中删除。

### 8.2.3 Window的更新过程

到这里，Window的删除过程已经分析完毕了，下面分析Window的更新过程，还是要看WindowManagerGlobal的updateViewLayout方法，如下所示。

```
public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
     if(view == null) {
         throw new IllegalArgumentException("view must not be null");
     }
     if(! (params instanceof WindowManager.LayoutParams)) {
         throw new IllegalArgumentException("Params must be WindowManager.LayoutParams") ;
     }
     final WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;

     view.setLayoutParams(wparams) ;

     synchronized(mLock) {
         int index = findViewLocked(view, true);
         ViewRootImpl root = mRoots.get(index);
         mParams.remove(index) ;
         mParams.add(index, wparams) ;
         root.setLayoutParams(wparams, false);
     }
 }
```

updateViewLayout方法做的事情就比较简单了，首先它需要更新View的LayoutParams并替换掉老的LayoutParams，接着再更新ViewRootImpl中的LayoutParams，这一步是通过ViewRootImpl的setLayoutParams方法来实现的。在ViewRootImpl中会通过scheduleTraversals方法来对View重新布局，包括测量、布局、重绘这三个过程。除了View本身的重绘以外，ViewRootImpl还会通过WindowSession来更新Window的视图，这个过程最终是由WindowManagerService的relayoutWindow()来具体实现的，它同样是一个IPC过程。

## 8.3 Window的创建过程

通过上面的分析可以看出，View是Android中的视图的呈现方式，但是View不能单独存在，它必须附着在Window这个抽象的概念上面，因此有视图的地方就有Window。哪些地方有视图呢？这个读者都比较清楚，Android中可以提供视图的地方有Activity、Dialog、Toast，除此之外，还有一些依托Window而实现的视图，比如PopUpWindow、菜单，它们也是视图，有视图的地方就有Window，因此Activity、Dialog、Toast等视图都对应着一个Window。本节将分析这些视图元素中的Window的创建过程，通过本节可以使读者进一步加深对Window的理解。

### 8.3.1 Activity的Window创建过程

要分析Activity中的Window的创建过程就必须了解Activity的启动过程，详细的过程会在第9章进行介绍，这里先大概了解即可。Activity的启动过程很复杂，最终会由ActivityThread中的performLaunchActivity()来完成整个启动过程，在这个方法内部会通过类加载器创建Activity的实例对象，并调用其attach方法为其关联运行过程中所依赖的一系列上下文环境变量，代码如下所示。

```
 java.lang.ClassLoader cl = r.packageInfo.getClassLoader();
 activity = mInstrumentation.newActivity(
         cl, component.getClassName(), r.intent);
 ...

 if(activity != null) {
     Context appContext = createBaseContextForActivity(r, activity);
     CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager()) ;
    Configuration config = new Configuration(mCompatConfiguration) ;
     if(DEBUG_CONFIGURATION) Slog.v(TAG, "Launching activity "
             + r.activityInfo.name + " with config " + config);
    activity.attach(appContext, this, getInstrumentation(), r.token,
             r.ident, app, r.intent, r.activityInfo, title, r.parent,
             r.embeddedID, r.lastNonConfigurationInstances, config,
             r.voiceInteractor);
     ...

 }
```

在Activity的attach方法里，系统会创建Activity所属的Window对象并为其设置回调接口，Window对象的创建是通过PolicyManager的makeNewWindow方法实现的。由于Activity实现了Window的Callback接口，因此当Window接收到外界的状态改变时就会回调Activity的方法。Callback接口中的方法很多，但是有几个却是我们都非常熟悉的，比如onAttachedToWindow、onDetachedFromWindow、dispatchTouchEvent，等等，代码如下所示。

```
mWindow = PolicyManager.makeNewWindow(this) ;
mWindow.setCallback(this);
mWindow.setOnWindowDismissedCallback(this) ;
mWindow.getLayoutInflater() .setPrivateFactory(this);
 if(info.softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
     mWindow.setSoftInputMode(info.softInputMode) ;
 }
 if(info.uiOptions != 0) {
     mWindow.setUiOptions(info.uiOptions) ;
}
```

从上面的分析可以看出，Activity的Window是通过PolicyManager的一个工厂方法来创建的，但是从PolicyManager的类名可以看出，它不是一个普通的类，它是一个策略类。PolicyManager中实现的几个工厂方法全部在策略接口IPolicy中声明了，IPolicy的定义如下：

```
 public interface IPolicy {
    public Window makeNewWindow(Context context) ;
    public LayoutInflater makeNewLayoutInflater(Context context);
    public WindowManagerPolicy makeNewWindowManager() ;
    public FallbackEventHandler makeNewFallbackEventHandler(Context
    context) ;
 }
```

在实际的调用中，PolicyManager的真正实现是Policy类，Policy类中的makeNewWindow方法的实现如下，由此可以发现，Window的具体实现的确是PhoneWindow。

```
public Window makeNewWindow(Context context) {
     return new PhoneWindow(context);
 }
```

关于策略类PolicyManager是如何关联到Policy上面的，这个无法从源码中的调用关系来得出，这里猜测可能是由编译环节动态控制的。到这里Window已经创建完成了，下面分析Activity的视图是怎么附属在Window上的。由于Activity的视图由setContentView方法提供，我们只需要看setContentView方法的实现即可。

```
 public void setContentView(int layoutResID) {
     getWindow() .setContentView(layoutResID) ;
     initWindowDecorActionBar() ;
 }
```

从Activity的setContentView的实现可以看出，Activity将具体实现交给了Window处理，而Window的具体实现是PhoneWindow，所以只需要看PhoneWindow的相关逻辑即可。PhoneWindow的setContentView方法大致遵循如下几个步骤。

1.如果没有DecorView，那么就创建它

DecorView是一个FrameLayout，在第4章已经做了初步的介绍，这里再简单说一下。DecorView是Activity中的顶级View，一般来说它的内部包含标题栏和内部栏，但是这个会随着主题的变换而发生改变。不管怎么样，内容栏是一定要存在的，并且内容来具体固定的id，那就是“content”，它的完整id是android.R.id.content。DecorView的创建过程由installDecor方法来完成，在方法内部会通过generateDecor方法来直接创建DecorView，这个时候DecorView还只是一个空白的FrameLayout：

```
protected DecorView generateDecor() {
     return new DecorView(getContext(), -1);
 }
```

为了初始化DecorView的结构，PhoneWindow还需要通过generateLayout方法来加载具体的布局文件到DecorView中，具体的布局文件和系统版本以及主题有关，这个过程如下所示。

```
 View in = mLayoutInflater.inflate(layoutResource, null);
 decor.addView(in, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT) );
mContentRoot = (ViewGroup) in;
ViewGroup contentParent = (ViewGroup) findViewById(ID_ANDROID_CONTENT);
```

其中ID_ANDROID_CONTENT的定义如下，这个id所对应的ViewGroup就是mContentParent:

```
public static final int ID_ANDROID_CONTENT = com.android.internal.R.id.
 content
```

2.将View添加到DecorView的mContentParent中

这个过程就比较简单了，由于在步骤1中已经创建并初始化了DecorView，因此这一步直接将Activity的视图添加到DecorView的mContentParent中即可：mLayoutInflater.inflate(layoutResID，mContentParent)。到此为止，Activity的布局文件已经添加到DecorView里面了，由此可以理解Activity的setContentView这个方法的来历了。不知道读者是否曾经怀疑过：为什么不叫setView呢？它明明是给Activity设置视图的啊！从这里来看，它的确不适合叫setView，因为Activity的布局文件只是被添加到DecorView的mContentParent中，因此叫setContentView更加准确。

3.回调Activity的onContentChanged方法通知Activity视图已经发生改变

这个过程就更简单了，由于Activity实现了Window的Callback接口，这里表示Activity的布局文件已经被添加到DecorView的mContentParent中了，于是需要通知Activity，使其可以做相应的处理。Activity的onContentChanged方法是个空实现，我们可以在子Activity中处理这个回调。这个过程的代码如下所示。

```
 final Callback cb = getCallback();
 if(cb != null && !isDestroyed()) {
     cb.onContentChanged() ;
 }
```

经过了上面的三个步骤，到这里为止DecorView已经被创建并初始化完毕，Activity的布局文件也已经成功添加到了DecorView的mContentParent中，但是这个时候DecorView还没有被WindowManager正式添加到Window中。这里需要正确理解Window的概念，Window更多表示的是一种抽象的功能集合，虽然说早在Activity的attach方法中Window就已经被创建了，但是这个时候由于DecorView并没有被WindowManager识别，所以这个时候的Window无法提供具体功能，因为它还无法接收外界的输入信息。在ActivityThread的handleResumeActivity方法中，首先会调用Activity的onResume方法，接着会调用Activity的makeVisible()，正是在makeVisible方法中，DecorView真正地完成了添加和显示这两个过程，到这里Activity的视图才能被用户看到，如下所示。

```
 void makeVisible() {
     if(!mWindowAdded) {
         ViewManager wm = getWindowManager();
         wm.addView(mDecor, getWindow() .getAttributes());
         mWindowAdded = true;
     }
     mDecor.setVisibility(View.VISIBLE) ;
 }
```

到这里，Activity中的Window的创建过程已经分析完了，读者对整个过程是不是有了更进一步的理解了呢？

### 8.3.2 Dialog的Window创建过程

Dialog的Window的创建过程和Activity类似，有如下几个步骤。

1.创建Window

Dialog中Window的创建同样是通过PolicyManager的makeNewWindow方法来完成的，从8.3.1节中可以知道，创建后的对象实际上就是PhoneWindow，这个过程和Activity的Window的创建过程是一致的，这里就不再详细说明了。

```
 Dialog(Context context, int theme, boolean createContextThemeWrapper) {
     ...

     mWindowManager = (WindowManager)context.getSystemService(Context.
     WINDOW_SERVICE) ;
    Window w = PolicyManager.makeNewWindow(mContext) ;
    mWindow = w;
     w.setCallback(this) ;
     w.setOnWindowDismissedCallback(this);
     w.setWindowManager(mWindowManager, null, null);
    w.setGravity(Gravity.CENTER) ;
    mListenersHandler = new ListenersHandler(this);
 }
```

2.初始化DecorView并将Dialog的视图添加到DecorView中

这个过程也和Activity的类似，都是通过Window去添加指定的布局文件。

```
public void setContentView(int layoutResID) {
    mWindow.setContentView(layoutResID);
 }
```

3.将DecorView添加到Window中并显示

在Dialog的show方法中，会通过WindowManager将DecorView添加到Window中，如下所示。

```
mWindowManager.addView(mDecor, l);
mShowing = true;
```

从上面三个步骤可以发现，Dialog的Window创建和Activity的Window创建过程很类似，二者几乎没有什么区别。当Dialog被关闭时，它会通过WindowManager来移除DecorView:mWindowManager.removeViewImmediate(mDecor)

普通的Dialog有一个特殊之处，那就是必须采用Activity的Context，如果采用Application的Context，那么就会报错。

```
 Dialog dialog = new Dialog(this.getApplicationContext());
 TextView textView = new TextView(this);
 textView.setText("this is toast!");
 dialog.setContentView(textView) ;
 dialog.show();
```

上述代码运行时会报错，错误信息如下所示。

```
 E/AndroidRuntime(1185) : Caused by: android.view.WindowManager$BadTokenException: Unable to add window -- token null is not for an application
 E/AndroidRuntime(1185): at android.view.ViewRootImpl.setView(ViewRootImpl.java:657)
 E/AndroidRuntime(1185) : at android.view.WindowManagerImpl .addView(WindowManagerImpl.java:326)
E/AndroidRuntime(1185) : at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:224)
E/AndroidRuntime(1185) : at android.view.WindowManagerImpl$CompatModeWrapper .addView(WindowManagerImpl.java: 149)
 E/AndroidRuntime(1185) : at android.app.Dialog.show(Dialog.java:316)
 E/AndroidRuntime(1185) : at com.ryg.chapter_8.DemoActivity_1.initView
 (DemoActivity_1.java:26)
E/AndroidRuntime(1185) : at com.ryg.chapter_8.DemoActivity_1.onCreate
 (DemoActivity_1.java:18)
E/AndroidRuntime(1185) : at android.app.Activity.performCreate(Activity.
 java:5086)
E/AndroidRuntime(1185) : at android.app.Instrumentation.callActivityOnCreate(Instrumentation. java:1079)
E/AndroidRuntime(1185) : at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2056)
```

上面的错误信息很明确，是没有应用token所导致的，而应用token一般只有Activity拥有，所以这里只需要用Activity作为Context来显示对话框即可。另外，系统Window比较特殊，它可以不需要token，因此在上面的例子中，只需要指定对话框的Window为系统类型就可以正常弹出对话框。在本章一开始讲到，WindowManager.LayoutParams中的type表示Window的类型，而系统Window的层级范围是2000～2999，这些层级范围就对应着type参数。系统Window的层级有很多值，对于本例来说，可以选用TYPE_SYSTEM_OVERLAY来指定对话框的Window类型为系统Window，如下所示。

```
 dialog.getWindow() .setType(LayoutParams.TYPE_SYSTEM_ERROR)
```

然后别忘了在AndroidManifest文件中声明权限从而可以使用系统Window，如下所示。

```
 <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### 8.3.3 Toast的Window创建过程

Toast和Dialog不同，它的工作过程就稍显复杂。首先Toast也是基于Window来实现的，但是由于Toast具有定时取消这一功能，所以系统采用了Handler。在Toast的内部有两类IPC过程，第一类是Toast访问NotificationManagerService，第二类是NotificationManagerService回调Toast里的TN接口。关于IPC的一些知识，请读者参考第2章的相关内容。为了便于描述，下面将NotificationManagerService简称为NMS。

Toast属于系统Window，它内部的视图由两种方式指定，一种是系统默认的样式，另一种是通过setView方法来指定一个自定义View，不管如何，它们都对应Toast的一个View类型的内部成员mNextView。Toast提供了show和cancel分别用于显示和隐藏Toast，它们的内部是一个IPC过程，show方法和cancel方法的实现如下：

```
public void show() {
     if(mNextView == null) {
         throw new RuntimeException("setView must have been called");
     }

     INotificationManager service = getService();
     String pkg = mContext.getOpPackageName() ;
     TN tn = mTN;
     tn.mNextView = mNextView;

     try {
         service.enqueueToast(pkg, tn, mDuration);
     } catch(RemoteException e) {
         // Empty
     }
 }

 public void cancel() {
    mTN.hide() ;

     try {
         getService() .cancelToast(mContext.getPackageName(), mTN);
     } catch(RemoteException e) {
         // Empty
     }
 }
```

从上面的代码可以看到，显示和隐藏Toast都需要通过NMS来实现，由于NMS运行在系统的进程中，所以只能通过远程调用的方式来显示和隐藏Toast。需要注意的是TN这个类，它是一个Binder类，在Toast和NMS进行IPC的过程中，当NMS处理Toast的显示或隐藏请求时会跨进程回调TN中的方法，这个时候由于TN运行在Binder线程池中，所以需要通过Handler将其切换到当前线程中。这里的当前线程是指发送Toast请求所在的线程。注意，由于这里使用了Handler，所以这意味着Toast无法在没有Looper的线程中弹出，这是因为Handler需要使用Looper才能完成切换线程的功能，关于Handler和Looper的具体介绍请参看第10章。

首先看Toast的显示过程，它调用了NMS中的enqueueToast方法，如下所示。

```
 INotificationManager service = getService();
 String pkg = mContext.getOpPackageName() ;
 TN tn = mTN;
 tn.mNextView = mNextView;
 try {
     service.enqueueToast(pkg, tn, mDuration);
 } catch(RemoteException e) {
     // Empty
 }
```

NMS的enqueueToast方法的第一个参数表示当前应用的包名，第二个参数tn表示远程回调，第三个参数表示Toast的时长。enqueueToast首先将Toast请求封装为ToastRecord对象并将其添加到一个名为mToastQueue的队列中。mToastQueue其实是一个ArrayList。对于非系统应用来说，mToastQueue中最多能同时存在50个ToastRecord，这样做是为了防止DOS（Denial of Service）。如果不这么做，试想一下，如果我们通过大量的循环去连续弹出Toast，这将会导致其他应用没有机会弹出Toast，那么对于其他应用的Toast请求，系统的行为就是拒绝服务，这就是拒绝服务攻击的含义，这种手段常用于网络攻击中。

```
 // Limit the number of toasts that any given package except the android
 // package can enqueue. Prevents DOS attacks and deals with leaks.
 if(!isSystemToast) {
     int count = 0;
     final int N = mToastQueue.size();
     for (int i=0; i<N; i++) {
          final ToastRecord r = mToastQueue.get(i);
          if(r.pkg.equals(pkg)) {
              count++;
              if(count >= MAX_PACKAGE_NOTIFICATIONS) {
                  Slog.e(TAG, "Package has already posted " + count
                         + " toasts. Not showing more. Package=" + pkg);
                  return;
              }
          }
     }
 }
```

正常情况下，一个应用不可能达到上限，当ToastRecord被添加到mToastQueue中后，NMS就会通过showNextToastLocked方法来显示当前的Toast。下面的代码很好理解，需要注意的是，Toast的显示是由ToastRecord的callback来完成的，这个callback实际上就是Toast中的TN对象的远程Binder，通过callback来访问TN中的方法是需要跨进程来完成的，最终被调用的TN中的方法会运行在发起Toast请求的应用的Binder线程池中。

```
 void showNextToastLocked() {
     ToastRecord record = mToastQueue.get(0);
     while(record != null) {
         if(DBG) Slog.d(TAG, "Show pkg=" + record.pkg + " callback=" + record.
         callback) ;
         try {
             record.callback.show() ;
             scheduleTimeoutLocked(record) ;
             return;
         } catch(RemoteException e) {
             Slog.w(TAG, "Object died trying to show notification " + record.
             callback
                     + " in package " + record.pkg);
             // remove it from the list and let the process die
             int index = mToastQueue.indexOf(record);
             if(index >= 0) {
                 mToastQueue. remove(index) ;
             }
             keepProcessAliveLocked(record.pid) ;
             if(mToastQueue.size() > 0) {
                 record = mToastQueue.get(0);
             } else {
                 record = null;
            }
        }
    }
}
```

Toast显示以后，NMS还会通过scheduleTimeoutLocked方法来发送一个延时消息，具体的延时取决于Toast的时长，如下所示。

```
 private void scheduleTimeoutLocked(ToastRecord r)
 {
     mHandler.removeCallbacksAndMessages(r) ;
     Message m = Message.obtain(mHandler, MESSAGE_TIMEOUT, r);
     long delay = r.duration == Toast .LENGTH_LONG ? LONG_DELAY : SHORT_DELAY;
     mHandler.sendMessageDelayed(m, delay);
 }
```

在上面的代码中，LONG_DELAY是3.5s，而SHORT_DELAY是2s。延迟相应的时间后，NMS会通过cancelToastLocked方法来隐藏Toast并将其从mToastQueue中移除，这个时候如果mToastQueue中还有其他Toast，那么NMS就继续显示其他Toast。

Toast的隐藏也是通过ToastRecord的callback来完成的，这同样也是一次IPC过程，它的工作方式和Toast的显示过程是类似的，如下所示。

```
 try {
     record.callback.hide() ;
 } catch(RemoteException e) {
     Slog.w(TAG, "Object died trying to hide notification " + record.callback
             + " in package " + record.pkg);
     // don't worry about this, we're about to remove it from
     // the list anyway
 }
```

通过上面的分析，大家知道Toast的显示和影响过程实际上是通过Toast中的TN这个类来实现的，它有两个方法show和hide，分别对应Toast的显示和隐藏。由于这两个方法是被NMS以跨进程的方式调用的，因此它们运行在Binder线程池中。为了将执行环境切换到Toast请求所在的线程，在它们的内部使用了Handler，如下所示。

```
/**
 * schedule handleShow into the right thread
 */
 @Override
public void show() {
     if(localLOGV) Log.v(TAG, "SHOW: " + this);
    mHandler.post(mShow) ;
 }

 /**
  * schedule handleHide into the right thread
  */
 @Override
public void hide() {
     if(localLOGV) Log.v(TAG, "HIDE: " + this);
    mHandler.post(mHide) ;
 }
```

上述代码中，mShow和mHide是两个Runnable，它们内部分别调用了handleShow和handleHide方法。由此可见，handleShow和handleHide才是真正完成显示和隐藏Toast的地方。TN的handleShow中会将Toast的视图添加到Window中，如下所示。

```
mWM = (WindowManager)context.getSystemService(Context .WINDOW_SERVICE) ;
mWM.addView(mView, mParams)
```

而NT的handleHide中会将Toast的视图从Window中移除，如下所示。

```
 if(mView.getParent() != null) {
     if(localLOGV) Log.v(TAG, "REMOVE! " + mView + " in " + this);
     mWM. removeView(mView) ;
 }
```

到这里Toast的Window的创建过程已经分析完了，相信读者对Toast的工作过程有了一个更加全面的理解了。除了上面已经提到的Activity、Dialog和Toast以外，PopupWindow、菜单以及状态栏等都是通过Window来实现的，这里就不一一介绍了，读者可以找自己感兴趣的内容来分析。

本章的意义在于让读者对Window有一个更加清晰的认识，同时能够深刻理解Window和View的依赖关系，这有助于理解其他更深层次的概念，比如SurfaceFlinger。通过本章读者应该知道，任何View都是附属在一个Window上面的，那么这里问一个问题：一个应用中到底有多少个Window呢？相信读者都已经清楚了。
