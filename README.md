# 景区 AI 导览 Android 客户端

第八组课程项目「景区导览 AI 数字人」的游客端 Android 客户端，覆盖登录、首页、景点列表、景点详情、AI 问答（含语音）、失物招领、个人中心共 7 个页面。

> **新加入的同学请先读 [docs/交接.md](docs/交接.md)**：后端接口、启动方式、硬约束和分工都在里面，读完就能直接开工。

## 技术栈

以下是团队约定的基线选型，最终以工程骨架（`app/` 模块）的实际配置为准。

| 层 | 选型 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM：`View` → `ViewModel`（StateFlow）→ `Repository` → `RemoteDataSource` |
| 网络 | OkHttp（SSE 手写解析）+ Retrofit + kotlinx.serialization |
| 异步 | Kotlin Coroutines + Flow |
| 本地存储 | DataStore（登录态、偏好）；Room 按需引入 |
| 音频 | `AudioRecord` / `MediaRecorder` 录音，`MediaPlayer` 播放 |
| 构建 | Gradle Kotlin DSL |

## 分支规范

- **`main` 只接受 PR**，任何人都不要直接向 `main` push。
- **每人一条 `feature/` 分支**，长期保留，从 `main` 切出，完成后向 `main` 提 PR。
- 分支名与分工对应：

  | 分支 | 负责范围 |
  | --- | --- |
  | `feature/a-voice` | 语音链路（录音 → ASR → 问答 → TTS 播放） |
  | `feature/b-network` | 网络基座、登录、首页、AI 问答 |
  | `feature/c-poi` | 景点列表、景点详情 |
  | `feature/d-skeleton` | 工程骨架、失物招领、打包 |

- 提交前先同步 `main`（`git pull --rebase origin main`），冲突优先解决共享文件（`build.gradle.kts`、`AndroidManifest.xml`、依赖清单）。
- 新功能尽量放在自己的包路径下，避免多人同时改同一个文件。

## 目录约定（规划）

```
app/src/main/java/<包名>/
├── data/
│   ├── remote/      # Retrofit / OkHttp 接口与 DTO
│   ├── local/       # DataStore、Room
│   └── repository/
├── ui/
│   ├── login/       # 登录
│   ├── home/        # 首页
│   ├── poi/         # 景点列表、景点详情
│   ├── chat/        # AI 问答（含语音）
│   ├── lostfound/   # 失物招领
│   └── profile/     # 个人中心
└── common/          # 网络配置、常量、工具
```

## 快速开始

1. `git clone` 本仓库，用 Android Studio 打开。
2. 先把后端跑起来（见 [docs/交接.md](docs/交接.md) 第 2 节）。
3. 模拟器里后端地址用 `http://10.0.2.2:8000`，真机用电脑局域网 IP。
4. 编译运行。

## 文档

- [docs/交接.md](docs/交接.md) —— 交接主文档
- [docs/openapi.yaml](docs/openapi.yaml) —— 后端接口契约（OpenAPI 3.0.3）
