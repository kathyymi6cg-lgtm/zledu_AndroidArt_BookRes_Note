# 从 Android 源码选择技术主题

这是选题地图，不是固定课程、面试频率排行榜或项目事实。只读取与当前代码相关的条目；先识别依赖版本，再选择深入的符号和资料。用户指定求职级别或岗位方向时据此调整，未指定时先满足当前代码理解。

| 代码线索 | 必要基础 | 可选深入点 | 面试追问方向 |
| --- | --- | --- | --- |
| Java 接口、匿名类、回调 | 对象、引用、接口、重写、方法调用 | 回调由谁保存和调用、闭包捕获、对象生命周期 | 传入回调等于立刻执行吗？接口与抽象类怎么选？ |
| Java 集合、泛型、异常 | 类型参数、继承关系、异常传播 | 类型擦除、集合结构、equals/hashCode、资源关闭 | 泛型解决什么问题？不同集合为何适合不同操作？ |
| Kotlin `?.`、`?:`、Lambda、扩展、`by` | 空值、函数值、接收者、属性访问 | 空安全边界、扩展解析、属性委托、Java 互操作 | 扩展函数改变原类了吗？`val` 等于对象不可变吗？ |
| Activity、Fragment、导航 | 对象与组件、状态、回调 | 生命周期、配置变化、返回栈、状态恢复 | 页面离开、旋转、进程终止分别发生什么？ |
| `Handler`、`Looper`、`post` | 线程、队列、Runnable、对象引用 | 消息投递与分发、线程绑定、延迟任务和引用持有 | Handler 的执行线程由什么决定？延迟任务如何影响生命周期？ |
| Service、AIDL、Binder、系统服务 | 进程与线程、接口、序列化 | 代理与服务端调用、IPC、线程模型、死亡通知 | 远程调用与本地调用有何区别？调用是否跨进程？ |
| View、ViewGroup、触摸回调 | 继承重写、坐标、树结构 | measure/layout/draw、事件分发、布局刷新 | 测量值和最终布局有什么区别？事件由谁消费？ |
| RecyclerView、Adapter、图片加载 | 数据与视图、复用、引用 | 绑定与回收、差量更新、图片缓存、列表性能 | 为什么复用后会错位？如何定位卡顿来源？ |
| ViewModel、LiveData、StateFlow | 状态、订阅、对象持有关系 | 作用域、状态暴露、生命周期感知收集、状态恢复 | 配置变化与进程终止为何不同？状态和事件如何表达？ |
| `suspend`、`launch`、Flow、RxJava | 函数值、回调、线程、异常 | 调度、挂起/阻塞、取消、结构化并发、冷热流 | 当前调用是否切线程？取消是否能停止底层工作？ |
| Retrofit、OkHttp、网络封装 | 接口、注解、泛型、HTTP | 接口到调用的生成、拦截器、请求调度、连接复用、超时 | 请求在哪一层执行？失败、超时、取消有什么区别？ |
| Room、SQLite、缓存 | 表与关系、对象映射、I/O | 事务、索引、迁移、一致性、缓存失效 | 何时命中缓存？更新如何保持一致？ |
| Hilt、Dagger、Repository、模块依赖 | 构造函数、接口、依赖关系 | 依赖注入、作用域、分层、测试替换 | 谁创建对象？为什么使用这个边界？ |
| Compose 状态与组合函数 | Lambda、状态、声明式 UI | 重组、状态提升、副作用、状态保存 | 状态变化为什么触发 UI 更新？副作用应放哪里？ |
| 耗时、内存、启动、ANR | 线程、引用、分配、进程生命周期 | 主线程工作、泄漏路径、性能测量、启动链路 | 有什么证据表明瓶颈在这里？如何验证优化有效？ |
| Gradle、模块与构建插件 | 源码/产物、依赖、编译 | 构建变体、依赖解析、代码生成、版本兼容 | 声明依赖和最终解析版本为什么可能不同？ |

## 选题方法

对候选主题判断三件事：它是否阻碍当前理解、能否迁移到其他代码、能否形成有源码依据的解释。优先当前障碍和高迁移性主题，避免仅因名词“高级”就深挖。

例如看到 `viewModelScope.launch`，可先学 Lambda 与挂起/线程关系；StateFlow、生命周期和异常传播先说明当前运行必需的部分，其余分到后续轮次。面试准备要求更完整时，把本项目未覆盖的相关主题单独列为“项目外待补”，不假装项目包含它们。

## 官方资料入口

这些是检索入口，使用时实际打开相应章节，并核对项目版本。入口本身不能替代论据。

- [Java 语言规范](https://docs.oracle.com/javase/specs/)：选择项目对应版本，查看接口、泛型、异常等章节。
- [Kotlin 文档](https://kotlinlang.org/docs/home.html)：语法与语言概念。
- [Kotlin 属性委托](https://kotlinlang.org/docs/delegated-properties.html)：`by` 与属性访问。
- [Kotlin 协程基础](https://kotlinlang.org/docs/coroutines-basics.html)：协程、作用域、调度器。
- [Android 进程与线程](https://developer.android.com/guide/components/processes-and-threads)：组件、主线程、进程边界。
- [Android 协程与线程调度](https://developer.android.com/kotlin/coroutines/coroutines-adv)：主线程安全与调度。
- [ViewModel 概述](https://developer.android.com/topic/libraries/architecture/viewmodel)与[状态保存](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)：生命周期与恢复边界。
- [AOSP Binder 概述](https://source.android.com/docs/core/architecture/ipc/binder-overview)：跨进程调用。
- [Android 源码检索](https://cs.android.com/)：查实现时定位到相应分支或提交。
- [Retrofit](https://square.github.io/retrofit/)与[OkHttp](https://square.github.io/okhttp/)：官方文档；内部实现继续查看对应发布标签的源码。

Framework、AndroidX 和第三方库的版本分别确认。旧书中的 API 或机制可以讲历史上下文，当前行为应另行验证；源码中的实现细节也不必然是跨版本不变的公开契约。
