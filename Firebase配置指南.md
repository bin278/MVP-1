# Firebase 配置指南

## 简介

本项目使用 Firebase Realtime Database 作为云数据库，无需自己搭建服务器，数据实时同步。

## Firebase 优势

| 优势 | 说明 |
|------|------|
| 免费额度大 | 免费套餐：5GB存储、50GB/月下载 |
| 实时同步 | 数据变更实时推送到所有设备 |
| 无需服务器 | 不需要自己搭建后端服务 |
| 离线支持 | 支持离线数据缓存 |
| 身份验证 | 内置用户认证系统 |

## 配置步骤

### 步骤1：创建Firebase项目

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 点击 "Add project"（添加项目）
3. 输入项目名称：`CampusLostFound`
4. 关闭 Google Analytics（可选）
5. 点击 "Create project"

### 步骤2：注册Android应用

1. 进入项目后，点击 **Android** 图标添加应用
2. 填写应用包名：`com.campus.lostfound`
3. 点击 "Register app"

### 步骤3：下载配置文件

1. 下载 `google-services.json` 文件
2. 将文件复制到项目目录：`app/google-services.json`
3. **替换** 占位符文件

### 步骤4：在Firebase中创建数据库

1. 左侧菜单选择 **Build → Realtime Database**
2. 点击 "Create Database"
3. 选择存储位置（建议选择离你最近的区域）
4. 选择 "Start in test mode"（测试模式）
5. 点击 "Enable"

### 步骤5：添加测试数据（可选）

在Firebase Console中手动添加测试数据：

```json
{
  "items": {
    "-NhXXXXX": {
      "type": "lost",
      "name": "校园卡",
      "description": "丢失了一张校园卡",
      "location": "图书馆",
      "contact": "13800138000",
      "publishTime": 1716000000000,
      "publisherId": "user123",
      "publisherName": "张三"
    }
  },
  "users": {
    "-NhXXXXX": {
      "username": "test",
      "password": "123456",
      "nickname": "测试用户",
      "phone": "13800138000",
      "createTime": 1715900000000
    }
  }
}
```

## 数据结构说明

### 物品表 (items)

```json
{
  "items": {
    "[物品ID]": {
      "type": "lost",        // "lost" 或 "found"
      "name": "物品名称",
      "description": "详细描述",
      "location": "地点",
      "contact": "联系方式",
      "imageUrl": "图片URL",
      "publishTime": 时间戳,
      "publisherId": "发布者ID",
      "publisherName": "发布者昵称"
    }
  }
}
```

### 用户表 (users)

```json
{
  "users": {
    "[用户ID]": {
      "username": "用户名",
      "password": "密码",
      "nickname": "昵称",
      "phone": "手机号",
      "createTime": 时间戳
    }
  }
}
```

### 收藏表 (favorites)

```json
{
  "favorites": {
    "[用户ID]_[物品ID]": {
      "userId": "用户ID",
      "itemId": "物品ID",
      "createTime": 时间戳
    }
  }
}
```

## 代码使用示例

### 添加物品

```kotlin
val item = Item(
    type = "lost",
    name = "校园卡",
    description = "丢失了一张校园卡",
    location = "图书馆",
    contact = "13800138000",
    publisherId = userId,
    publisherName = "张三"
)

FirebaseHelper.addItem(item) { itemId ->
    if (itemId != null) {
        Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show()
    }
}
```

### 获取物品列表

```kotlin
FirebaseHelper.getAllItems("lost") { items ->
    // items 是 List<Item> 类型
    adapter.submitList(items)
}
```

### 用户登录

```kotlin
FirebaseHelper.login(username, password) { user ->
    if (user != null) {
        // 登录成功，保存用户信息
        saveUserToLocal(user)
    } else {
        // 登录失败
    }
}
```

## 安全规则建议

在 Firebase Console 中设置适当的安全规则：

```json
{
  "rules": {
    "items": {
      ".read": true,
      ".write": "auth != null"
    },
    "users": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "favorites": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

## 下一步

1. 下载并配置 `google-services.json`
2. 在 Firebase Console 中创建数据库
3. 运行项目测试

## 常见问题

**Q: 构建失败提示 google-services.json 错误？**
A: 确保文件放在 `app/google-services.json`，且包名正确。

**Q: 数据库权限被拒绝？**
A: 在 Firebase Console 中将安全规则设置为测试模式或添加适当规则。
