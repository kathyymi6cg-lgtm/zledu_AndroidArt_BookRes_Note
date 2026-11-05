# 教学样本上下文

这是专为试用建立的 Android/Java 8 代码片段集合，不是用户真实项目或完整 Android 工程。SDK 与真实依赖版本未提供。

创建端提供一个固定线程池作为 worker；工作不会在调用 execute 的主线程直接运行，多个任务可能乱序完成。SearchApi.fetch 是会阻塞到返回或抛出异常的调用，未提供具体网络实现；不能推断使用 Retrofit 或 OkHttp。

search 和 find 的所有入口均在主线程。忽略 Looper 退出、线程池拒绝任务等运行环境故障。

试用可使用以下离线 API 契约：Executor.execute 提交 Runnable，执行策略取决于实现；Handler 绑定其构造时的 Looper，post 将 Runnable 放入该 Looper 的队列，Looper 所在线程随后执行它。创建 Runnable 不会执行其 run 方法。本样本没有对网络请求的取消 API。

参考入口（本次离线，不表示在线检索完成）：
- https://developer.android.com/reference/android/os/Handler
- https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html

用户请求：带我读 SearchPresenter.search 这个搜索流程。我基础比较弱，想搞懂代码逻辑，并顺带学这里用到的 Java、Android 和框架技术，为面试准备。今天先讲解，不要考试，不要改源码，只在回复中讲，不写学习笔记。不要联网，使用所给源码和上下文。
