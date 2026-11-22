# TakusanManga

TakusanManga 是一个使用 Kotlin 与 Jetpack Compose 开发的 Android 原生在线漫画阅读器。用户自行导入外部来源规则，应用通过统一来源接口解析内容并在阅读器中展示。

> 当前状态：Android 工程骨架、Design System 主题基础和 App Shell 导航已建立；业务页面尚未实现。

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

## 安全

请不要在公开 Issue 中发布可直接利用的漏洞细节、Cookie、账号信息、签名 URL 或私密内容。安全报告请使用仓库 Security 页面中可用的私密漏洞报告入口；如入口尚未启用，请先联系维护者开通。

## 许可证

本项目使用 [MIT License](LICENSE)。
