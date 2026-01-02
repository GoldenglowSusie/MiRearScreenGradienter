# MiRearScreenGradienter (背屏水平仪)

为小米17Pro/17 Pro Max等双屏设备打造的背屏水平仪工具。

## 📄 开源协议

本项目采用 **GPL-3.0** 开源协议。

**💬 交流与支持**
- QQ交流群：**932738927** - [加入群聊](https://tgwgroup.ltd/2025/10/21/%e5%85%b3%e4%ba%8emrss%e4%ba%a4%e6%b5%81%e7%be%a4/)
- 打赏支持：[请作者喝杯咖啡](https://tgwgroup.ltd/2025/10/19/%e5%85%b3%e4%ba%8e%e6%89%93%e8%b5%8f/) ☕

---

## ✨ 功能特性 (V1.0.0)

- 🎯 **背屏水平仪**: 在背屏实时显示水平仪，支持横滚(ROLL)和俯仰(PITCH)角度检测
- 📊 **雷达式界面**: 精美的同心圆雷达显示，0-90度刻度标注
- 💚 **智能指示**: 颜色编码气泡指示器（绿色=水平，黄色=轻微倾斜，红色=较大倾斜）
- 🎨 **精致UI**: 黑色背景+霓虹绿主题，Material 3设计
- 🚀 **无需ROOT**: 基于Shizuku实现，无需ROOT权限
- 💡 **背屏常亮**: 使用keycode wakeup保持背屏常亮
- 📱 **实时响应**: 120Hz高刷新率传感器采样，低延迟显示
- 🔄 **智能平滑**: 低通滤波器平滑传感器数据，避免数值跳动
- 🎯 **智能贴靠**: 89.1°自动贴靠至90°，便于精确调平

## 📋 使用前提

1. **设备要求**: 支持背屏的小米手机（小米17Pro/17 Pro Max等双屏设备）
2. **Shizuku**: 需要安装并启动Shizuku
   - 下载地址: [Shizuku官网](https://shizuku.rikka.app/)
   - 启动方式: ADB或无线调试

## 🚀 使用方法

### 1. 初次设置

1. 安装MiRearScreenGradienter应用
2. 确保Shizuku已启动
3. 打开应用，授予Shizuku权限
4. 点击"启动水平仪"按钮

### 2. 日常使用

**启动水平仪：**
1. 打开应用
2. 点击"启动水平仪"按钮
3. 翻转手机至背屏
4. 背屏显示实时水平仪界面

**停止水平仪：**
1. 返回主屏
2. 在应用中点击"停止水平仪"按钮
3. 背屏自动关闭

**💡 提示**: 
- 水平仪运行时背屏会保持常亮
- 放置手机在水平面上，气泡会显示为绿色
- ROLL和PITCH数值会实时更新
- 接近90度时会自动贴靠至90.0°以便读数

## 🔧 技术实现

- **Flutter**: 跨平台UI框架，Material 3设计
- **Shizuku**: 提供shell权限执行特权操作
- **sensors_plus**: 高频率传感器数据采集 (120Hz)
- **Foreground Service**: 前台服务保活
- **Keycode Wakeup**: 使用`input keyevent KEYCODE_WAKEUP`保持背屏常亮
- **Low-pass Filter**: 低通滤波器 (α=0.1) 平滑传感器数据
- **Custom Canvas Rendering**: 自定义Canvas绘制雷达式水平仪界面
- **3-Axis Accelerometer**: 利用X/Y/Z三轴加速度计精确计算倾斜角度

## 📝 权限说明

- `moe.shizuku.manager.permission.API_V23`: Shizuku API权限，用于执行特权操作
- `android.permission.WAKE_LOCK`: 保持背屏常亮
- `android.permission.FOREGROUND_SERVICE`: 前台服务权限
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`: 特殊用途前台服务

## 🛠️ 开发构建

```bash
# 安装依赖
flutter pub get

# 构建Debug APK
flutter build apk --debug

# 构建Release APK (arm64-v8a)
flutter build apk --release --split-per-abi --target-platform android-arm64
```

生成的APK位于: `build/app/outputs/flutter-apk/app-arm64-v8a-release.apk`

## 🔍 技术细节

### 核心功能

1. **传感器数据采集** 📊
   - 使用sensors_plus获取加速度计数据
   - 采样间隔: 5ms (理论200Hz，实际约120Hz)
   - 低通滤波器平滑数据 (α=0.1)
   - Z轴修正确保角度准确到90°

2. **角度计算** 📐
   - ROLL (横滚): `atan2(y, sqrt(x² + z²))`
   - PITCH (俯仰): `atan2(-x, sqrt(y² + z²))`
   - 智能贴靠: 接近89.1°时自动显示90.0°

3. **雷达式UI** 🎨
   - 同心圆显示0°/30°/60°/90°刻度
   - 十字+对角线网格
   - 气泡位置实时反映倾斜角度和方向
   - 颜色编码: 绿色(<3°) / 黄色(3-10°) / 红色(>10°)

4. **背屏保活** 💡
   - Shizuku TaskService持续发送KEYCODE_WAKEUP
   - 100ms间隔防止背屏息屏
   - 停止时强制终止Service进程

## 📄 许可证

GPL-3.0 License - 详见 [LICENSE](LICENSE) 文件

---

## 📝 更新日志

### V1.0.0 (2025-11-21)

#### 首次发布
- 实现背屏水平仪核心功能
- 雷达式UI设计
- 智能角度计算和平滑
- Shizuku集成和背屏保活
- 精美渐变主界面

## 👥 团队

### 作者
**AntiOblivionis**
- 🎮 QQ: 319641317
- 📱 酷安: [@AntiOblivionis](http://www.coolapk.com/u/8158212)
- 🐙 Github: [GoldenglowSusie](https://github.com/GoldenglowSusie/)
- 📺 Bilibili: [罗德岛T0驭械术师澄闪](https://space.bilibili.com/407059627)

## 🤖 AI协作开发

本项目由作者与以下AI助手共同开发：
- Cursor
- Gemini-3-Pro

## 🙏 致谢

- [Shizuku](https://github.com/RikkaApps/Shizuku) - 提供特权API支持
- Flutter团队 - 优秀的跨平台框架
- Xiaomi HyperOS 小米澎湃OS团队 - 小米手机背屏功能

---

## 📜 免责声明

本应用为开源项目，基于Shizuku实现背屏功能扩展，仅供学习交流使用。使用本应用即表示您理解并同意：
- 本应用非小米官方应用，与小米公司无任何关联
- 使用本应用的风险由用户自行承担
- 开发者不对使用本应用造成的任何损失负责

---
