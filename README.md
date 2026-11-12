<div align="center">

<img src="assets/icon.png" alt="澎湃音乐图标" width="96">  


# [澎湃音乐](https://github.com/misaka10902/HyperMusic)

#### 这是你想象中的小米音乐吗？

![Android](https://img.shields.io/badge/Android-13%2B-3DDC84?labelColor=555&logo=android) ![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue) ![Miuix](https://img.shields.io/badge/Miuix-Glass-blue?labelColor=555) ![Media3](https://img.shields.io/badge/Media3-1.10.1-4F6BED?style=flat)

</div>

---

## 项目简介

澎湃音乐是一款基于 Jetpack Compose、Compose Miuix 和 AndroidX Media3 构建的 Android 在线音乐播放器，通过 GD API 获取在线音源，界面设计和交互全面契合小米澎湃OS，为小米用户带来更一致的听歌体验。

## ⚠⚠注意事项与免责声明⚠⚠

- 为保证本项目的存续，**本项目谢绝任何形式的公开宣传**，开发者也不会在公共社交平台做任何宣传。请不要在微博、酷安、小红书等公众平台引流、推荐本项目（允许在QQ、微信、电报等IM平台小范围传播），如开发者在境内平台发现有传播本项目者，将立即停止更新并删除本仓库。
- 本项目严禁任何商业用途，如您在闲鱼等平台发现有商家兜售本项目，请立即向平台举报；付费使用该项目之副本产生的版权风险由使用者自行承担。
- 本项目唯一发布渠道为 GitHub，目前无任何社交账号、群组等联系方式，请谨防上当受骗！
- 本项目不存储任何音频资源，接口均来自互联网公开资料，仅供技术交流，请下载后24小时内删除。
- 欢迎前往B站关注[GD Studio](https://space.bilibili.com/13715770)，没有他就没有多姿多彩的 HyperMusic。

## 软件界面预览

### 主要页面

<table>  
  <tr>  
    <td><img src="./assets/1.png" width="200" alt="01"></td>  
    <td><img src="./assets/2.png" width="200" alt="02"></td>  
    <td><img src="./assets/3.png" width="200" alt="03"></td>  
    <td><img src="./assets/4.png" width="200" alt="04"></td>  
  </tr>  
  <tr>  
    <td><img src="./assets/5.png" width="200" alt="05"></td>  
    <td><img src="./assets/6.png" width="200" alt="06"></td>  
    <td><img src="./assets/7.png" width="200" alt="07"></td>  
    <td><img src="./assets/8.png" width="200" alt="08"></td>  
  </tr>  
  <tr>  
    <td><img src="./assets/9.png" width="200" alt="09"></td>  
    <td><img src="./assets/10.png" width="200" alt="10"></td>  
    <td><img src="./assets/11.png" width="200" alt="11"></td>  
    <td><img src="./assets/12.png" width="200" alt="12"></td>  
  </tr>  
</table>

### 播放页与歌词

<table>  
  <tr>  
    <td><img src="./assets/13.png" width="200" alt="01"></td>  
    <td><img src="./assets/14.png" width="200" alt="02"></td>  
    <td><img src="./assets/15.png" width="200" alt="03"></td>  
    <td><img src="./assets/16.png" width="200" alt="04"></td>  
  </tr>  
</table>

<table>  
  <tr>  
    <td><img src="./assets/17.jpg" width="400" alt="05"></td>  
    <td><img src="./assets/18.jpg" width="400" alt="06"></td>  
  </tr>  
</table>

## 特性

- 基于 GD Studio API，实现歌曲在线播放与下载、专辑与歌手详情查看等功能
- 基于 Compose Miuix 测试版本（miuix-glass）打造整体界面与控件，视觉效果与小米澎湃OS4协调一致
- 极致轻巧，安装包仅4MB出头，麻雀虽小五脏俱全
- 处处可见、称心如意、简单而不失优雅的细节动效
- 支持历史记录查询、本地歌曲播放、在线歌单导入、歌曲收藏等基础功能
- 首页歌曲推荐、背景自定义、音频淡入淡出、播放界面自定义、睡眠定时等特色功能

## TODO

- **播放界面展开动效**
- **播放界面歌词模糊**
- **音频焦点功能**<br>以上三项在测试阶段曾有多次调试，但效果不佳。
- **对折叠屏、平板等大屏设备的界面适配**
- **音乐锁屏功能**<br>待小米官方推送该功能后适配。

## NOT TODO

- **Monet 取色、Monochrome 图标、预测返回**等原生特性
- **液态玻璃效果**<br>由于加入以上效果会破坏应用UI/UX体验上与小米澎湃OS的一致性、在一定程度上影响较低端手机的性能，故决定不做，在此表示抱歉。本仓库亦**不接受**相关 request，如有需求，请自行 fork 该仓库。
- **歌曲评论读取功能**<br>技术上可以实现，但本项目的宗旨之一是“以本地播放器的思维做在线音乐，精简为美”，故暂决定不做评论相关功能。如果需求较多，可考虑添加。
- **TME系（QQ/酷狗/酷我/JOOX）音源接入**<br>吃DMCA概率极高，故不添加。

## 运行要求

- 基于 `miuix-glass` 的运行要求以及安卓生态现状，本应用支持 Android 13（API 33）或更高版本的设备运行，在此向较低版本设备用户致歉
- 首次运行需授予通知和“音乐和音频”读取权限
- 本应用不保证在非小米澎湃OS设备上的运行效果，但理论上在其他安卓系统上的体验也较为完善（曾进行定向测试，效果较好）
- 当前 APK 仅提供 `arm64-v8a`

## 与澎湃OS的深度融合

本项目可通过逆向修改部分文件的手段“替代”小米音乐，替换成功后，你将获得一些基于白名单的特性适配：

- 支持流转到平板、电脑等设备，在电脑端可使用大屏布局
- 小米超级岛支持下拉展开小窗/上滑小窗恢复岛形态
- 超级小爱输入法适配圆角界面
- 后台优化策略
- Xiaomi Pro 系列（17之后）在背屏可控制歌曲播放
- Xiaomi Flip 设备可在外屏使用本应用

但同时，由于一些限制与不完善之处，应用的刷新率将被锁定（可通过修改系统设置恢复），小爱播放音乐、视频工具箱 - 听歌识曲、超级岛分享等功能也无法实现，正在探索中。

由于安全问题，本项目不公开修改方法，**感兴趣的可以询问大模型**。

## 自行构建

```bash
# Release
./gradlew :app:assembleRelease

# Debug
./gradlew :app:assembleDebug
```

产物路径：

```
app/build/outputs/apk/release/app-release.apk
```

## 下载与安装

[GitHub Releases](https://github.com/misaka10902/HyperMusic/releases)

## 关于 Hyperify Project

## 致谢

- [GD Studio](https://music.gdstudio.org/) - 音源服务提供
- [Otter Music](https://github.com/DJChanahCJD/otter-music) - 提供API接入思路
- [Compose Miuix](https://github.com/compose-miuix-ui/miuix) - 界面基础框架
- [AndroidX Media3](https://developer.android.com/media/media3) - 音频播放基础框架
- [Nexio Scheduler](https://github.com/HaoZai000/NexioSchedule) - 项目构建早期参考
- [Melox](https://github.com/Inefy-03/Melox) - UX细节方案提供

----

本项目由 WorkBuddy 协助开发，代码多有错漏之处，敬请见谅。

作为 Hyperify Project 的首个项目，本项目将投入更多精力维护，欢迎通过 [Issues](https://github.com/misaka10902/HyperMusic/issues) 反馈问题或提交 [Pull Request](https://github.com/misaka10902/HyperMusic/pulls)。
