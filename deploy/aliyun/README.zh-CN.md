# Forge 汉化版阿里云镜像

本目录用于把 Forge 汉化版的安卓包、桌面包、资源包和中文卡图发布到阿里云 OSS，
再由阿里云 CDN 对中国内地用户分发。生产环境不需要 ECS。

## 一、域名与备案

1. 在阿里云购买一个可备案域名，并完成域名实名认证。
2. 在阿里云备案系统完成 ICP 备案。
3. 备案完成后准备两个子域名：
   - `update.example.com`：更新清单、APK、桌面包和 `assets.zip`。
   - `img.example.com`：中文卡图。

中国内地 CDN 必须使用已经备案的域名。备案完成前可用 OSS 外网域名做内部测试，
但不要把该地址发布给正式用户。

## 二、OSS

建议在同一中国内地地域创建两个私有 Bucket：

- `forge-cn-update-账号唯一后缀`
- `forge-cn-images-账号唯一后缀`

两个 Bucket 均应：

- 开启阻止公共访问，保持私有读写。
- 开启版本控制，以便恢复误覆盖的清单或图片。
- 设置生命周期规则，自动清理未完成的分片上传。
- 只给发布用 RAM 用户授予对应 Bucket 的上传和列举权限，不使用主账号 AccessKey。

## 三、CDN

分别创建两个 CDN 加速域名，并启用同账号私有 OSS 回源：

### update.example.com

- 业务类型：大文件下载。
- 强制 HTTPS。
- 开启 Range 回源。
- `manifest-v1.properties`：缓存 60 秒。
- `android/`、`desktop/`、`assets/`：缓存 365 天。
- 缓存键保留完整路径；发布文件必须带不可变版本目录。

### img.example.com

- 业务类型：图片小文件。
- 强制 HTTPS。
- `cards/`：缓存 365 天。
- 缓存不存在的响应不超过 5 分钟，避免新上传图片长期返回旧的 404。

原生客户端通常不发送可靠的 Referer，不建议使用 Referer 白名单。请改用费用告警、
带宽封顶、单 IP 频率限制和 CDN 日志监控防止盗刷。

## 四、程序配置

备案域名可用后，修改：

`forge-gui/src/main/resources/forge-update.properties`

```properties
update.baseUrl=https://update.example.com/forge/
images.baseUrl=https://img.example.com/cards/
```

桌面调试时也可以不修改文件，直接设置：

```powershell
$env:FORGE_UPDATE_URL = 'https://update.example.com/forge/'
$env:FORGE_IMAGE_URL = 'https://img.example.com/cards/'
```

## 五、发布更新

先安装并配置阿里云 `ossutil`，然后运行：

```powershell
.\deploy\aliyun\publish-release.ps1 `
  -Version '2.0.15-cn.1' `
  -Bucket 'forge-cn-update-账号唯一后缀' `
  -AndroidApk '.\forge-gui-android\target\forge-android-cn.apk' `
  -DesktopPackage '.\forge-gui-desktop\target\forge-desktop-cn.jar' `
  -AssetsZip '.\assets.zip'
```

脚本会计算文件大小和 SHA-256，先上传不可变文件，最后上传更新清单。只有最后一步成功后，
客户端才会看到新版本。

## 六、发布卡图

```powershell
.\deploy\aliyun\publish-card-images.ps1 `
  -Bucket 'forge-cn-images-账号唯一后缀' `
  -CardImageDirectory 'D:\卡图'
```

脚本不会删除 OSS 中已有对象。确认版本控制和备份策略前，不要使用 `ossutil sync --delete`。

### 独立发布安卓资源包

资源内容变化但不需要同时发布新 APK 时，可以只发布 `assets.zip`：

```powershell
.\deploy\aliyun\publish-assets.ps1 `
  -Version '2.0.15-cn.assets.1' `
  -Bucket 'forge-cn-update-账号唯一后缀' `
  -AssetsZip '.\forge-gui-android\target\assets.zip'
```

脚本会保留现有清单中的 APK 和桌面包字段，只更新 `assets.*` 字段、大小和 SHA-256。
安卓客户端允许清单中只包含资源包，因此资源更新不再要求同时发布或安装新 APK。

## 七、回滚

安装包路径包含版本号，不需要覆盖或删除旧包。回滚时从 OSS 版本记录恢复旧的
`forge/manifest-v1.properties`，或重新上传指向旧版本文件的清单，然后刷新该清单的 CDN 缓存。
