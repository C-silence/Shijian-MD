# 拾简 · Shijian

面向**手机与平板**的 Markdown 阅读 / 编辑应用。原生 Kotlin + Jetpack Compose 实现，**不用 WebView**；视觉语言参考 Typora 的 phycat 主题族后重新设计，针对触屏操作与多尺寸屏幕重新排版。

> **Shijian** — a native Kotlin + Jetpack Compose Markdown reader / editor for Android phones and tablets. No WebView, 12 theme families, tablet-optimized layout.

<p align="center">
  <img src="docs/mockups/shots/1-phone-read-sakura.png" width="260" alt="拾简 · 手机阅读 · 樱（默认主题）">
</p>

| 项 | 值 |
| --- | --- |
| 应用名 | 拾简 |
| 包名 | `com.shijian.md` |
| 技术栈 | Kotlin 2.2 · Jetpack Compose · Material 3 · commonmark 0.30 · Coil 3 |
| 构建 | AGP 9.4 · Gradle Wrapper · JDK 21 |
| minSdk / targetSdk | 24 / 37（compileSdk 37），覆盖约 98% 在用设备 |
| 默认主题 | 樱（sakura） |
| 网络 | 不申请 `INTERNET` 权限，文档只在本机处理 |
| 界面语言 | 简体中文 |

---

## 界面预览

> 下列图片是开工前用 HTML 做的**静态视觉稿**（1px ≈ 1dp），用来先把版式层级和配色定下来；正式实现是 Compose 原生渲染，按同一套规范落地。可交互原型见 [`docs/mockups/拾简视觉稿.html`](docs/mockups/拾简视觉稿.html)（浏览器打开，右上角切主题）。

### 手机 · 阅读与编辑

<p align="center">
  <img src="docs/mockups/shots/1-phone-read-sakura.png" width="248" alt="手机阅读：标题层级、任务列表、引用与代码块">
  <img src="docs/mockups/shots/2-phone-edit-sakura.png" width="248" alt="手机编辑：Markdown 源码高亮与底部插入工具条">
</p>

### 主题（12 族里的浅色三族）

<p align="center">
  <img src="docs/mockups/shots/1-phone-read-sakura.png" width="218" alt="樱（默认）">
  <img src="docs/mockups/shots/4-phone-read-caramel.png" width="218" alt="焦糖">
  <img src="docs/mockups/shots/5-phone-read-mint.png" width="218" alt="薄荷">
</p>

### 平板 / 大屏

<p align="center">
  <img src="docs/mockups/shots/3-tablet-sakura.png" width="372" alt="平板三栏：大纲 | 编辑 | 预览">
  <img src="docs/mockups/shots/6-tablet-dark.png" width="372" alt="平板 · 樱深色（跟随系统深色）">
</p>

### 细节

<p align="center">
  <img src="docs/mockups/shots/8-back-to-top.png" width="330" alt="悬浮回顶按钮在五套主题下的形态">
  <img src="docs/mockups/shots/9-font-size.png" width="330" alt="正文字号 10sp / 16sp / 22sp 对照">
</p>

---

## 功能

### 阅读

- 原生 Compose 渲染，不用 WebView：标题 / 列表 / 引用 / 代码块 / 表格 / 分割线 / 图片 / 属性卡 / 公式块各有独立版式
- 标题六级各有形状标识（H1 居中短条 / H2 胶囊 / H3 左竖条 / H4 实心点 / H5 空心点 / H6 短横）
- 任务列表复选框可直接点选，**改动写回源文件**（设置里可关）
- 目录抽屉：手机从左侧滑出，平板（≥840dp）可常驻左栏，正文列宽上限 720dp
- 正文列宽、页边距随窗口宽度自适应；字号 **10–22sp** 可调，右下角显示阅读进度
- 悬浮回顶按钮：滚出首屏后淡入，一点回文首；编辑模式下按光标位置出现，并自动让开底部工具条
- 代码块带语法高亮，行内代码、粗斜体、删除线、高亮、脚注、`<kbd>` 均有对应样式

### 编辑

- 源码编辑 + 实时分栏预览；Markdown 语法着色用 `VisualTransformation` 实现，零拷贝
- 底部插入工具条：标题 / 粗体 / 斜体 / 删除线 / 高亮 / 行内码 / 代码块 / 列表 / 任务 / 引用 / 链接 / 表格 / 分割线 / 图片
- 自动保存：停止输入约 1.2 秒写回（可在设置里关）；未命名文档自动存草稿，下次启动可继续
- 解析在 `Dispatchers.Default` 上做，输入防抖 200ms，长文档不卡输入

### 主题（12 族）

- 浅色 9 族：樱、焦糖、薄荷、天青、森、紫藤、靛、樱红、石墨
- 深色 3 族：松烟、夜幕、墨黑；每个浅色族也派生一套深色（降饱和、提亮主色）
- 三档纹理强度（完整 4.5% / 克制 2% / 极简 0%），6 种矢量底纹（网格 / 交叉 / 点阵 / 星点 / 六边 / 三角）
- 护眼纸质模式可叠加在任意主题上；深浅模式支持跟随系统

### 文件

- SAF 打开 / 另存为 / 写回，支持 `ACTION_VIEW`、`ACTION_EDIT`、`ACTION_SEND` 三种外部入口
- 外部分享来源（QQ / 微信 / 钉钉只给读授权）自动导入应用私有副本继续编辑，原文件不受影响，状态栏标「导入副本」
- 编码自适应：BOM → UTF-8 严格解码 → GBK 回退
- 最近打开列表（本机记录，可单条移除）；授权已失效的条目会标「需重新授权」

### 设置

- 主题与深浅模式、纹理强度与底纹样式、护眼纸质模式
- 正文字号 10–22sp、行距、页边距
- 自动保存开关、任务勾选回写开关、目录面板开关

---

## 屏幕适配

按窗口宽度分三档，用 `LocalConfiguration.screenWidthDp` 判定（见 `ui/WindowSize.kt`）：

| 档位 | 宽度 | 典型设备 | 布局 |
| --- | --- | --- | --- |
| `COMPACT` | < 600dp | 手机竖屏 | 单栏；目录从左侧滑出；顶栏用图标切换阅读 / 编辑 |
| `MEDIUM` | 600–839dp | 手机横屏、折叠屏展开、小平板 | 内容区加宽，模式切换用胶囊按钮 |
| `EXPANDED` | ≥ 840dp | 平板 | 大纲 / 编辑 / 预览可三栏并存，大纲栏 212dp |

另外：所有可点元素按 ≥ 44dp 触摸目标设计；排版尺寸全部走 dp / sp，不写死像素，规避不同密度屏幕的错位。

---

## 架构

```
外部入口（ACTION_VIEW / EDIT / SEND）
   ↓
DocIo            SAF 与文件读写、URI 授权判定、编码嗅探、私有副本导入
   ↓
AppViewModel     文档状态、防抖解析、防抖保存、任务回写、最近记录
   ↓
MdParser         commonmark + GFM 扩展 → MdBlock 语义块
   ↓
Compose          ReaderPane / EditorPane 按块渲染
```

目录结构：

```
app/src/main/java/com/shijian/md/
├── MainActivity.kt            入口，处理外部 Intent
├── AppViewModel.kt            文档状态、解析、保存、任务回写
├── data/                      SettingsStore / RecentStore / DocIo
├── md/                        MdModel / MarkdownParser（commonmark → 语义块）
└── ui/
    ├── AppRoot.kt             主题包装 + 页面路由 + 提示条
    ├── DocumentScreen.kt      顶栏、模式切换、目录、阅读底栏
    ├── WindowSize.kt          断点分级
    ├── components/            矢量图标、行内渲染、块级渲染、代码高亮
    ├── editor/                源码编辑与插入工具条
    ├── reader/                阅读分栏与目录
    ├── settings/              主题与偏好设置
    └── theme/                 主题令牌与 12 族配色
```

---

## 构建

环境：JDK 21 + Android SDK（compileSdk 37）。依赖都在 `gradle/libs.versions.toml` 里，Gradle Wrapper 会自己拉。

```powershell
.\gradlew.bat :app:assembleDebug        # 产物：app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat :app:testDebugUnitTest    # 解析层单元测试
```

本仓库额外带了一个 `build.cmd`：它会先清空 `JAVA_HOME` 再调用 Gradle，用来绕开本机 `JAVA_HOME` 指向失效路径的问题，等价于：

```powershell
.\build.cmd :app:assembleDebug
.\build.cmd :app:testDebugUnitTest
```

安装到设备：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 文档

| 文件 | 内容 |
| --- | --- |
| [`docs/项目设计文档.md`](docs/项目设计文档.md) | 需求、设计决策、元素规范、里程碑 |
| [`docs/实现说明.md`](docs/实现说明.md) | v1 实现说明、已知边界、验证记录 |
| [`docs/mockups/拾简视觉稿.html`](docs/mockups/拾简视觉稿.html) | 可交互原型（樱 / 焦糖 / 薄荷 + 樱深色） |
| [`docs/mockups/README.md`](docs/mockups/README.md) | 视觉稿清单与评审要点 |

---

## 已知边界（有意为之）

- **远程图片不加载**：应用未申请 `INTERNET` 权限，远程图片显示占位卡；本地相对路径按文档所在目录解析
- 行内图片（段落中间）以 `[图片: 描述]` 占位，独立成段的图片才整幅渲染
- Mermaid 不做；数学公式 v1 以等宽降级呈现，v1.2 接真渲染
- 行内所见即所得排在 v1.1，v1 是「源码编辑 + 分栏预览」
- 外部分享来源只能读时导入私有副本编辑，原文件不会被改动（Android 的授权模型决定）
- 尚未做多机型 / 多 Android 版本的兼容性矩阵测试

---

## 后续版本

- **v1.1**：行内所见即所得（在阅读视图里直接改文字）
- **v1.2**：数学公式真渲染（KaTeX 兼容子集）；Mermaid 不做

---

## 应用图标

图形取自应用自身的版式语言——**短标题条 + 三行正文条**（对应 H1 短横与正文行），底色是默认主题「樱」的主色阶梯（#F79BB6 → #E0668C → #821C42）。

![应用图标](docs/mockups/shots/7-icon-showcase.png)

- 自适应图标：`drawable/ic_launcher_background.xml`（渐变）+ `ic_launcher_foreground.xml`（图形）+ `ic_launcher_monochrome.xml`（Android 13 主题化图标剪影）
- 低版本回退：`mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png` 与 `ic_launcher_round.png`
- 重新生成位图：`powershell -File tools\make-icons.ps1 -Sizes "48,72,96,144,192"`（几何改动后需同步手写矢量 XML）

---

## 致谢

- 主题设计的参考起点是 [Typora](https://typora.io/) 的 phycat 主题族，本项目按其配色思路重新推导为 Compose 令牌，未直接复制其 CSS 数值
- Markdown 解析：[commonmark-java](https://github.com/commonmark/commonmark-java)
- 图片加载：[Coil](https://coil-kt.github.io/coil/)
