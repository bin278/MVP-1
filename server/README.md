# 校园失物招领系统 - 后端服务

## 快速开始

### 1. 安装MySQL数据库

参考根目录的 `本地数据库部署方案.md` 文档

### 2. 导入数据库

```bash
mysql -u root -p < ../campus_lost_found.sql
```

### 3. 修改配置

编辑 `config.py`，修改MySQL密码：

```python
DB_CONFIG = {
    'password': '你的MySQL密码',  # 修改这里
    ...
}
```

### 4. 启动服务

**方式一：使用启动脚本（推荐）**

双击 `start.bat` 文件

**方式二：命令行启动**

```bash
# 安装依赖
pip install -r requirements.txt

# 启动服务
python app.py
```

### 5. 测试API

打开浏览器访问：
- http://localhost:5000/api/items

## API接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/register | POST | 用户注册 |
| /api/login | POST | 用户登录 |
| /api/user/info | GET | 获取用户信息 |
| /api/user/update | POST | 更新用户信息 |
| /api/items | GET | 获取物品列表 |
| /api/items | POST | 发布物品 |
| /api/items/{id} | GET | 获取物品详情 |
| /api/items/{id} | DELETE | 删除物品 |
| /api/favorites | GET | 获取收藏列表 |
| /api/favorites | POST | 添加收藏 |
| /api/favorites | DELETE | 取消收藏 |

## 注意事项

1. 确保MySQL服务已启动
2. 确保数据库 `campus_lost_found` 已创建
3. 如需外部访问，请开放防火墙5000端口
