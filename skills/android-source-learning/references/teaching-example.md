# 完成态讲解示例：回调不一定异步

本页用一个独立 Java 程序展示“原代码 → 执行过程 → 具体值 → 技术机制”的讲解密度。它不使用 Android Handler，也不创建线程，不能用它代替真实项目的线程分析。

## 原代码

```java
import java.util.function.Consumer;

public class CallbackLesson {
    private static String cached = "Alice";

    static void find(Consumer<String> callback) {
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        callback.accept("Bob");
    }

    public static void main(String[] args) {
        System.out.println("开始");
        find(value -> System.out.println("结果：" + value));
        System.out.println("find 已返回");
    }
}
```

保存为 `CallbackLesson.java`，需要 Java 8 或更新版本。下面描述预期输出；在具体教学会话中，是否执行过应按当次实际情况说明。

## 先讲它在干什么

调用者请求一个名字，`find` 把结果交给调用者传入的处理函数。示例的缓存变量初值是 `Alice`；没有缓存时交付演示值 `Bob`。没有文件、网络请求或后台线程。

## 按执行顺序解释

1. 程序进入 `main`，先打印“开始”。
2. `value -> System.out.println(...)` 提供一个接收字符串的操作。这个 Lambda 交给 `find`，此时还没有打印“结果”。
3. 进入 `find`，参数 `callback` 持有调用者传入的操作。`cached != null` 成立，因为当前值是 `Alice`。
4. `callback.accept(cached)` 把 `Alice` 作为参数交给那个操作，执行 Lambda。此时 Lambda 的 `value` 是 `Alice`，打印“结果：Alice”。注意：这是在 `find` 内部直接调用，`find` 还没有返回。
5. `return` 结束本次 `find`，所以不会再执行后面的 `callback.accept("Bob")`。它不会结束整个程序；控制回到 `main` 的调用点之后。
6. `main` 继续打印“find 已返回”。

因此，预期输出为：

```text
开始
结果：Alice
find 已返回
```

如果把初始值改成 `null`，第 3 步条件不成立，跳过 if 块，执行传入 `Bob` 的操作；输出第二行变成“结果：Bob”。两条路径都在原调用线程同步执行。调用栈里的 `return` 和“什么时候交付结果”是两件需要分别跟踪的事。

## 这里的技术是什么

**回调**是把后续处理交给另一段代码，让它在合适时机调用。本例中，`find` 负责选择结果，调用者负责决定拿到结果后做什么。能更换处理动作是这种写法的作用；是否值得使用，要看调用者是否需要这种灵活性。

`Consumer<T>` 是 Java 标准库的函数式接口，其唯一抽象方法是 `accept(T value)`，接收一个值、不返回结果。这里的 `T` 用 `String` 替换，因此 `Consumer<String>` 接收字符串。它不是 Android 框架接口，也不是本项目自己声明的类型。参见 [Java 8 Consumer API](https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html)。

Lambda 的参数名 `value` 可以换成其他合法名字。它通过类型和参数位置接收 `accept` 传来的值，与 `cached` 名称是否相同无关。将这段 Lambda 展开后，核心关系更明显：

```java
find(new Consumer<String>() {
    @Override
    public void accept(String value) {
        System.out.println("结果：" + value);
    }
});
```

这是在同一上下文中对调用表达式的改写：创建一个实现接口的匿名类对象。`@Override` 帮助编译器检查实现的方法签名，不负责执行或调度代码。对本例没有使用 `this` 的方法体，两种写法都完成同一个打印动作；不要据此宣称匿名类与 Lambda 的所有语言语义都相同。

这里 `find` 的返回类型是 `void`，数据通过 `accept` 的参数交给调用者提供的操作。`void` 不意味着没有结果数据，回调也不意味着异步。判断它是否延后执行，需要查调用位置：本例直接执行 `accept`，没有把任务保存到队列或提交到执行器。

如果需求只是同步取得一个字符串，`String find()` 再由调用者打印会更直接。如果把处理交给一个未来才执行的任务，调用顺序才可能改变；必须看真实的任务队列或框架契约，不能靠“用了 Lambda”判断。

## 回到项目和面试表达

读真实项目时，找出三处：处理动作在哪创建、交给谁、哪里真正调用。缓存分支可能同步交付，网络分支可能在另一个时刻交付；同一接口的不同分支需要分别追踪。

可以这样回答：“回调描述的是控制权如何交给被调用方，不保证异步。本例把 Consumer 传给 find，find 选择名字后直接调用 accept，因此结果处理发生在 find 返回之前。换到 Android 项目，还要继续看任务如何调度、由哪个线程调用回调。”

追问“为什么不是 find 返回字符串？”时，先解释数据通过回调参数交付，再讨论这种接口能否适应延后结果；同时指出本例完全同步，直接返回值也是可行设计。

## 与泛泛总结的区别

“这里用回调解耦，Lambda 简化代码”只给出了标签。完成态讲解应让读者能回答：Alice 从哪里来，何时变成 value，谁调用 accept，为什么输出顺序如此，return 跳过哪段代码，以及换成无缓存输入会发生什么。
