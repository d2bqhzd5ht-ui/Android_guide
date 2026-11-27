# UI 规范（西湖文旅风格，A/B/C/D 统一）

目标：所有页面采用现代、简约、自然的文旅产品风格。统一使用 Compose Material 3 与 `ScenicGuideTheme`，不使用 WebView，不在页面中另建一套主题色。

## 1. 配色

颜色统一维护在 `app/src/main/java/com/example/scenicguide/ui/theme/Theme.kt`。

| 用途 | 色值 | Compose 映射 |
| --- | --- | --- |
| 西湖墨绿 | `#526F61` | `primary` |
| 竹青 | `#81988A` | `secondary` |
| 浅青灰 | `#DCE5DD` | `secondaryContainer` |
| 宣纸白 | `#F7F6F2` | `background` |
| 卡片白 | `#FFFFFF` | `surface` |
| 主要文字 | `#303A35` | `onSurface` |
| 次要文字 | `#7B827D` | `onSurfaceVariant` |
| 轻边框 | `#E8E8E2` | `outline` |
| 错误 | `#C45F55` | `error` |

绿色只用于主要操作、选中态和少量强调。普通文字不用纯黑，页面背景统一暖白。后端下发的多彩 CSS 色值和 Emoji 图标不直接作为页面主样式。

## 2. 字体与间距

- 页面主标题：`headlineSmall`，24sp
- 模块标题：`titleMedium`，18sp
- 卡片标题：`titleSmall`，16sp
- 正文：`bodyMedium`，14sp
- 辅助说明：`bodySmall` / `labelSmall`，12sp
- 页面水平内边距：16dp；模块间距：16–24dp
- 内容尽量左对齐，避免信息堆叠和无意义装饰

## 3. 组件

共享组件位于 `ui/components/GuideComponents.kt` 与 `StateViews.kt`：

- `GuideCard`：白色、8dp 圆角、1dp `outline` 边框、零阴影
- `GuideTopBar`：统一页面标题、副标题、线性返回图标与操作区
- `SectionTitle`：统一模块标题
- `EmptyView` / `LoadingView` / `ErrorView`：统一空、加载和错误状态
- `PoiImage`：加载后端真实图片，失败时使用浅青灰背景与线性景点图标
- 按钮和输入框使用 Material 3 主题形状；主按钮至少 48dp 高，加载时显示进度指示

禁止 Emoji 作为导航或菜单图标，禁止大面积渐变、发光边框、重阴影、彩色竖线和玻璃拟态。图标统一使用 Material Icons Outlined。

## 4. 移动端导航

`AppRoot.kt` 使用原生 `NavigationBar`，顶级入口为：

`首页 | AI 导览 | 游玩攻略 | 景点 | 我的`

底栏只在首页、AI 问答、景点列表、个人中心显示；登录、注册、详情和编辑页隐藏，键盘弹出时也隐藏。游玩攻略尚未提供页面时只显示“暂未开放”，不虚构路线数据。

## 5. 交互约定

- 所有网络页必须有加载中、失败可重试、空态三种状态，不出现白屏
- 离开页面取消进行中的长任务，聊天页继续保留流式请求与语音清理
- 文案使用中文，句尾不加句号；错误使用“…失败，请重试”
- 样式改造不得修改 ViewModel、Repository、接口字段、路由参数和已有业务回调

## 6. 首页接口字段

`GET /api/v1/home → data`：

```text
banners:            [{ id: Int, title: String, image: String, color: String }]
quickEntries:       [{ icon: String, label: String, path: String, color: String }]
hotPois:            [{ id: Int, name: String, category: String, description: String? }]
recommendedRoutes:  [{ id: Int, name: String, theme: String, description: String?, estimated_minutes: Int }]
hotProducts:        []
stats:              { totalInteractions: Int, todayInteractions: Int, positiveRate: Double }
```

来源：`whoisthe4th/backend/src/routes/home.ts`，客户端 DTO 在 `data/model/HomeModels.kt`。
