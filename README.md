# 功能机模式 (Feature Phone Launcher)

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-34-orange)
![Version](https://img.shields.io/badge/Version-1.2.3%20(6)-brightgreen)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)

把安卓智能手机变成一台"真正的功能机"：复古 LCD 屏幕 + 实体按键布局 + 常驻 Kiosk 模式。

> 本应用由紫空幻影与 AI 共同设计开发，永久免费使用，不包含任何付费内容、广告或内购项目。

---

## 截图

截图放在 [`docs/screenshots/`](docs/screenshots/) 目录，按下表命名；放入后取消注释即可显示。

| 待机界面 | 菜单界面 | 拨号界面 |
|:---:|:---:|:---:|
| `01-idle.png` | `02-menu.png` | `03-dialer.png` |

| 通讯录 | 通话记录 | 信息 |
|:---:|:---:|:---:|
| `04-contacts.png` | `05-call-log.png` | `06-messages.png` |

| 颜色设置 | 显示设置 | 关于 |
|:---:|:---:|:---:|
| `07-color.png` | `08-display.png` | `09-about.png` |

<!-- 截图就位后，删除下面注释标记即可显示
![待机界面](docs/screenshots/01-idle.png)
![菜单界面](docs/screenshots/02-menu.png)
![拨号界面](docs/screenshots/03-dialer.png)
-->

---

## 功能特性

### 核心体验
- **复古 LCD 屏幕**：信号 / 时间 / 电量 + 待机大时钟 / 日期 / 农历 / 运营商
- **完整实体按键**：左右软键、五向导航、绿键（拨打）、红键（挂断/返回）、3×4 数字键
- **拨号状态内联在主界面**：点数字键直接进入拨号屏，与桌面共用同一块屏幕和键盘，不再有"第二套拨号页"
- **T9 智能拨号**：号码包含匹配 + 姓名拼音首字母匹配（如「张三」→ 输入 `97` 即可命中）
- **统一的 500ms 过渡**：Activity 跳转与待机 / 菜单 / 拨号切换全部使用 500ms 淡入淡出

### 内置功能
- **电话**：拨号、T9 选人、紧急号码、无电话应用时自动回退到系统拨号界面
- **信息**：短信列表查看、调起系统短信发送
- **通讯录**：联系人列表、点击拨号、添加联系人
- **通话记录**：列表查看、点击回拨、长按菜单（加联系人 / 复制号码 / 删除记录）
- **软件列表**：查看已安装应用并直接启动
- **快捷应用**：把常用应用放进菜单首页（最多 8 个）
- **设置**：联网、颜色、显示、语言与单位、通知管理、省电优化、其他设置、防护提示、关于

### 自定义模块
- **电量模块**：实时显示电量
- **天气模块**：Open-Meteo 免费接口 + 定位，显示当地天气与温度（可切摄氏 / 华氏）
- **空白模块**：纯占位，保持按键区底色统一
- 模块 1（屏幕中部）与模块 2（按键区底部）可分别选择，且不能重复

### 外观与交互
- **颜色设置**：屏幕文字色 / 屏幕背景色 / 按键文字色 / 按键背景色
  - 一键「黑底白字」「白底黑字」
  - **冲突互斥**：会看不见的配色组合（白字配白底等）直接禁止选择
  - 拨号号码、T9 列表同样跟随配色
- **字体大小**：标准 / 大（1.3×）/ 巨大（1.5×），标题、菜单、各类列表（含行高与内边距）一起放大
- **按键震动反馈**：所有可点击控件统一震动
- **屏幕常亮**：待机界面保持亮屏

### 常驻前台机制（三层）
1. **前台服务**：低优先级通知保持应用存活
2. **默认桌面**：注册为 HOME，按 Home 键回到本应用
3. **Kiosk 锁定**：Device Owner + LockTask，真正无法退出

### 其他
- 农历显示（1900–2039）
- 开机自启（Android 10+ 由系统拉起默认桌面）
- 新短信提醒（Toast + 振动）
- 防截屏（`FLAG_SECURE`）

---

## 更新日志

### 1.2.3 (versionCode 6)
新增「防护提示」页与状态栏/来电相关调整，并修复相关缺陷：
- **防护模式改为「防护提示」**：本软件不再接管铃声（非默认拨号应用在 Android 9+ 拿不到来电号码，原功能在多数机型上本来就不生效），页面改为纯文字说明，引导用户使用系统电话应用的骚扰拦截与全屏来电设置；入口移到「其他设置」下方
- **来电时自动退到后台**：检测到 `CALL_STATE_RINGING` 时 `moveTaskToBack`，让系统电话应用有机会全屏弹出（部分 ROM 仍会以通知形式显示，见「已知限制」）
- **状态栏信号/电量固定字号**：信号 20sp、电量 26sp 不随「字体大小」放大，避免挤压中间的时间与日期
- **补申请 `READ_PHONE_STATE`**：信号栏与来电检测都依赖该权限，此前从未申请过，导致这两项功能可能静默失效；已加入启动批量申请，老用户升级后单独补申请一次
- **移除来电静音实现**：删除不再使用的 `CallGuardManager` 调用，老用户残留的 `whitelist_enabled` 不会再偷偷把铃声压低；随之移除 `MODIFY_AUDIO_SETTINGS` 权限
- 防护提示页补上语言切换支持，并支持用方向键上下翻页（非触摸机型上原本看不全说明文字）
- 授权完成后立即重新注册电话状态监听，信号栏不必等到下次返回桌面才恢复

### 1.2.2 (versionCode 5)
代码评审复核后的补充加固：
- 天气取位置：超时/停止监听/回调成功/注册失败四条路径统一 `removeUpdates`，不再残留定位监听器
- T9 匹配：新输入先取消排队中的查询任务（`Future.cancel`），快速连按不再堆积无效查询
- 崩溃熔断后主界面给出提示（不再出现"闪退后毫无反应"）；崩溃日志中的长数字串做脱敏
- 电量模块动态注册系统广播补上 `RECEIVER_NOT_EXPORTED` 标记（targetSdk 34 适配）
- 前台服务显式声明 `android:stopWithTask="false"`，明确划掉任务后服务的存活行为

### 1.2.1 (versionCode 4)
安全与长期驻留加固：
- 短信内部广播改为**签名级权限** + `RECEIVER_NOT_EXPORTED`，第三方应用无法再伪造「新短信」提示，同时修复 Android 14 上接收器注册抛异常导致提醒失效的问题
- 移除代码中从未使用的权限（SEND_SMS / WRITE_CONTACTS / DISABLE_KEYGUARD / WAKE_LOCK / ACCESS_FINE_LOCATION），缩小攻击面
- 崩溃处理器不再自动勾选免责声明；连续崩溃 10 秒内超过 3 次时停止自动重启，避免死循环；崩溃日志超过 512KB 自动轮转
- 天气定位请求去重 + 30 秒超时，避免长期驻留时重复注册 LocationListener
- 来电静音状态持久化，进程被杀后下次启动自动恢复铃声
- T9 查询改为单线程串行 + 结果令牌，避免快速输入产生线程风暴与列表乱序
- 服务被划掉后重启、页面销毁后回写 UI 均加了保护

### 1.2 (versionCode 3)
- 拨号状态完全集成到主界面，移除独立的 `DialerActivity` 与 `activity_dialer.xml`
- 修复"点数字键闪回主界面"的崩溃（T9 列表 `ViewHolder` 把子 View 当作 itemView）
- 界面切换统一 500ms 过渡动画（待机 / 菜单 / 拨号）
- 拨号号码纳入颜色设置，深色主题下不再"看不见"
- 字体大小对列表行生效（通讯录 / 通话记录 / 信息 / 软件列表 / T9 列表），行高与内边距同步放大
- 「关于」页显示 `版本 1.2 (3)`，便于确认手机上装的是哪一次构建
- 启动权限申请改为每次安装只请求一次，避免反复弹窗
- 来电防护改为只压低铃声音量（不再全局静音）

### 1.1
- 颜色 / 字体 / 单位 / 语言 / 联网 / 通知 / 防护等设置页
- 电量与天气信息模块、快捷应用、极简模式
- Kiosk、设备管理员、后台应用管理、默认桌面

---

## 项目结构

```
FeaturePhoneLauncher/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/featurephone/launcher/
│       │   ├── FeaturePhoneApp.kt          # Application：崩溃日志、通知渠道、字体初始化
│       │   ├── MainActivity.kt             # 待机 / 菜单 / 拨号（内联）三态桌面
│       │   ├── FeaturePhoneService.kt      # 常驻前台服务
│       │   ├── BootReceiver.kt             # 开机自启
│       │   ├── SmsReceiver.kt              # 短信接收提醒
│       │   ├── DeviceAdminReceiver.kt      # 设备管理员
│       │   ├── ColorManager.kt             # 配色与冲突规则
│       │   ├── ShortcutManager.kt          # 快捷应用
│       │   ├── apps/                       # 二级页面（信息/通讯录/通话记录/软件/设置…）
│       │   ├── modules/                    # 信息模块：电量 / 天气 / 空白
│       │   ├── ui/MenuAdapter.kt           # 菜单列表（反色选中）
│       │   ├── model/AppItem.kt
│       │   └── util/                       # 字体、权限、T9、农历、动画、Kiosk、震动等
│       └── res/                            # layout / drawable / values / anim / xml
├── docs/screenshots/                       # 截图目录（见上文命名表）
├── gradle/wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── keystore.properties.template            # 签名配置模板（真实口令不入库）
├── LICENSE
└── README.md
```

---

## 构建方法

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Android Studio（推荐）
1. `Open an existing project`，选择本目录
2. 等待 Gradle Sync 完成
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`

### 命令行
仓库目前**没有附带 `gradlew` 脚本与 wrapper jar**，如需命令行构建：

```bash
# 方式一：本机已安装 Gradle 8.2+
gradle assembleDebug
gradle assembleRelease

# 方式二：先生成 wrapper（需已安装 Gradle）
gradle wrapper --gradle-version 8.2
./gradlew assembleDebug
```

产物路径：
- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`（未配置签名时为 `app-release-unsigned.apk`）

### Release 签名（可选）
Release 签名口令**只从本地 `keystore.properties` 读取**，仓库里不保留任何明文口令：

```bash
# 1. 复制模板
cp keystore.properties.template keystore.properties

# 2. 填入真实值（storeFile、storePassword、keyAlias、keyPassword）
# 3. 再执行 assembleRelease
```

> 没有 `keystore.properties` 时，Release 仍可编译，但产物是**未签名 APK**，无法直接安装发布。
> 该文件与 `*.jks` 均已被 `.gitignore` 排除。

---

## 安装与配置

### 1. 安装 APK
```bash
adb install -r app-release.apk
```

### 2. 基础设置（必做）
打开应用 → `设置`：

1. **权限管理**：授予电话、短信、通讯录、通话记录权限（应用会列出具体缺失项）
2. **设为默认桌面**：在系统设置中选择本应用

### 3. 设备管理员（可选）
`其他设置 > 设备管理员` 连续点击 5 次进入激活页，或：

```bash
adb shell am start -a android.app.action.ADD_DEVICE_ADMIN \
    --eu android.app.extra.DEVICE_ADMIN \
    com.featurephone.launcher/.DeviceAdminReceiver
```

> 设备管理员 ≠ 设备所有者，仅提供锁屏、禁用相机等基础能力，**不能**单独实现 Kiosk。

### 4. 启用真正的 Kiosk（完全锁定）
需要把应用设为**设备所有者（Device Owner）**：

```bash
# 建议在恢复出厂设置后、未登录任何账号前执行
adb shell dpm set-device-owner com.featurephone.launcher/.DeviceAdminReceiver
```

成功后进入 `其他设置 > Kiosk 锁定模式` 启用。

> Device Owner 一旦设置通常需要恢复出厂才能移除，请谨慎操作。

### 5. 开机自启
小米 / 华为 / OPPO 等需在系统设置中允许「自启动」「后台运行」。
Android 10+ 禁止后台启动界面，因此仅由前台服务兜底，桌面由系统按「默认桌面」自动拉起。

---

## 使用说明

### 按键操作

| 按键 | 待机 | 菜单 | 拨号 |
|------|------|------|------|
| 左软键 | 打开菜单 | 确认 / 进入 | 清空号码 |
| 右软键 | 通讯录 | 返回待机 | 返回待机 |
| 上 / 下 | 打开菜单 | 移动选择 | — |
| OK | 打开菜单 | 进入选中项 | — |
| 绿键（拨打） | 进入拨号 | 执行选中项 | 拨打 |
| 红键（挂断） | 返回系统桌面 | 返回待机 | 返回待机 |
| 数字键 | 进入拨号并输入 | — | 输入号码（触发 T9） |

### T9 智能拨号示例
- 号码匹配：输入 `138` → 命中所有包含 `138` 的号码
- 拼音首字母：通讯录「张三」→ 输入 `97`（Z=9、S=7）即可命中
- 点击匹配行直接拨打

### 退出功能机模式

**普通模式**：按 Home 键或把默认桌面切回其他应用。

**Kiosk 模式**：
1. `其他设置 > Kiosk 锁定模式` 先停用
2. 清除设备所有者：
   ```bash
   adb shell dpm remove-active-admin com.featurephone.launcher/.DeviceAdminReceiver
   ```
3. 必要时恢复出厂设置

---

## 技术实现要点

### 三层常驻方案

| 层级 | 技术 | 效果 | 限制 |
|------|------|------|------|
| 1 | Foreground Service | 进程不易被回收 | 用户可从最近任务划掉 |
| 2 | HOME 桌面 | 按 Home 回到本应用 | 用户可更换默认桌面 |
| 3 | Device Owner + LockTask | 真正锁定，屏蔽系统按键 | 需 ADB 设置 |

### 界面与视觉
- 屏幕 / 按键区均为 `LinearLayout`，配色在运行时由 `ColorManager` 统一注入
- 菜单选中项采用反色（深底 + 屏幕底色文字）
- 界面切换使用 `TransitionManager` + `Fade`/`ChangeBounds`，时长 500ms
- 字体缩放采用 `SP` 缩放 + `View#setTag` 缓存原始字号，列表复用时不会累加

### 拨号与 T9
- `T9Matcher` 同时做「号码包含」与「拼音首字母前缀」匹配
- 拼音首字母通过 GB2312 编码区间推断，不依赖第三方拼音库、不增加包体
- 拨号界面的号码、匹配列表均跟随颜色与字体设置

### 稳定性
- 全局 `UncaughtExceptionHandler` 记录崩溃日志到
  `/sdcard/Android/data/com.featurephone.launcher/files/crash_log.txt`
- 拨号权限申请带回调续拨；无 `ACTION_CALL` 处理者时自动回退 `ACTION_DIAL`

---

## 已知限制

1. **无法完全禁用状态栏下拉**：需要 Device Owner 调用 `setStatusBarDisabled()`
2. **无法拦截电源键**：系统限制，任何应用都做不到
3. **部分品牌 ROM 限制后台**：需要手动允许自启动 / 后台运行
4. **短信发送**：调起系统短信应用，未内置短信编写界面
5. **不支持 VoLTE 视频通话**：仅普通语音电话
6. **来电全屏 / 来电防护**：Android 9 起非默认拨号应用通常拿不到来电号码，本软件不接管铃声；「来电时退到后台」只是尽量让系统电话应用恢复前台，是否全屏仍由系统电话应用与 ROM 决定（页面内有各品牌设置路径）
7. **开机自启**：Android 10 起禁止后台启动界面，`BootReceiver` 只拉起前台服务；桌面由系统按「默认桌面」启动
8. **Kiosk 的前提是 Device Owner**：仅激活「设备管理员」不会生效，必须用 ADB 设置设备所有者
9. **紧急号码**：110/120/119 等会跳转系统拨号界面二次确认，属系统安全限制

---

## 后续可扩展

- [ ] 内置短信编写界面（T9 输入法）
- [ ] 闹钟 / 计算器 / 日历等小工具
- [ ] 收音机（依赖硬件）
- [ ] 蓝牙连接管理
- [ ] 流量监控
- [ ] 多套 LCD 配色主题（蓝屏 / 黑屏 / 绿屏）
- [ ] 自定义按键映射
- [ ] 老人模式（超大字体、SOS 紧急呼叫）

---

## 签名与密钥安全

- 仓库内**不含任何签名口令**：`app/build.gradle.kts` 只从 `keystore.properties` 读取，读不到就**不配置签名**（产出未签名 APK），没有明文回退值。
- `.gitignore` 已排除：`keystore.properties`、`*.jks`、`*.keystore`、`*.p12`、`*.pfx`。
- `featurephone.jks` 仍在项目目录内：Git 不会提交它，但**把整个文件夹压缩上传时会一并带走**。建议移到项目之外（例如 `D:\android-keys\featurephone.jks`），并在 `keystore.properties` 中写绝对路径：
  ```properties
  storeFile=D:/android-keys/featurephone.jks
  ```
- 如果曾经把带明文口令的版本推送到公开仓库：请**更换 keystore 与口令**，并清理历史提交（`git filter-repo` 等）。
- 注意：**通过 GitHub 网页"拖拽上传"时 `.gitignore` 不生效**。发布源码请用命令行 `git add . && git commit && git push`，让忽略规则真正起作用；上传前建议再执行一次：
  ```bash
  git status --ignored --short | findstr /i "jks keystore"
  ```

---

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

Copyright (c) 2026 紫空幻影
