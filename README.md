# 拾简 · Shijian

面向**手机与平板**的 Markdown 阅读 / 编辑应用。原生 Kotlin + Jetpack Compose 实现，视觉语言参考 Typora 的 phycat 主题族后重新设计，针对触屏与多尺寸屏幕重排。

| 项 | 值 |
| --- | --- |
| 应用名 | 拾简 |
| 包名 | `com.shijian.md` |
| 技术栈 | Kotlin 2.2 · Jetpack Compose · Material 3 |
| minSdk / targetSdk | 24 / 37（compileSdk 37） |
| 默认主题 | 樱（sakura） |
| 网络 | 无（未申请 INTERNET 权限，文档只在本机处理） |

---

## 构建

本机 `JAVA_HOME` 指向的路径不存在，所以构建统一走 `build.cmd`（内部清空 JAVA_HOME 后调用 Gradle，使用 JDK 21）：

```powershell
.\build.cmd :app:assembleDebug        # 产物：app\build\outputs\apk\debug\app-debug.apk
.\build.cmd :app:testDebugUnitTest    # 解析层单元测试
```

安装到设备：

```powershell
D:\software\Android\SDK\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 已实现（v1）

**阅读**
- 原生 Compose 渲染，不用 WebView；标题 / 列表 / 引用 / 代码块 / 表格 / 分割线 / 图片 / 属性卡 / 公式块全部有独立版式
- 任务列表复选框可直接点选，**改回源文件**（设置里可关）
- 目录抽屉：手机从左侧滑出，平板（≥840dp）常驻左栏，正文列宽上限 720dp
- 正文列宽、页边距随窗口宽度自适应；字号 10–22sp 可调，右下角显示阅读进度
- 悬浮回顶按钮：滚出首屏后淡入，一点回文首；编辑模式下按光标位置出现，并自动让开插入工具条

**编辑**
- 源码编辑 + 分栏预览；Markdown 语法着色（VisualTransformation 实现，零拷贝）
- 底部插入工具条：标题 / 粗斜 / 删除线 / 高亮 / 行内码 / 代码块 / 列表 / 任务 / 引用 / 链接 / 表格 / 分割线 / 图片
- 自动保存（停止输入约 1 秒写回），未命名文档自动存草稿，下次启动可继续

**主题（12 族）**
- 浅色：樱、焦糖、薄荷、天青、森、紫藤、靛、樱红、石墨
- 深色：松烟、夜幕、墨黑；浅色族也会派生一套深色（降饱和、提亮主色）
- 三档纹理强度（完整 4.5% / 克制 2% / 极简 0%），6 种矢量底纹（网格 / 交叉 / 点阵 / 星点 / 六边 / 三角）
- 护眼纸质模式可叠加任意主题；深浅模式支持跟随系统

**文件**
- SAF 打开 / 另存为 / 写回，`ACTION_VIEW`、`ACTION_EDIT`、`ACTION_SEND` 三种外部入口
- 外部分享来源（QQ / 微信 / 钉钉只给读授权）自动导入应用私有副本继续编辑，原文件不受影响，状态栏标「导入副本」
- 编码自适应：BOM → UTF-8 严格解码 → GBK 回退
- 最近打开列表（本机记录，可单条移除）

---

## 目录结构

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

设计与视觉稿见 `docs/`：
- `docs/项目设计文档.md`：完整需求与设计决策
- `docs/实现说明.md`：v1 实现说明与验证记录
- `docs/mockups/拾简视觉稿.html`：可切换主题的交互原型

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
