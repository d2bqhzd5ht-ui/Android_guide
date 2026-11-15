# UI 规范（B 定稿，A/C/D 对齐用）

目标：三屏之外的页面直接复用同一套色板和组件，避免各人各写一套。

## 1. 配色

主色系写在 `app/src/main/java/com/example/scenicguide/ui/theme/Theme.kt`，不要在页面里写死新颜色。

| 用途 | 色值 | 说明 |
| --- | --- | --- |
| 主色 | `#0E7C66` | 按钮、选中态、强调文字、TopAppBar 主题色 |
| 深色渐变 | `#0B5E4E` → `#063A30` | 登录页背景渐变（自上而下 `#0E7C66` → `#0B5E4E` → `#063A30`） |
| 浅色提示 | `#B7EADB` / `#9BD4C5` | 深色背景上的副标题、说明文字 |
| 卡片底色 | `Color.White` | 登录卡片 |
| 次要容器 | `MaterialTheme.colorScheme.surfaceVariant` | 景点卡片、轮播占位、未选中圆点 |
| 错误 | `MaterialTheme.colorScheme.error` | 错误文案 |

首页轮播的渐变**不要写死**：后端 `banners[].color` 下发的是 CSS 渐变串（如 `linear-gradient(135deg, #1989fa, #4db8ff)`），`HomeScreen` 里的 `bannerBrush()` 会从串里取十六进制色值，取不到才退回主题色。快捷入口同理，用 `quickEntries[].color`。

## 2. 文字与层级

- 页面内大标题：`headlineSmall` + `FontWeight.Bold`
- 分节标题（如「热门景点」）：`titleMedium` + `FontWeight.SemiBold`
- 卡片标题：`titleSmall` + `FontWeight.SemiBold`
- 正文说明：`bodySmall`，颜色 `onSurfaceVariant`
- 标签/元信息：`labelSmall`，颜色 primary
- 空态与提示：`bodySmall` + `onSurfaceVariant`

## 3. 组件与间距

复用 `ui/components/StateViews.kt` 和 `ui/home/HomeScreen.kt` 里的私有组件思路（跨包复用请自行提取到 `ui/components/`，不要复制粘贴）：

- `LoadingView(text)`：整页加载，居中转圈 + 一行文案（例：「正在加载首页…」）
- `ErrorView(message, onRetry)`：整页错误，红色文案 + 「重试」文本按钮
- `SectionTitle(text)`：分节标题
- TopAppBar：标题「景区导览」+ 右侧文本按钮（如「退出」），标题加 `FontWeight.SemiBold`
- 卡片圆角：14dp（列表卡片）/ 16dp（大卡片、轮播、AI 问答入口）
- 页面水平内边距 16dp，块与块间距 16dp，卡片内部 padding 14–18dp
- 主按钮高度 48dp、整行宽度；`loading` 时按钮内显示 20dp 的 `CircularProgressIndicator`（白色、strokeWidth 2dp）
- 输入框统一 `OutlinedTextField` + `singleLine = true`，密码用 `PasswordVisualTransformation()`

## 4. 交互约定

- 所有网络页必须有 加载中 / 失败可重试 / 空态 三种状态，不要出现白屏
- 离开页面要能取消进行中的长任务（参考 `ChatScreen` 的 `DisposableEffect` → `viewModel.stop()`）
- 文案用中文、句尾不加句号；状态动词用「正在…」，错误用「…失败，请重试」

## 5. 已知接口字段（与后端源码一致，别再猜）

`GET /api/v1/home` → `data`：

```
banners:            [{ id: Int, title: String, image: String, color: String }]
quickEntries:       [{ icon: String, label: String, path: String, color: String }]
hotPois:            [{ id: Int, name: String, category: String, description: String? }]
recommendedRoutes:  [{ id: Int, name: String, theme: String, description: String?, estimated_minutes: Int }]
hotProducts:        []
stats:              { totalInteractions: Int, todayInteractions: Int, positiveRate: Double }
```

来源：`whoisthe4th/backend/src/routes/home.ts`，客户端 DTO 在 `data/model/HomeModels.kt`。
