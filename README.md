# VensIDE

一款轻量级的 Android 代码编辑器，专为移动端开发设计。

## 功能特性

### 📁 文件管理
- **多工作区管理**：创建和管理多个工作区，每个工作区对应一个目录
- **文件树视图**：左侧资源管理器，支持文件夹展开/折叠
- **文件操作**：
  - 新建文件/文件夹
  - 长按文件/文件夹进行重命名和删除
  - 支持在任意子文件夹中创建新内容

### ✏️ 代码编辑
- **编辑器功能**：
  - 语法高亮显示（基础）
  - 行号显示（左侧）
  - 等宽字体支持
  - 支持大文件编辑

### 🎨 界面设计
- **深色主题**：符合开发者习惯的深色配色方案
- **简洁布局**：侧边栏 + 编辑器的经典布局
- **响应式设计**：适配不同屏幕尺寸

### 🔧 技术栈
- **语言**：Kotlin
- **原生库**：C++ (JNI)
- **最低 SDK**：API 21 (Android 5.0)
- **目标 SDK**：API 34 (Android 14)

## 构建说明

### 环境要求
- Android SDK (build-tools 34.0.0)
- Android NDK
- Kotlin 编译器
- make
- aapt2 / aapt
- apksigner

### 构建步骤

```bash
# 1. 清理旧构建
make clean

# 2. 构建完整 APK
make

# 3. 安装到设备
make install

# 或手动安装
adb install -r build/apk/VensIDE-1.0.0.apk
```

### 可用命令

```bash
make help              # 显示帮助信息
make create-keystore   # 创建签名密钥库
make all              # 构建完整 APK（默认）
make native           # 编译 C++ 原生库
make kotlin           # 编译 Kotlin 代码
make dex              # 转换为 DEX 文件
make apk              # 打包并签名 APK
make clean            # 清理构建文件
make install          # 安装 APK 到设备
```

## 项目结构

```
VensIDE/
├── src/
│   ├── cpp/              # C++ 原生代码
│   │   ├── native.cpp
│   │   └── include/
│   └── kotlin/           # Kotlin 源代码
│       ├── MainActivity.kt
│       ├── LoadingActivity.kt
│       ├── WorkspaceActivity.kt
│       └── WorkspaceManagementActivity.kt
├── res/                  # 资源文件
│   ├── layout/           # 布局文件
│   ├── drawable/         # 图片资源
│   └── values/           # 值资源
├── build/                # 构建输出目录
├── Makefile              # 构建脚本
├── AndroidManifest.xml   # 应用清单
└── venside-release.jks   # 签名密钥库
```

## 使用说明

### 创建工作区
1. 打开应用，点击"创建新工作区"
2. 输入工作区名称和路径
3. 点击确认创建

### 打开文件
1. 在文件树中点击文件即可打开
2. 支持编辑和保存内容

### 管理文件/文件夹
1. **新建**：点击侧边栏右上角的新建按钮
2. **重命名/删除**：长按文件树中的项目，选择对应操作
3. **文件夹操作**：点击文件夹可展开/折叠

## 版本历史

### v1.0.0 (2026-03-03)
- ✨ 初始版本发布
- 📁 多工作区管理功能
- 🌲 文件树视图
- ✏️ 代码编辑器（带行号）
- 🔧 文件操作（新建、重命名、删除）
- 🎨 深色主题界面

## 开发计划

- [ ] 语法高亮增强
- [ ] 代码自动补全
- [ ] Git 集成
- [ ] 搜索和替换功能
- [ ] 多标签编辑
- [ ] 终端集成

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

本项目采用 MIT 许可证。

## 联系方式

- GitHub: https://github.com/XianTingXinBu1/VensIDE

---

**VensIDE** - 让移动开发更简单 🚀