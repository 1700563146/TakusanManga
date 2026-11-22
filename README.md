# TakusanManga

TakusanManga 是一个使用 Kotlin 与 Jetpack Compose 开发的 Android 原生在线漫画阅读器。用户自行导入外部来源规则，应用通过统一来源接口解析内容并在阅读器中展示。

> 当前状态：M1 UI 原型已通过测试与设计审查，M2 Domain、MangaSource 与 Repository 契约已建立，UI 原型已接入 Fake MangaSource，并已通过公开 API 快照与兼容性检查；M3 已接入带 Cookie 隔离、取消传播、响应限制及 URL/DNS/重定向安全校验的 Source HTTP 客户端，真实来源解析和持久化尚未接入。

## 项目边界

- 应用不内置漫画内容、图源合集、账号服务或云端书架。
- 不下载或离线保存漫画，不将漫画图片、章节正文或压缩包写入磁盘。
- 图片只允许受容量和生命周期限制的内存缓存。
- V1 不动态加载 APK、DEX、动态库或任意 Kotlin/Java 类。
- 不绕过付费墙、DRM、验证码或访问权限控制。

## 计划功能

- 自有 JSON 来源规则导入、校验和管理。
- 漫画搜索、详情、章节目录和页面图片 URL 解析。
- 分页阅读与纵向连续阅读。
- Light、Dark 和跟随系统主题。
- 请求取消、来源隔离、错误诊断和重试。
- 后续按验证结果增加受限 JavaScript 插件与兼容格式。

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Gradle Version Catalog
- Android Gradle Plugin 8.13
- Kotlin Binary Compatibility Validator
- OkHttp 5.4（Apache-2.0）
- `minSdk` 26，`compileSdk` / `targetSdk` 36

## 构建

需要 JDK 21 和 Android SDK 36。

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

Linux 或 macOS：

```bash
./gradlew :app:assembleDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

公共 API 快照位于 `app/api/app.api`。修改公开契约后运行：

```powershell
.\gradlew.bat :app:apiDump
.\gradlew.bat :app:apiCheck
```

## 安全

请不要在公开 Issue 中发布可直接利用的漏洞细节、Cookie、账号信息、签名 URL 或私密内容。安全报告请使用仓库 Security 页面中可用的私密漏洞报告入口；如入口尚未启用，请先联系维护者开通。

## 许可证

本项目使用 [MIT License](LICENSE)。
