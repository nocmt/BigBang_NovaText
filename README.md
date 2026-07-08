<p align="center">
  <a href="screenshots/1.jpg"><img src="screenshots/1.jpg" alt="Screenshot 1" width="18%" /></a>
  <a href="screenshots/2.jpg"><img src="screenshots/2.jpg" alt="Screenshot 2" width="18%" /></a>
  <a href="screenshots/3.jpg"><img src="screenshots/3.jpg" alt="Screenshot 3" width="18%" /></a>
  <a href="screenshots/4.jpg"><img src="screenshots/4.jpg" alt="Screenshot 4" width="18%" /></a>
  <a href="screenshots/5.jpg"><img src="screenshots/5.jpg" alt="Screenshot 5" width="18%" /></a>
</p>

# Nova Text

Nova Text 是经典 Smartisan OS「大爆炸」功能的 Android 原生迁移与现代化项目。

当前版本：`1.2.2`

当前仓库不是概念验证阶段，已经具备完整的本地化主链路：

- 本地 `cppjieba` 分词
- Compose 设置页
- BigBang 浮层壳 + legacy 词块内核
- Compose 搜索浮层 + WebView
- 悬浮球 + 无障碍文本提取
- ML Kit OCR V2 离线识别
- OCR 白名单分流
- 统一的悬浮球启动 loop 动画和 BigBang 入场动画

## 1.2.2 相比上一基线版本的主要变化

本节以 `803bc8997fd6ff097a730b5c3658f71483ff09a5` 之前的版本为基线，记录 1.2.0 到 1.2.2 这一轮围绕悬浮球触发、识别弹窗交互、翻译和设置页整理做出的变化：

- **悬浮球触发方式可配置**：新增点击、双击、拖拽三种触发方式，默认改为点击，减少与系统全局返回手势的冲突。
- **识别弹窗下拉动作重做**：下拉不再执行原来的“上一页/下一页”语义，而是按顺序循环执行全选、连续数字、邮箱、链接、取消选择。
- **下拉动作顺序可调整**：设置页新增下拉动作顺序配置，可手动拖拽排序；拖拽时增加选中悬浮效果，并修复高亮不跟手、松手后残留等问题。
- **匹配类动作更直接**：连续数字、邮箱、链接只匹配并选择第一个结果；当前动作无匹配时会继续跳到下一项，直到找到可执行动作或一轮结束；匹配选择会走和点击选择一致的工具栏路径。
- **URL 识别增强**：链接匹配覆盖 `https://tool.oschina.net/regex/`、`http://tool.oschina.net`、`tool.oschina.net`、`tool.oschina.net/cd` 等常见形式。
- **下拉匹配选择体验修复**：下拉触发数字、邮箱、链接后会连续选中命中范围，并修复替换已有选择时工具栏不出现、选区背景缺失和文字位移不一致的问题。
- **前后文追加改为可选**：原底栏“上句/下句”实际是追加前文/后文，现改为默认隐藏，可在设置页通过“显示前后文追加”打开；按钮移动到关闭按钮左侧，并更换为追加语义图标。
- **识别弹窗设置入口可用**：右上角设置按钮不再 Toast“功能建设中”，点击后直接打开应用设置页。
- **识别弹窗编辑可用**：左上角编辑按钮会把当前正文切换为输入框，修改后点击空白或完成输入即可重新分词显示。
- **识别弹窗内翻译**：右下角翻译按钮会先显示目标语言列表，选择后通过设置页配置的 OpenAI 兼容接口翻译当前正文，并在同一个弹窗内重新分词显示。
- **翻译设置收敛**：设置页只保留接口地址、密钥、模型和提示词；默认目标语言不再作为输入项展示，默认使用英语，用户在识别弹窗语言列表中选择后自动保存替换。
- **设置页按标签整理**：主页设置按入口、识别、翻译、搜索、调试分组，主标签改为两行胶囊布局，并修复点击态阴影与圆角不一致的问题。
- **构建工具修复**：修复 Gradle wrapper 缺失 `org/gradle/wrapper/IDownload` 导致无法初始化的问题。

## Android 版本支持策略

- Android 11+：当前主维护目标，优先保证完整功能适配与稳定性
- Android 7-10：当前仅作二级支持
  - 允许继续编译和尝试运行
  - 不保证所有功能可用
  - 不保证不同机型上的稳定性与一致性

## 当前进度

已完成：

- Gradle 构建已打通，继续兼容 legacy `src/` / `res/` 目录
- `cppjieba` JNI 已替代远程分词主路径，并在启动后后台预热
- 设置页已重构为 Compose，支持深色模式、调试入口、悬浮球配置、OCR 白名单配置
- 设置页已补悬浮球锁定高度、单手优化、单手角度阈值、识别调试日志和 Android 7-10 Shizuku 状态
- BigBang 页面已接入 Compose 外层浮层壳，内部词块选择与多选逻辑仍复用 legacy Java
- 搜索页已改为 Compose + WebView 浮层页
- OCR 已切到离线 ML Kit V2，支持中文 / 日语 / 韩语 / 英语
- 设置页图片调试入口与系统图片分享入口可进入 OCR 范围选择页
- 悬浮球白名单 OCR 链路已接通：截图后直接全屏 OCR，并按触点命中最近文本块进入 BigBang
- 悬浮球无障碍链路已补静默截图缓存，供 BigBang 内手动重进 OCR 复用
- 识别弹窗下拉动作已改为可配置循环动作，支持全选、连续数字、邮箱、链接和取消选择；匹配类选择会保持和点击选择一致的工具栏与选区背景
- 前后文追加已改为可选底栏按钮，默认关闭，避免和段落切换产生歧义
- BigBang 外壳已支持重新 OCR 识别、全文编辑后重新分词、选择目标语言后翻译并重新分词
- 设置页已按入口、识别、翻译、搜索、调试拆分，翻译配置保留接口地址、密钥、模型和提示词
- 搜索页已扩展 DuckDuckGo、萌娘百科，浏览器操作栏已补前进和刷新

仍在进行：

- 横屏与平板适配尚未系统收口
- 多机型、多 Android 版本下的实机兼容性验证和 Debug 仍需持续推进
- 弹窗内翻译当前走用户配置的 OpenAI 兼容网络接口，离线翻译仍未内置
- OCR 最近段落命中、段落合并和复杂页面提取规则仍会继续打磨，但不再是“链路未打通”状态

## 快速使用

### 设置页

启动 `TextBoomSettingsActivity` 后可直接：

- 检查悬浮窗 / 无障碍状态
- 启动和停止悬浮球
- 调整悬浮球大小与透明度
- 调整悬浮球锁定高度、单手优化与角度阈值
- 切换预制调试文本并预览 BigBang
- 配置悬浮球触发方式、下拉动作顺序、前后文追加按钮显示、翻译接口、搜索源、词典源、OCR 语言和 OCR 白名单
- 开启识别调试日志
- 选择图片进入 OCR 调试

### 悬浮球主链路

1. 授予悬浮窗权限
2. 启用 `NovaTextAccessibilityService`
3. 在设置页启动悬浮球
4. 按设置中的触发方式点击、双击或拖拽悬浮球

当前分两条路径：

- 白名单外：先隐藏悬浮球并静默截图缓存，再显示 loop 动画，随后走无障碍文本提取；若无障碍抓不到文本，再复用同一张缓存图回退到 OCR
- 白名单内：先隐藏悬浮球并截图，再显示 loop 动画，随后走全屏 OCR，并按触点命中最近文本块进入 BigBang
- 悬浮球主链路的截图缓存只保存在内存里，且只保留当前活动 token，对应旧图会自动回收
- 前台应用识别优先取无障碍活跃窗口，其次回退到无障碍最近事件缓存；两者都拿不到时直接走 OCR
- Android 11+ 优先用无障碍截图；Android 7-10 走 Shizuku 截图回退
- 悬浮球拖动松手后会自动贴到屏幕左侧或右侧，横屏下也不会停在屏幕中间
- 悬浮球启动完成后由 `notifyBigBangShellShown()` 收口 loop 动画和隐藏状态；3 秒内未拉起外层 UI 会自动兜底恢复悬浮球
- 进入 BigBang 后可下拉循环执行可配置动作，匹配数字、邮箱、链接时会显示和点击选择一致的工具栏；底栏可重进 OCR、关闭弹窗、可选显示前后文追加，右下角可选择目标语言并翻译当前正文
- 左上角编辑按钮会把当前正文切换为输入框，修改后点击空白或完成输入即可重新分词显示

### OCR 调试 / 分享链路

这两条入口保留手动范围选择页：

1. 图片输入
2. 范围选择
3. 离线 OCR
4. BigBang

说明：

- OCR 结果进入 BigBang 后，左下角可重进 OCR 范围选择
- 默认 OCR 语言和翻译接口在设置页配置；进入 BigBang 后可继续使用编辑、翻译、下拉动作和搜索等统一能力
- 图片输入、分享和悬浮球缓存图复用的 OCR 源都统一走 `ManualOcrSourceStore`，不再落盘缓存

## 构建

```bash
bash ./gradlew assembleDebug
```

当前主要源码目录：

- `src/com/smartisanos/textboom/`：legacy Java BigBang 内核、词块布局、多选逻辑
- `app/src/main/kotlin/com/smartisanos/textboom/`：Compose 页面、Activity、Service、OCR、启动编排
- `app/src/main/kotlin/com/smartisanos/textboom/domain/capture/`：无障碍文本提取会话与最近段落窗口
- `app/src/main/cpp/`：`cppjieba` JNI
- `archive/legacy-ui/`：已归档的旧设置页 / 旧搜索页代码，不再主链路编译

## 文档

- [文档索引](./docs/README.md)
- [开发计划](./docs/development-plan.md)
- [架构文档](./docs/architecture.md)
- [接口与 API 文档](./docs/api.md)

## 致谢

- [cppjieba](https://github.com/yanyiwu/cppjieba)
- [BigBang](https://github.com/SmartisanTech/packages_apps_BigBang)


## License / 许可证说明

Nova Text 作为一个整体，以 **GNU General Public License v3.0（GPLv3）** 协议分发。完整协议文本见 [`LICENSE`](./LICENSE)。

本项目是基于 SmartisanTech 开源的 BigBang / BigBoom 应用继续开发的社区分支与现代化改造版本。原始项目基于 **Apache License 2.0** 发布，因此本仓库中来自原始 SmartisanTech BigBang / BigBoom 项目的代码、资源、版权声明与归属信息，仍然保留其原有的 Apache License 2.0 授权与声明。Apache License 2.0 协议文本见 [`LICENSE-Apache 2.0`](./LICENSE-Apache%202.0)。

本仓库的许可结构可以理解为：

* 本分支作为整体，包括 Nova Text 新增代码、重构代码、集成逻辑、现代化适配、构建系统调整和项目特定修改，按 **GPLv3** 分发。
* 来自原始 SmartisanTech BigBang / BigBoom 项目的部分，仍保留原始 **Apache License 2.0** 的版权声明、归属声明和许可要求。
* 基于原始项目修改过的文件，可能同时包含原始作者版权声明和本项目修改声明。
* 第三方库、依赖、字体、模型、词典、图标或其他资源，如果各自带有独立许可证，则仍遵循其各自的许可证条款。
* Apache License 2.0 与 GPLv3 在该方向上兼容：Apache-2.0 代码可以被纳入 GPLv3 项目中；但本分支中受 GPLv3 约束的新增代码和修改代码，不能在没有额外授权的情况下重新以 Apache-2.0 协议并入原始项目。

本项目是独立的社区分支，不隶属于 Smartisan / SmartisanTech，也未获得 Smartisan / SmartisanTech 的官方背书或赞助。Smartisan、BigBang、BigBoom、锤子科技、Smartisan OS 等名称可能是其各自权利人的商标或产品名称，仅用于说明项目来源与兼容背景。
