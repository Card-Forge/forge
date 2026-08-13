# Forge 简体中文民间版开发与发布手册

本文记录 `kaorou-bot/forge` 的 `zh-cn-community-release` 分支在 Windows 上维护、构建、测试和发布到阿里云的实际经验。目标是让后续维护者能够复现当前版本，并避免已经发生过的更新循环、黑屏、中文缺字、输入法失效和 Windows 启动器失配等问题。

## 1. 项目边界与仓库

- 上游仓库：`https://github.com/Card-Forge/forge.git`
- 民间版仓库：`https://github.com/kaorou-bot/forge.git`
- 长期维护分支：`zh-cn-community-release`
- 用户支持：QQ群 `813597628`
- 更新域名：`https://update.mtg-forge-kaorou.vip/forge/`
- 卡图域名：`https://images.mtg-forge-kaorou.vip/cards/`
- OSS 更新 Bucket：`forge-cn-update-a8k3`
- OSS 卡图 Bucket：`forge-cn-images-a8k3`

本版是完全免费的民间汉化，不代表 Card Forge 或 Wizards of the Coast 官方。发布说明和程序内联系方式必须保持这一表述。

## 2. 当前民间版特性与主要代码位置

| 特性 | 主要位置 |
| --- | --- |
| 默认简体中文 | `forge-gui/src/main/java/forge/localinstance/properties/ForgePreferences.java`、`forge-gui/forge.profile.properties.example` |
| 中文界面与卡名 | `forge-gui/res/languages/zh-CN.properties`、`cardnames-zh-CN.txt` |
| 民间版身份、QQ群与发布说明 | `CommunityEditionInfo.java`、`forge-community-release-notes-zh-CN.txt` |
| 独立更新和卡图线路 | `forge-update.properties`、`ForgeUpdateConfig.java`、`ImageFetcher.java`、`LibGDXImageFetcher.java` |
| 单一更新渠道 | `AutoUpdater.java`、`AssetsDownloader.java` |
| 安卓内置 CJK 字体 | `forge-gui-android/assets/bundled-font/`、`AssetsDownloader.installBundledCjkFont()` |
| 安卓动态 CJK 字形 | `forge-gui-mobile/src/forge/assets/FSkinFont.java` |
| 安卓原生中文输入框 | `FTextField.startNativeAndroidEdit()`、`DefaultAndroidInput.getTextInput()` |
| 禁止上游 Sentry 上报 | `forge-gui/src/main/java/forge/gui/error/BugReporter.java` |
| 阿里云发布 | `deploy/aliyun/*.ps1` |
| 字库覆盖检查 | `deploy/VerifyCjkFontCoverage.java` |

## 3. 开发环境

已经验证的环境：Windows 11、JDK 21、Maven 3.9.12、Android Build Tools 36.0.0、最低 Android SDK 26、7-Zip 和 ossutil 2.3.0。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:ANDROID_HOME = 'C:\Users\Administrator\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_PREFS_ROOT = 'C:\Users\Administrator\.android'
$env:MAVEN_OPTS = '-Xmx4g -Dfile.encoding=UTF-8'
```

AccessKey 只应存在于用户目录的 ossutil 配置中；不要写入仓库、文档、脚本参数或终端日志。Android 当前发布包使用本机 `C:\Users\Administrator\.android\debug.keystore`。同一应用要覆盖安装，后续必须继续使用同一签名；准备面向更大范围长期发布前，应迁移到妥善备份的正式密钥，但改变签名会使已有用户无法直接覆盖安装。

## 4. 上游同步方法

不要直接在 `master` 上开发。同步前保证工作区干净并留存当前可发布版本标签。

```powershell
git switch zh-cn-community-release
git fetch upstream
git fetch origin
git merge upstream/master
```

处理冲突时优先检查：

1. `pom.xml` 中的版本属性；
2. `ForgePreferences` 默认语言和更新渠道；
3. `AssetsDownloader` 的镜像更新、字体安装和资源包逻辑；
4. `FTextField` 与 Android 后端输入法代码；
5. `ImageFetcher` 的民间卡图 URL；
6. 上游新增加的 Discord、论坛、Sentry 或下载全部卡图入口；
7. 上游新增英文文案是否需要补充到 `zh-CN.properties`。

合并后至少编译桌面模块和移动模块，并重新执行字库覆盖检查。上游可能改变 libGDX、Android 插件或输入系统，不能只凭“合并无冲突”判断可发布。

## 5. 版本体系：最容易出错的部分

项目同时存在三种版本标识：

| 属性 | 示例 | 用途 |
| --- | --- | --- |
| `snapshotName` / Maven `revision` | `-cn0813r2` / `2.0.15-cn0813r2` | Maven 产物名、Android `versionName` |
| `displayVersion` | `2.0.15-汉化-08.13.2` | 面向用户的显示版本、桌面 JAR Implementation-Version |
| `androidVersionCode` | `2026081303` | Android 覆盖安装所需的单调递增整数 |

修改位置在根目录 `pom.xml`。每次发布必须同时更新三者，其中 `androidVersionCode` 只能增加，不能回退或复用。

### Android 无限更新的根因

Android 的 `AssetsDownloader` 使用已安装 APK 的 `versionName` 比较 `android.version`。APK 当前值是 ASCII 的 `2.0.15-cn0813r2`。若清单写成中文显示版本 `2.0.15-汉化-08.13.2`，两者永不相等，用户安装完仍会反复提示更新。

规则：

- `manifest version` 与 `android.version` 必须等于 APK 的实际 `versionName`；
- `desktop.version` 应等于桌面 JAR 的 `Implementation-Version`，可以使用中文显示版本；
- 发布后必须用 `aapt dump badging` 和公网清单做精确相等校验。

`publish-clients.ps1` 的 `ManifestVersion` 参数表示 Android 的精确 ASCII 版本；如桌面显示版本不同，使用 `-DesktopVersion` 单独传入。

## 6. 中文资源维护

### 6.1 卡名映射

正式文件为 `forge-gui/res/languages/cardnames-zh-CN.txt`。外部映射文件更新时：

1. 保留 UTF-8 编码和 `英文名|中文名|类别|规则` 的四列结构；
2. 规则文本自身可能含竖线，不能无条件拆分所有 `|` 后截断规则；
3. 检查重复英文键、空英文键、异常行数和乱码；
4. 双面牌、特殊画框和同名不同版本不应仅靠中文文件名判断卡图；
5. 构建后确认 APK 内存在 `assets/localization/cardnames-zh-CN.txt`。

### 6.2 properties 文件

中文 properties 文件必须保持 UTF-8。占位符数量必须与英文项一致，例如 `{0}`、`{1}` 不可丢失或调换。添加新键时同时修改 `en-US.properties` 和 `zh-CN.properties`，否则回退语言或本地化检查可能失败。

### 6.3 CJK 字体

字体和许可证位于 `forge-gui-android/assets/bundled-font/`。不要根据当前文案预生成有限字符集的位图字体。卡名映射和用户输入会持续增加字符，有限字库必然再次出现方框。当前方案用 FreeType 增量生成字形，并为新方案设置缓存版本；修改字体或生成算法时必须让旧缓存失效。

发布前运行：

```powershell
& "$env:JAVA_HOME\bin\javac.exe" deploy\VerifyCjkFontCoverage.java
& "$env:JAVA_HOME\bin\java.exe" -cp deploy VerifyCjkFontCoverage
```

审计应覆盖中文界面、卡名、发布说明和必要配置文本。补充平面字符无法由 libGDX 的 UTF-16 `char` 字体接口可靠呈现，审计工具会把这类字符一并报告。

### 6.4 首次启动资源

Android 在完整资源包下载和解压之前就要显示启动界面，因此 APK 必须自带：

- `fallback_skin` 的启动背景和字体；
- libGDX `lsans-15.fnt/png`；
- `libgdx-freetype.so` 等四种 ABI 原生库；
- CJK TTF 和许可证；
- tinylog/SLF4J 的 `META-INF/services`。

缺少这些内容可能表现为授权后黑屏、首启崩溃或中文回退为方框，而不是明显的“文件不存在”提示。

## 7. Android 中文输入经验

libGDX 的 GL Surface 文本框不能稳定接收 Android IME 的拼音组合与候选词提交。只处理 `keyTyped`、`commitText` 或单个搜索框都不足以覆盖不同输入法和所有界面。

当前策略：Android 上所有 `FTextField` 单行输入统一调用系统原生 `EditText` 对话框；完成后一次性把字符串提交回 Forge。桌面和其他平台保留原输入路径。

修改时必须人工覆盖卡牌搜索、牌组名称、数字输入、取消恢复、确认回调和再次打开时的选择状态。仅在模拟器看到键盘弹出不等于中文输入成功；必须用实体设备和主流中文输入法选词后确认文本真正进入 Forge。

## 8. 构建流程

### 8.1 快速编译验证

```powershell
.\.tools\apache-maven-3.9.12\bin\mvn.cmd `
  '-Dmaven.repo.local=C:\Users\Administrator\.m2\repository' `
  -DskipTests -pl forge-gui-mobile -am package

.\.tools\apache-maven-3.9.12\bin\mvn.cmd `
  '-Dmaven.repo.local=C:\Users\Administrator\.m2\repository' `
  '-Dlaunch4j.skip=true' -DskipTests -pl forge-gui-desktop -am package
```

### 8.2 Windows 中文路径问题

Launch4j 的 `windres.exe` 在包含中文的工作区路径下可能失败。推荐在纯 ASCII 路径的工作树中正式构建；也可跳过 Launch4j，复用已验证的无代码启动器外壳，但必须读取 EXE 内嵌字符串，确保包内 JAR 文件名完全一致。

已经发生过的故障：复用的 `forge.exe` 内写死 `forge-gui-desktop-2.0.15-cn0812-jar-with-dependencies.jar`，包内却命名为 `cn0813`，双击后没有任何提示。发布前必须执行：

```powershell
rg -a -o 'forge-gui-desktop-[0-9A-Za-z._-]+jar' .\forge.exe
```

结果必须与目录中的 JAR 文件名逐字一致。随后实际启动并确认内置 `runtime/bin/javaw.exe` 子进程存在。EXE 外壳自行退出而 `javaw.exe` 继续运行属于正常行为。

Windows 完整包应包含 `forge.exe`、匹配的主 JAR、`runtime`、`res`、配置示例、使用说明和更新说明。用户必须完整解压，不能在 ZIP 内运行。

### 8.3 Android D8 命令过长

老 `android-maven-plugin` 在 Windows 上把全部 classpath 展开成一个 D8 命令，容易超过系统命令行长度。常规 Maven 流程通常能完成 Java 编译、资源打包和 ProGuard，然后在 D8 阶段失败。

解决方案：将仓库临时映射到短盘符，把 D8 参数逐行写入 `@args` 文件，再调用 Build Tools 的 `d8.bat @args`；随后把 dex、四 ABI 原生库、字体、fallback 资源和服务描述符加入 `.ap_`，最后用 `zipalign` 与 `apksigner` 完成对齐、签名和验证。

参数文件中包含版本化模块 JAR 路径。版本更新后若复用旧 args 文件，必须替换所有旧 `cn...` 版本，否则会把旧代码打进新 APK。

APK 最终检查至少包括：递增的 `versionCode`、正确 `versionName`、dex、中文映射、镜像配置、发布说明、CJK 字体、四 ABI FreeType 库、日志服务以及与上一正式版一致的签名。

## 9. 阿里云发布

详细基础设施配置见 `deploy/aliyun/README.zh-CN.md`。生产发布遵循“不可变文件先上传，清单最后上传”，客户端不能看到指向尚未上传完成文件的清单。

```powershell
.\deploy\aliyun\publish-clients.ps1 `
  -ManifestVersion '2.0.15-cn0813r2' `
  -DesktopVersion '2.0.15-汉化-08.13.2' `
  -ObjectVersion '2.0.15-cn-08.13.2' `
  -Bucket 'forge-cn-update-a8k3' `
  -AndroidApk '.\Forge-Android.apk' `
  -DesktopPackage '.\Forge-Windows.zip' `
  -Ossutil '.\.tools\ossutil\ossutil-2.3.0-windows-amd64\ossutil.exe'
```

规则：

- 安装包和资源包使用带版本号的不可变路径，清单短缓存并最后更新；
- 同一路径上传过错误文件后不要覆盖并期待 CDN 立刻变化，应换新对象版本，如 `desktopfix1`；
- 发布脚本计算大小和 SHA-256，并保留未变化的 `assets.*` 字段；
- 不要使用 `sync --delete` 发布卡图；
- 资源包和卡图分别使用 `publish-assets.ps1`、`publish-card-images.ps1`。

### 发布后公网验收

不能只看 ossutil 上传成功。必须从 CDN 回读并确认：清单无 UTF-8 BOM；三个对象返回 `200`；`Content-Length`、类型和 SHA 正确；APK `versionName == android.version`；桌面 JAR `Implementation-Version == desktop.version`；新装、覆盖安装和完全重启后均不重复提示同版本更新。

OSS Bucket 私有时，RAM 发布账号可能有上传权限却没有读取对象 ACL 的权限，`ossutil stat` 因 `?acl` 返回 403 不代表对象不存在。可用 `ossutil cp` 回读清单，再通过 CDN HEAD/GET 验证对象。

### 回滚

不要删除已发布的版本对象。回滚只需让 `manifest-v1.properties` 重新指向已验证的旧对象，再刷新或等待清单短缓存。OSS 版本控制应保持开启。

## 10. 发布检查清单

- [ ] Git 工作区干净，`git diff --check` 通过；
- [ ] 三套版本均更新，Android code 单调增加；
- [ ] 中文占位符和 UTF-8 正常；
- [ ] 卡名文件无重复键、空键和规则截断；
- [ ] CJK 覆盖审计为 0；
- [ ] Maven mobile/desktop 编译通过；
- [ ] Android 实体设备完成中文选词和提交；
- [ ] Android 首次授权、资源下载、第二次启动正常；
- [ ] APK 关键资源、签名和 badging 校验通过；
- [ ] Windows EXE 内嵌 JAR 名与实际文件一致；
- [ ] Windows 使用内置 runtime 启动；
- [ ] 安装包和说明明确完全免费、民间非官方、QQ群；
- [ ] 上传不可变对象后最后更新清单；
- [ ] CDN 公网大小、SHA、版本与启动更新行为全部验收。

## 11. 磁盘清理

构建产物不应长期散落在各模块 `target`。确认正式包已上传并另行归档后使用：

```powershell
.\deploy\cleanup-development-artifacts.ps1
.\deploy\cleanup-development-artifacts.ps1 -Execute
```

第一次只预览。`-Execute` 仅删除当前仓库内名为 `target` 的目录，不删除源码、`.git`、`.tools`、Maven 仓库、Android SDK、ossutil 配置或阿里云对象。正式发布的权威副本是 OSS 中带版本号的不可变对象。

## 12. 关键教训

1. APK 能安装不代表首启资源完整；黑屏常来自字体、皮肤或原生库缺失。
2. 键盘能弹出不代表中文能提交；必须实际选择候选词。
3. 版本代表同一版不代表字符串相等；Android 必须使用 APK 精确 `versionName`。
4. EXE 存在不代表能找到主 JAR；Launch4j 的 JAR 名可能写死。
5. OSS 上传成功不代表 CDN 用户拿到新文件；错误路径应换版本，清单必须最后切换。
6. 修一个缺字不是字库方案；应审计全部本地化数据并使用增量字体。
7. PowerShell 默认 BOM 会破坏 Java Properties 的第一个键；清单必须为 UTF-8 无 BOM。
8. 所有清理应限定在已解析的仓库路径内，并默认 dry-run。

