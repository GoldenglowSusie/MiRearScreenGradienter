# 部署指南

本指南将帮助您配置 GitHub Actions 自动编译和发布 Android APK。

## 📋 目录

- [快速开始](#快速开始)
- [签名配置](#签名配置)
  - [创建签名密钥](#创建签名密钥)
  - [本地开发签名](#本地开发签名)
  - [GitHub Actions 签名](#github-actions-签名)
- [Gradle 镜像源配置](#gradle-镜像源配置)
- [自动编译配置](#自动编译配置)
- [发布版本](#发布版本)
- [故障排查](#故障排查)

---

## 快速开始

### 1. 基本配置（无需签名）

如果您不需要签名（使用 debug 签名），直接推送代码即可触发自动编译：

```bash
git push origin main
```

### 2. 使用签名发布（推荐）

如果需要使用自己的签名发布正式版本，请按照[签名配置](#签名配置)章节进行设置。

---

## 签名配置

### 创建签名密钥

#### 使用 keytool 创建密钥

```bash
keytool -genkey -v -keystore ~/upload-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload \
  -storepass <您的密钥库密码> \
  -keypass <您的密钥密码>
```

**重要提示：**
- 请妥善保管密钥文件（`.jks`）和密码
- 密钥文件丢失后无法恢复
- 生产环境请使用强密码

#### 密钥信息说明

创建密钥时需要填写以下信息：
- **密钥库密码（storePassword）**：密钥库的保护密码
- **密钥别名（keyAlias）**：默认为 `upload`，可自定义
- **密钥密码（keyPassword）**：密钥的保护密码（可与密钥库密码相同）
- **有效期（validity）**：建议设置为 10000 天（约 27 年）

---

### 本地开发签名

#### 1. 准备密钥文件

将生成的 `upload-keystore.jks` 文件放置到 `android/` 目录（**不要提交到 Git**）：

```
MiRearScreenGradienter/
├── android/
│   ├── upload-keystore.jks  ← 放置在这里
│   ├── key.properties       ← 稍后创建
│   └── app/
├── lib/
└── ...
```

#### 2. 创建 key.properties 文件

在 `android/` 目录下创建 `key.properties` 文件（**不要提交到 Git**）：

```properties
storeFile=upload-keystore.jks
storePassword=您的密钥库密码
keyAlias=upload
keyPassword=您的密钥密码
```

**文件路径：** `android/key.properties`

#### 3. 验证配置

运行以下命令验证签名配置：

```bash
flutter build apk --release
```

如果配置正确，APK 将使用您的签名密钥进行签名。

#### 4. 安全提示

确保以下文件已在 `.gitignore` 中（已默认配置）：

```
android/key.properties
android/**/*.keystore
android/**/*.jks
```

---

### GitHub Actions 签名

要在 GitHub Actions 中使用签名，需要配置 GitHub Secrets。

#### 1. 准备密钥文件

确保您已有 `upload-keystore.jks` 文件。

#### 2. 将密钥文件转换为 Base64

**Linux/macOS:**
```bash
base64 -i upload-keystore.jks | pbcopy  # macOS
# 或
base64 upload-keystore.jks              # Linux，然后手动复制输出
```

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks"))
```

#### 3. 配置 GitHub Secrets

进入仓库的 **Settings → Secrets and variables → Actions**，添加以下 Secrets：

| Secret 名称 | 说明 | 示例值 |
|------------|------|--------|
| `KEYSTORE_BASE64` | 密钥文件的 Base64 编码 | `UEsDBBQAAAAI...` （完整 Base64 字符串）|
| `KEYSTORE_PASSWORD` | 密钥库密码 | `your_store_password` |
| `KEY_ALIAS` | 密钥别名 | `upload` |
| `KEY_PASSWORD` | 密钥密码 | `your_key_password` |

**配置步骤：**
1. 点击 "New repository secret"
2. 输入 Secret 名称（如 `KEYSTORE_BASE64`）
3. 粘贴对应的值
4. 点击 "Add secret"
5. 重复以上步骤添加所有 4 个 Secrets

#### 4. 工作原理

GitHub Actions 工作流会自动：
1. 检查是否存在 `KEYSTORE_BASE64` Secret
2. 如果存在，解码并创建 `android/app/upload-keystore.jks`
3. 创建 `android/key.properties` 文件
4. 使用签名密钥编译 APK
5. 如果不存在，使用 debug 签名编译

---

## Gradle 镜像源配置

项目已配置使用**腾讯云镜像源**加速 Gradle 下载，保证国内连接性。

### 配置位置

`android/gradle/wrapper/gradle-wrapper.properties`

```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.12-all.zip
```

### 镜像源说明

- **腾讯云镜像**：国内访问速度快，适合国内开发者
- **自动回退**：如果镜像不可用，Gradle 会自动尝试官方源
- **多人协作**：配置在代码中，所有开发者自动使用相同镜像源

### 更换镜像源

如果需要使用其他镜像源，可以修改 `distributionUrl`：

- **阿里云镜像**：`https://mirrors.aliyun.com/macports/distfiles/gradle/gradle-8.12-all.zip`
- **官方源**：`https://services.gradle.org/distributions/gradle-8.12-all.zip`

---

## 自动编译配置

### 触发条件

工作流会在以下情况自动触发：

1. **代码推送**：推送到任意分支或标签
2. **手动触发**：在 GitHub Actions 页面点击 "Run workflow"

### 工作流程

1. **检出代码**
2. **设置环境**：JDK 17、Flutter 3.24.0
3. **配置签名**（如果存在）
4. **编译 APK**：Release 版本
5. **重命名文件**：根据分支/标签生成文件名
6. **上传 Artifact**：保存 30 天
7. **创建 Release**（仅限标签推送）

### 文件命名规则

- **Tag 推送**：`MRSG-V{版本号}-release.apk`
  - 例如：`v1.0.0` → `MRSG-V1.0.0-release.apk`
  - 例如：`V1.0.0` → `MRSG-V1.0.0-release.apk`
- **分支推送**：`MRSG-{分支名}-debug.apk`
  - 例如：`main` → `MRSG-main-debug.apk`

---

## 发布版本

### 1. 创建并推送标签

```bash
# 创建标签（推荐使用 v 前缀）
git tag v1.0.0

# 推送标签
git push origin v1.0.0
```

### 2. 等待编译完成

在 GitHub 仓库的 **Actions** 页面查看编译进度。

### 3. 下载 APK

编译完成后有两种方式获取 APK：

#### 方式一：从 Release 下载（推荐）

1. 进入 **Releases** 页面
2. 找到对应的 Release（如 `v1.0.0`）
3. 下载 `MRSG-V1.0.0-release.apk`

#### 方式二：从 Artifacts 下载

1. 进入 **Actions** 页面
2. 找到对应的编译任务
3. 在 "Artifacts" 部分下载 APK

---

## 故障排查

### 问题 1：编译失败，提示找不到签名配置

**原因**：`key.properties` 文件不存在或配置错误

**解决方法**：
- 检查 `android/key.properties` 文件是否存在
- 检查文件内容格式是否正确
- 检查密钥文件路径是否正确

### 问题 2：GitHub Actions 编译失败，提示签名错误

**原因**：GitHub Secrets 配置错误

**解决方法**：
1. 检查所有 4 个 Secrets 是否都已配置
2. 检查 `KEYSTORE_BASE64` 是否正确（完整 Base64 字符串）
3. 检查密码和别名是否与密钥文件匹配
4. 确认密钥文件 Base64 编码正确

### 问题 3：Gradle 下载缓慢或失败

**原因**：镜像源不可用或网络问题

**解决方法**：
1. 检查 `gradle-wrapper.properties` 中的镜像源 URL
2. 尝试切换到其他镜像源（阿里云、官方源）
3. 检查网络连接

### 问题 4：本地编译成功，GitHub Actions 失败

**可能原因**：
- Flutter 版本不一致
- 依赖缓存问题
- 签名配置不同

**解决方法**：
1. 检查工作流中的 Flutter 版本（当前：3.24.0）
2. 查看 Actions 日志了解具体错误
3. 清理 GitHub Actions 缓存后重试

### 问题 5：APK 文件名不符合预期

**原因**：标签命名格式问题

**解决方法**：
- 确保标签格式正确（推荐：`v1.0.0` 或 `V1.0.0`）
- 检查工作流日志中的文件名生成逻辑

---

## 最佳实践

### 1. 密钥管理

- ✅ **使用强密码**：密钥库和密钥密码都应使用强密码
- ✅ **备份密钥**：将密钥文件备份到安全位置
- ✅ **不要提交密钥**：确保密钥文件在 `.gitignore` 中
- ✅ **使用 GitHub Secrets**：不要在代码中硬编码密码

### 2. 版本管理

- ✅ **使用语义化版本**：遵循 `主版本.次版本.修订版本` 格式
- ✅ **标签命名规范**：使用 `v` 前缀（如 `v1.0.0`）
- ✅ **版本说明**：在 Release 中添加更新日志

### 3. 测试流程

- ✅ **本地测试**：在推送标签前先本地编译测试
- ✅ **分支测试**：使用分支推送测试编译流程
- ✅ **签名验证**：确认 APK 使用正确的签名

---

## 相关资源

- [Flutter 官方签名文档](https://docs.flutter.dev/deployment/android#signing-the-app)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Gradle 官方文档](https://docs.gradle.org/)
- [keytool 使用指南](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

---

## 支持

如有问题，请：
- 查看 [GitHub Issues](https://github.com/your-repo/issues)
- 联系项目维护者

---

**最后更新**：2025-11-21

