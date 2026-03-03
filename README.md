# VensIDE - 项目

VensIDE 是一个在 Android 设备上运行的集成开发环境，支持代码编辑、编译和调试功能。

**版本**: 1.0.0
**更新日期**: 2026-03-03
**开发环境**: Termux + Kotlin + NDK

## 项目结构

```
VensIDE/
├── AndroidManifest.xml    # 清单文件
├── Makefile              # 构建脚本
├── README.md             # 项目说明
├── src/
│   ├── kotlin/           # Kotlin 源代码
│   │   └── MainActivity.kt
│   └── cpp/              # C++ 源代码（可选）
│       ├── native.cpp
│       └── include/
│           └── native.h
├── res/                  # 资源文件
│   ├── drawable/         # 图标
│   └── values/           # 字符串、样式
└── build/                # 构建输出（自动生成）
```

## 功能特性

- 📝 代码编辑器
- 🔄 文件管理
- 🏗️ 项目构建
- 📦 APK 打包
- 🔧 终端集成
- 🎨 主题支持

## 前置条件

```bash
# 安装开发环境
cd ~/工作区/VensIDE/原生
bash install_all.sh

# 配置环境
source ~/工作区/VensIDE/原生/setup_env.sh
```

## 构建

```bash
cd ~/工作区/VensIDE/原生/projects/VensIDE

# 完整构建
make

# 分步构建
make native    # 编译 C++
make kotlin    # 编译 Kotlin
make dex       # 转换 DEX
make apk       # 签名 APK

# 其他命令
make clean     # 清理
make help      # 帮助
make install   # 安装到设备
```

## 安装 APK

```bash
adb devices
adb install build/apk/VensIDE-1.0.0.apk
```

## 开发指南

### 添加新功能

1. 在 `src/kotlin/` 中添加 Kotlin 代码
2. 在 `src/cpp/` 中添加 C++ 代码（如需要）
3. 在 `res/` 中添加资源文件
4. 运行 `make` 构建

### 修改应用配置

编辑 `Makefile` 中的以下变量：
- `APP_ID` - 应用包名
- `APP_NAME` - 应用名称
- `APP_VERSION` - 应用版本

## 技术栈

- **语言**: Kotlin, C++
- **SDK**: Android API 34
- **NDK**: Termux clang (aarch64)
- **构建**: Makefile
- **最低支持**: Android 5.0 (API 21)

## 许可证

本项目为 VensIDE 项目的核心组件。

## 参考资料

- [Android 官方文档](https://developer.android.com/)
- [Kotlin 文档](https://kotlinlang.org/docs/)
- [Termux Wiki](https://wiki.termux.com/)