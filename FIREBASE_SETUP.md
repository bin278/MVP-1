# Firebase 数据库配置指南

## 问题：无法发布信息到 Firebase

### 可能的原因

1. **Firebase 数据库规则未配置写入权限**
2. **网络连接问题**
3. **Firebase 项目未启用 Realtime Database**

---

## 解决方案

### 1. 检查 Firebase 控制台

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 选择您的项目：`keshe-306fe`
3. 进入 **Realtime Database** 页面

### 2. 启用 Realtime Database

如果看到 "创建数据库" 按钮：
1. 点击 "创建数据库"
2. 选择位置（推荐选择 asia-east1 或 asia-southeast1）
3. 选择 **测试模式**（方便测试）
4. 点击 "启用"

### 3. 配置数据库规则

在 Firebase Console 的 Realtime Database 页面：
1. 点击 **规则** 标签
2. 将规则修改为以下内容：

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

**注意**：这是开发/测试模式，允许任何人读写。生产环境需要更严格的规则。

### 4. 生产环境规则（推荐）

如果需要更安全的规则，使用以下配置：

```json
{
  "rules": {
    "items": {
      ".read": true,
      ".write": "auth != null",
      "$itemId": {
        ".write": "auth != null && (data.child('publisherId').val() === auth.uid || !data.exists())"
      }
    },
    "users": {
      ".read": true,
      ".write": "auth != null",
      "$userId": {
        ".write": "auth != null && ($userId === auth.uid || !data.exists())"
      }
    },
    "favorites": {
      ".read": true,
      ".write": "auth != null"
    }
  }
}
```

---

## 验证步骤

### 1. 查看应用日志

在 Android Studio 的 Logcat 中，过滤以下标签：
- `FirebaseHelper`
- `PublishActivity`
- `MainActivity`

**正常日志示例**：
```
D/FirebaseHelper: FirebaseHelper 初始化
D/FirebaseHelper: 数据库URL: https://keshe-306fe-default-rtdb.firebaseio.com
D/PublishActivity: 发布信息 - 用户ID: xxx, 用户名: xxx
D/PublishActivity: 校区: 文昌校区
D/FirebaseHelper: 开始添加物品: iPhone 15
D/FirebaseHelper: 生成的物品ID: -Nxxx
D/FirebaseHelper: ✅ 物品添加成功: -Nxxx
D/PublishActivity: ✅ 发布成功，物品ID: -Nxxx
```

**错误日志示例**：
```
E/FirebaseHelper: ❌ 物品添加失败: Permission denied
E/FirebaseHelper: 错误代码: PERMISSION_DENIED
E/FirebaseHelper: 错误详情: Permission denied
```

### 2. 检查网络连接

确保设备或模拟器有网络连接：
```bash
adb shell ping -c 1 8.8.8.8
```

### 3. 使用测试页面

应用中包含 `FirebaseMainActivity`，可以用来测试 Firebase 连接：
1. 在 AndroidManifest.xml 中将 `FirebaseMainActivity` 设为启动 Activity
2. 运行应用
3. 点击 "添加测试数据" 按钮
4. 查看日志输出

---

## 常见错误

### 错误 1: Permission denied

**原因**：数据库规则不允许写入

**解决**：按照步骤 3 修改数据库规则

### 错误 2: Network error

**原因**：网络连接问题

**解决**：
- 检查设备网络连接
- 确保防火墙未阻止 Firebase 连接
- 尝试切换到其他网络

### 错误 3: 用户ID为空

**原因**：用户未登录

**解决**：
- 确保用户已登录
- 检查 `UserManager.getUserId()` 返回值

---

## 联系方式

如果问题仍然存在，请提供以下信息：
1. Logcat 日志（过滤 `FirebaseHelper` 和 `PublishActivity`）
2. Firebase 控制台截图（数据库规则页面）
3. 设备/模拟器信息