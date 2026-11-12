# Android 源码学习 Skill

适合希望在读项目源码时，同时学习 Java/Kotlin 语法、Android 原理、框架机制和面试表达的开发者。

当前版本：**2.1.0**。默认中文讲解，先按执行顺序读一个完整场景，再讲透理解它所需的技术。必需基础全部解释，额外底层细节按需展开；练习与模拟面试可选。

这版明确要求正文包含代码走读、具体输入推演和实质技术教程，不能只列技术名称、画调用链或写学习计划。可先看 [一份完整讲解示例](android-source-learning/references/teaching-example.md)，了解期望的讲解深度。

本版已移除学习记录功能：不读取、创建、更新、补齐、续写或恢复学习记录、进度索引和知识卡片。教学与面试表达直接在回复中完成；继续阅读依据当前对话和源码，不从学习档案恢复进度。已有学习文件保持原样，不删除或迁移。

## 安装到 Codex

完整复制 [android-source-learning](android-source-learning/) 文件夹，保留其中的 `SKILL.md`、`agents/` 和 `references/`。

- 仅当前项目使用：放到目标项目的 `.agents/skills/android-source-learning/`。
- 个人跨项目使用：放到用户目录的 `.agents/skills/android-source-learning/`，Windows 中即 `%USERPROFILE%\.agents\skills\android-source-learning\`。
- 也可以直接让 Codex 执行：“用 skill-installer 安装 https://github.com/kathyymi6cg-lgtm/zledu_AndroidArt_BookRes_Note/tree/main/skills/android-source-learning 中的 skill。”

仓库的 `skills/` 是分发目录，上传到这里不等于已安装到本机。Codex 未发现新 skill 时重新打开会话或重启应用。请避免在多个发现目录重复安装同名副本。

加载位置与调用方式参见 [Codex 官方文档](https://learn.chatgpt.com/docs/build-skills)。本 skill 不需要额外插件、API Key 或运行脚本。

## 第一次使用

在要学习的 Android 项目中打开 Codex，提供文件、函数或一个功能入口，例如：

```text
使用 $android-source-learning，先读取 SKILL.md 并告诉我版本，再带我读 app/src/main/java/example/MainViewModel.kt 的 refresh。
我基础比较薄弱，想顺带学习语法、Android 原理和框架技术，为面试准备。
按执行顺序逐块解释代码，用一组具体输入走通分支和回调，再讲用到的技术及最小例子。
先直接讲解，不要只给计划或知识点清单，不要强制问答。
```

也可以使用自然语言：“带我读这段 Android 源码，顺便把用到的技术讲明白。”若客户端支持技能选择器，可直接选择“Android 源码学习教练”。

只有代码片段时也能开始；AI 会说明缺少哪些上下文。这个书籍仓库里的代码可作为学习材料，但书籍片段不能代替某个实际项目的依赖版本和运行证据。

## 学习过程

1. 定位代码和必要依赖，交代触发场景、对象职责和本轮范围。
2. 按执行顺序展示并解释关键代码块，包括参数、分支、调用、返回、状态变化和异步边界。
3. 用明确输入与初始状态走到最终结果，再对照会改变结果的路径。
4. 结合代码教授必需语法、Android 原理和框架机制，提供最小例子或改写对比。
5. 在回复中整理面试表达，并指出下一处可阅读的源码入口。

可以随时说：“这个语法再基础一点”“继续追框架内部实现”“给我模拟面试”“这次只讲业务”。这些要求优先于默认流程。

## 更新已有安装

GitHub 上的文件更新不会自动替换你之前复制的安装目录。找到实际使用的 `android-source-learning` 文件夹，用仓库中的完整目录更新它；如有个人修改，先保留副本。不要只复制 README 或保留旧的 references/ 文件。

升级到 2.1.0 时，移除旧安装中的 `assets/knowledge-card-template.md`。这只删除 skill 自带的模板，不删除项目中以前已有的学习文件。

更新后让 AI **实际读取安装位置的 SKILL.md**，报告 `metadata.version`，本版应为 `2.1.0`。如果没有读到这个文件、报不出版本或仍显示旧版，先修正安装位置/技能选择，再开始带读。不要仅凭 AI 回答“正在使用”判断已加载。

## 文件与验证

- [SKILL.md](android-source-learning/SKILL.md)：使用边界、教学方式和证据要求。
- [Android 技术地图](android-source-learning/references/android-topics.md)：从代码线索选择学习主题。
- [完成态讲解示例](android-source-learning/references/teaching-example.md)：演示逐块代码走读、实值推演和技术解释。
- [试用场景](tests/android-source-learning.md)：维护 skill 时检查实际教学行为。

验证包括简单片段、带分支和异步回调的搜索样本，以及不读取、不更新、不补齐学习档案的边界。样本试用不能证明所有模型稳定遵守或长期学习效果，后续按实际教学反馈调整。
