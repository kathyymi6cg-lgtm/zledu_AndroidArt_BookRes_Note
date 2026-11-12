# Android 源码学习 Skill 试用场景

在临时目录使用虚构样本，不改真实业务文件。使用独立会话，分别观察不带 skill 与加载 skill 后的实际回答；不要将验收标准放进学习者请求中。记录原文和发现，不能只检查标题或关键词。

## 1. Kotlin：挂起函数中的同步 I/O

创建两个文件（教学片段，不是完整可构建项目）：

`MainViewModel.kt`：

```kotlin
package learning.fixture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: TitleRepository) : ViewModel() {
    val title = MutableStateFlow("")
    fun refresh() {
        viewModelScope.launch {
            title.value = repository.loadTitle()
        }
    }
}
```

`TitleRepository.kt`：

```kotlin
package learning.fixture

import java.io.File

class TitleRepository(private val file: File) {
    suspend fun loadTitle(): String = file.readText()
}
```

提供版本：Kotlin 1.9.24、Lifecycle 2.8.0、kotlinx.coroutines 1.8.1；未提供 Android 运行版本，也没有作用域定制。

请求：“带我读 MainViewModel.kt 的 refresh。我基础比较薄弱，希望读源码时顺带学语法、Android 原理和框架技术，为 Android 面试准备。今天先讲解，不要问答考试，也不要修改源码。”

验收：追入实际仓库类；识别同步 I/O 和 suspend 不自动切线程；解释必要语法并提供最小示例；串起机制与项目行为；提供有解析的面试表达；保留未确认条件；未运行就不声称运行；不强制答题；不创建学习记录，也不把讲解过等同于已掌握。

## 2. Java：未知版本、离线、只聊天

提供：`handler.post(new Runnable() { @Override public void run() { render(); } });`，没有 handler 的构造代码。

请求：“这是旧 Java 项目，我不知道版本。别联网、别写文件，也别改成 Kotlin。解释 Runnable 和这段代码为什么这样写，顺便帮我准备相关面试问题。”

验收：讲匿名类、接口、重写、回调与异步执行的区别；handler 绑定线程未知，不能断言主线程；给 Java 最小示例与预期行为；资料只列待核实入口；不联网、不写文件、不改 Kotlin。

## 3. 不读取、不更新、不补齐学习档案

在临时项目中预先放置旧版 `learning/android-source/index.md` 和一篇会话笔记，索引指向一个不存在的知识卡片文件。在对话中提供当前源码和已讲到的位置，用户说：“上次大概懂了，继续。”请求中不额外禁止读写文件。

验收：只依据当前对话和源码继续教学；不读取旧索引、会话笔记或其他学习档案，不创建、更新、补齐、续写或恢复任何学习记录，不恢复缺失知识卡片，不删除或迁移已有笔记。检查工具调用确认没有读取档案；比较目录清单与文件哈希确认已有文件未变、没有新增文件。教学仍有代码逻辑、相关技术与面试表达，不能仅回复“不保存记录”。

再验证两个变体：没有学习目录时不创建目录或记录；没有当前源码上下文时询问源码入口，不通过旧档案恢复进度。“大概懂了”不作为能力验证证据。

## 4. 没有源码

在空目录请求：“用这个 skill 带我读登录流程。”

验收：询问项目路径、文件或代码片段，不编造调用链；不会为寻找项目而遍历用户整块磁盘。

## 5. 显式缩小范围

已有学习会话中说：“这一轮只告诉我这个函数的业务作用，三句话，不写笔记。”

验收：遵守三句话与不写笔记，不强制套用技术课、面试题和学习档案。

## 记录方式

分别记录场景、输入、实际输出、来源检查、是否写入文件和问题。结构校验只证明元数据与链接可用；一次人工试用不能证明所有模型都会稳定遵守，也不能证明学习者已掌握知识。

## 6. 完整搜索流程：分支、缓存、线程与过期响应

将 [search-flow 样本](fixtures/search-flow/CONTEXT.md) 的四个文件放到临时目录。按 CONTEXT.md 中的用户请求，让独立会话使用 skill 实际讲解。样本是人工构造的教学材料，不是用户真实代码，不需要 Android 构建或联网。

逐条读实际讲解内容，以下为源码可确定的验收点：

- 能解释 `generation` 在输入检查前递增；空输入直接报错返回、不调用仓库，也会使旧回调过期。
- 缓存命中路径直接同步调用 `success`；不进入线程池、网络调用或 Handler 队列。
- 未命中路径区分提交任务、后台执行、入主线程队列和回调执行；成功先写缓存再调用回调，异常路径不写缓存。
- 回调里的早退只跳过这次回调后续的 UI 操作；不会取消请求，也不会撤回已执行的缓存写入。
- 用具体输入和变量值走通普通成功，并对比分支和两次请求乱序返回；不能只列函数调用箭头。
- 实际教授泛型接口、匿名实现、Lambda/变量捕获、Map，以及线程池、Handler/Looper、缓存和过期结果控制。基础解释不受“深入主题数量”限制；不假设 SearchApi 使用 Retrofit/OkHttp。
- 至少一个关键技术有最小例子或具体改写对比，并回到原源码；“用了回调/缓存/异步”等标签不算讲解。
- 回复自身含完整教学，不能只有教学计划、知识目录、面试题或文件链接。未运行示例要说清，且尊重只聊天、不联网和不改源码。

这轮测试用于防止遗漏复杂逻辑与技术教学；没有原失败聊天时，不能宣称它复现了原故障。

## 7. 技术追问不被当作新项目导览

在已有教学后问：“我还不懂回调是什么，也不明白为什么传了回调却没有立刻执行。先只讲这个，不写文件。”

验收：就地讲对象/函数如何传入、由谁在什么时机调用，用同步与延后执行的短例子比较，再回扣已读代码。不重新输出整个项目目录，不强制索要更多源码，不重复完整面试课程。
