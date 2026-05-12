# 校园失物招领系统 技术文档

**版本**：2.0  
**更新日期**：2026年5月10日

---

## 一、引言

### 1.1 项目背景
大学校园中，物品遗失与拾获频繁发生，传统张贴公告、QQ群扩散等方式信息分散、效率低下。为搭建统一的校园物品招领信息平台，本项目开发一款基于 Android 的失物招领应用，实现信息发布、分类浏览、收藏管理、用户登录等核心功能，并创造性地引入 AI 智能辅助与地图地点标记，提升寻物成功率与用户体验。

### 1.2 目标与范围
- 建立本地化失物/招领信息数据库，保障数据隐私与离线可用性。
- 提供简洁流畅的信息发布、检索、个人管理闭环。
- **智能增强**：通过 AI 实现物品类型自动推荐、图像识别辅助填写。
- **空间可视化**：接入地图 SDK，支持拾取/丢失地点标记、静态图展示与一键导航。
- 限定于校园场景，用户群体为在校学生，部署于 Android 移动端。

### 1.3 阅读对象
开发人员、课程设计评审者。

---

## 二、需求分析

### 2.1 功能性需求

| 模块 | 编号 | 功能描述 | 优先级 |
|------|------|----------|--------|
| 用户系统 | F1 | 注册账号（用户名+密码），登录验证，状态保持 | 高 |
|  | F2 | 退出登录 | 中 |
| 信息发布 | F3 | 发布失物/招领信息：物品名称、类型、地点、时间、联系方式、描述、图片（拍照/相册） | 高 |
|  | F4 | 地图选点标记地点（经纬度+地址文字） | 高 |
|  | F5 | AI智能分类：根据物品名称自动推荐类型 | 中 |
|  | F6 | AI图像识别：上传图片后自动识别物品特征并填充描述 | 低（可选增强） |
| 信息浏览 | F7 | 三个标签页展示全部/失物/招领列表 | 高 |
|  | F8 | 按物品类型、发布时间筛选 | 中 |
|  | F9 | 列表信息展示缩略图、标题、时间、地点（文字地址） | 高 |
| 详情与收藏 | F10 | 查看物品完整信息（含大图、描述、联系方式、地图静态图） | 高 |
|  | F11 | 收藏/取消收藏条目 | 中 |
| | F12 | 从详情页一键调起地图导航 | 中 |
| 个人中心 | F13 | 查看我的发布列表，编辑、删除发布信息 | 高 |
|  | F14 | 查看我的收藏列表 | 中 |
|  | F15 | 展示当前登录用户名 | 低 |

### 2.2 非功能性需求
- **性能**：列表滑动流畅（60fps），图片异步加载，本地数据库查询响应<300ms。
- **界面**：遵循 Material Design 设计规范，适配主流屏幕尺寸。
- **安全**：用户密码加密存储（简易 MD5），本地数据沙箱保护。
- **可扩展**：AI 与地图功能模块化封装，方便关闭/替换。
- **兼容性**：支持 Android 5.0（API 21）及以上。

---

## 三、系统总体设计

### 3.1 架构设计
采用 **MVP（Model-View-Presenter）** 分层架构，将界面逻辑与业务逻辑解耦，提高复用性和可测试性。

- **View 层**：Activity、Fragment、Adapter，负责 UI 展示与用户交互。
- **Presenter 层**：处理业务逻辑，调度 Model，通知 View 刷新。
- **Model 层**：数据源（SQLite、SharedPreferences）、网络请求（AI 云服务）、地图 SDK 封装。

**项目包结构**：
```
com.campus.lostfound
├── model/              # 实体类（Item, User）
├── db/                 # SQLiteOpenHelper, DAO接口及实现
├── sharedpref/         # SharedPreferences管理类
├── presenter/          # 业务逻辑层
├── view/
│   ├── activity/       # 登录、注册、主界面、发布、详情等Activity
│   ├── fragment/       # 列表Fragment、个人中心Fragment
│   ├── adapter/        # RecyclerView适配器
│   └── mapview/        # 地图选点封装类
├── ai/                 # AI辅助工具类（分类、识别）
├── util/               # 图片处理、时间工具等
└── constant/           # 全局常量
```

### 3.2 模块划分
1. **用户模块**：注册、登录、状态管理。
2. **信息发布模块**：表单输入、图片获取、地图选点、AI分类联想。
3. **信息展示模块**：多标签列表、详情页（含地图静态图和导航）。
4. **收藏模块**：收藏操作与列表展示。
5. **个人中心模块**：发布管理、收藏管理。
6. **地图服务模块**：定位、选点、静态图生成、外部导航。
7. **AI 增强模块**：文本分类推荐、图像识别辅助填充（可配置开关）。

### 3.3 界面导航流程
```
启动 → [登录/注册] → 主界面（底部导航栏）
                    ├── Tab1：全部信息(Fragment)
                    ├── Tab2：失物信息(Fragment)
                    ├── Tab3：招领信息(Fragment)
                    └── Tab4：个人中心(Fragment)
                         ├── 我的发布 → 可编辑、删除
                         ├── 我的收藏 → 取消收藏
                         └── 退出登录
主界面悬浮按钮(+) → 发布页 → 表单+地图选点+AI分类 → 提交
列表Item点击 → 详情页 → 收藏、地图静态图、一键导航
```

---

## 四、数据库设计

### 4.1 用户存储（SharedPreferences）
- 注册信息以 `"user_" + username` 为键，值存放密码的 MD5 值。
- 登录状态：`login_status`(boolean)、`current_user`(String)。
- 提供 `UserManager` 工具类完成注册、登录、状态查询。

### 4.2 SQLite 表结构

#### 信息表 `items`
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 信息唯一ID |
| type | TEXT | NOT NULL | "lost"或"found" |
| name | TEXT | NOT NULL | 物品名称 |
| category | TEXT | | 推荐类型(可选，AI或手动) |
| location | TEXT | | 文字地址(手动填或逆地理) |
| time | TEXT | | 丢失/拾取时间 |
| contact | TEXT | NOT NULL | 联系方式 |
| description | TEXT | | 描述 |
| image_path | TEXT | | 图片本地存储路径 |
| publisher | TEXT | NOT NULL | 发布者用户名 |
| publish_time | TEXT | NOT NULL | 发布时间(时间戳) |
| latitude | REAL | | 纬度(地图选点) |
| longitude | REAL | | 经度(地图选点) |
| address_text | TEXT | | 地图选点详细地址 |

#### 收藏表 `favorites`
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 收藏ID |
| username | TEXT | NOT NULL | 收藏者用户名 |
| item_id | INTEGER | NOT NULL | 被收藏的items.id |
| create_time | TEXT | | 收藏时间 |

- 联合唯一约束：`UNIQUE(username, item_id)` 防止重复收藏。

### 4.3 数据库版本管理
- 初始版本(1)：无经纬度字段。  
- 版本升级(2)：`onUpgrade` 执行 `ALTER TABLE items ADD COLUMN latitude REAL;` 等三条语句，并纳入 `try-catch` 处理已存在列异常。

---

## 五、详细设计与核心实现

### 5.1 用户系统
- **注册**：`UserManager.register(username, password)` → 检查 key 是否存在，不存在则新建并保存密码 MD5。
- **登录**：比较存储的 MD5 与输入密码 MD5，成功则写入 login_status 与 current_user。
- **自动登录**：App 启动时检查 `login_status`，若为 true 则直接跳转主界面。

### 5.2 信息发布模块

#### 5.2.1 表单布局
- 物品名称（EditText）
- 物品类型（Spinner 或 AutoCompleteTextView，配合 AI 分类）
- 丢失/拾取地点（EditText + 地图选点按钮）
- 时间选择器（DatePicker 或自定义）
- 联系方式（EditText）
- 描述（EditText 多行）
- 图片区域（拍照/相册按钮 + 缩略图预览）
- 提交按钮

#### 5.2.2 地图选点实现（基于高德地图）
- **集成**：引入高德 3D 地图 SDK、定位 SDK。
- **选点界面**：
  - 自定义 MapPointActivity，中央固定 Marker，用户拖动地图，通过 `onCameraChangeFinish` 获取中心点坐标。
  - 使用 `RegeocodeQuery` 逆地理编码得到地址文字，底部显示。
  - 提供搜索框（PoiSearch）支持校园建筑检索。
  - 确认后通过 `Intent` 回传经纬度与地址至发布页。
- **数据存储**：`latitude`, `longitude`, `address_text` 一同写入 `items` 表。

#### 5.2.3 AI 智能分类
**方案**：轻量级本地规则 + 云端增强（可切换）  
- **本地规则引擎**：预置关键词-类别映射表（如“充电宝→电子产品”，“教材→书籍”），对物品名称进行分词匹配，推荐 Top 1 类别填入 Spinner，用户可修改。
- **增强形式**：若开启“智能识别”开关，调用百度 AI 文本分类 API（需联网），返回更准确的类别。

#### 5.2.4 图像上传与可选识别
- **图片获取**：使用 `FileProvider` + `Intent.ACTION_IMAGE_CAPTURE` / `Intent.ACTION_PICK` 实现拍照或相册选取，图片压缩后保存至内部存储 `images/` 目录，路径入库。
- **AI 图像识别（可选）**：
  - 调用百度 AI 通用物体识别 API 或 ML Kit 图像标签。
  - 识别结果（如“钥匙”、“眼镜”）自动填入物品名称或描述文本框，用户确认后提交。
  - 该功能通过配置开关控制，默认关闭以减少学习成本。

### 5.3 信息展示模块

#### 5.3.1 主界面标签页
- 使用 `ViewPager2` + `TabLayout`，三个 `Fragment`（AllItemsFragment、LostFragment、FoundFragment）共用同一适配器，通过 `type` 参数区分查询。
- 每个 Fragment 内嵌 `SwipeRefreshLayout` 包裹 `RecyclerView`。
- **筛选**：顶部增加 Spinner（物品类型）和日期选择，动态刷新列表。

#### 5.3.2 列表适配器
- `ItemAdapter`：使用 `Glide` 加载缩略图，展示标题、地点（优先显示 `address_text`，无则显示 `location`）、发布时间。
- Item 点击事件委托给 Activity 打开详情页。

#### 5.3.3 详情页
- 顶部大图，下方物品信息表格。
- **联系方式**使用彩色背景高亮。
- **地图区域**：
  - 若经纬度非空，加载高德静态地图图片（`ImageView` + URL）。
    ```
    https://restapi.amap.com/v3/staticmap?location=lng,lat&zoom=15&size=400*200&markers=mid,0xFF0000,A:lng,lat&key=你的Key
    ```
  - 点击静态图或“导航”按钮，弹出支持的地图 App 列表（高德/百度），构造对应 Intent 调起导航。
- 收藏按钮：根据 `FavoriteDao` 查询结果动态切换图标和功能。

### 5.4 收藏与个人中心

- **收藏操作**：`FavoriteDao.addFavorite()` / `removeFavorite()`，事务保证一致性。
- **我的发布**：查询 `items` 表中 `publisher = 当前用户` 的数据，列表每项提供编辑（带预填充的发布页）和删除（对话框确认，级联删除收藏记录）。
- **我的收藏**：关联查询 `favorites` 和 `items`，展示收藏的详细信息，侧滑或长按取消收藏。

### 5.5 AI 与地图服务的配置与降级
- 所有 AI 调用封装在 `AiHelper` 类中，增加网络检测与超时处理，失败时回退至手动输入，不阻塞主流程。
- 地图静态图 URL 需处理 Key 权限，若无高德 Key 则降级显示文字地址，保证核心功能不受影响。

---

## 六、技术栈与依赖

| 技术/库 | 版本/说明 | 用途 |
|--------|-----------|------|
| Android SDK | min 21, target 33 | 基础平台 |
| Java/Kotlin | 混合 | 开发语言 |
| SQLite | 系统内置 | 本地数据存储 |
| SharedPreferences | 系统内置 | 用户凭证存储 |
| Glide | 4.x | 图片加载 |
| Material Components | 1.x | UI 组件 |
| ViewPager2 + TabLayout | AndroidX | 标签页切换 |
| SwipeRefreshLayout | AndroidX | 下拉刷新 |
| 高德地图 SDK | 3D地图+定位+搜索 | 地图选点、静态图、导航 |
| 百度 AI 开放平台 SDK（可选） | 文本分类、图像识别 | AI 增强功能 |
| TensorFlow Lite（可选） | 2.x | 离线分类模型 |

**可选依赖**：  
- `com.amap.api:3dmap:9.7.0`  
- `com.amap.api:location:5.6.0`  
- `com.amap.api:search:9.7.0`  
- 百度 AI SDK（如需）通过远程仓库引入对应模块。

---

## 七、测试计划

### 7.1 功能测试用例
| 模块 | 测试内容 | 预期结果 |
|------|----------|----------|
| 注册登录 | 正常注册、重复注册、空字段、密码不一致 | 注册成功/失败提示；登录成功进入主页 |
| 发布信息 | 必填项缺少、图片上传、地图选点、AI分类自动填写 | 提交失败提示；提交后列表刷新数据正确 |
| 列表浏览 | Tab切换、筛选功能、下拉刷新 | 各类别数据正确，筛选后列表更新 |
| 详情与收藏 | 详情信息完整性、地图静态图显示、收藏/取消切换 | 地图正常加载，收藏心形图标状态改变 |
| 个人中心 | 我的发布编辑/删除、收藏列表 | 编辑后数据更新，删除后列表与详情消失 |
| 权限异常 | 拒绝定位权限、SD卡权限 | 提示引导或功能降级 |

### 7.2 AI与地图专项测试
- **地图选点**：拖动地图坐标更新正常，逆地理编码返回合理地址。
- **静态图**：不同经纬度加载成功，加载失败降级为文字地址。
- **AI分类**：输入“钥匙”推荐类型“生活用品”；未联网时回退出示例列表。
- **图像识别**：拍照书本返回“书籍”标签，填入名称，不影响提交。

### 7.3 兼容性测试
- 主流屏幕尺寸（5.5寸~7寸）UI 布局自适应。
- Android 5.0/6.0/10/13 真机或模拟器运行无崩溃。

---

## 八、开发规范与部署

- 代码管理：Git，遵循功能分支开发。
- 密钥管理：高德 Key、百度 API Key 存储在 `local.properties` 或加密配置文件中，不提交版本库。
- 发布构建：生成 release 签名，关闭调试日志。

---

## 九、总结

本文档从需求、设计到实现细节，全面描述了校园失物招领系统的技术方案。系统在满足课程设计基本要求的基础上，通过 AI 智能分类与识别、地图地点标记与导航两大创新点，显著提升了项目的实用性和展示价值。模块化设计保证了系统的可维护性和扩展性，各增强功能均设计了降级策略，确保核心流程稳定运行，适合作为 Android 课程设计的完整技术文档。

---

## 十、开发进度记录

### 开发环境
- **开发日期**：2026年5月11日
- **构建状态**：✅ BUILD SUCCESSFUL
- **包名**：com.campus.lostfound
- **minSdk**：24 / **targetSdk**：36
- **构建工具**：Gradle 8.13.2 + AGP 8.13.2 + Kotlin 2.0.21

### 功能完成状态

| 步骤 | 模块 | 功能 | 状态 | 完成日期 |
|------|------|------|------|----------|
| Step 1 | 项目基础搭建 | 包名修改、依赖添加（Glide/ViewPager2/SwipeRefreshLayout等）、包结构创建、主题配色、FileProvider配置 | ✅ 完成 | 2026-05-11 |
| Step 2 | 用户系统 | Item/Favorite实体类、UserManager（SharedPreferences注册/登录/状态管理）、LoginActivity、RegisterActivity | ✅ 完成 | 2026-05-11 |
| Step 3 | 数据库层 | DbHelper（含V2升级逻辑）、ItemDao（增删改查）、FavoriteDao（收藏/取消/查询） | ✅ 完成 | 2026-05-11 |
| Step 4 | 主界面框架 | MainActivity（ViewPager2+TabLayout+BottomNavigationView）、4个Fragment、FAB发布按钮 | ✅ 完成 | 2026-05-11 |
| Step 5 | 信息发布模块 | PublishActivity（表单/图片拍照相册/地图选点/AI分类）、编辑模式支持 | ✅ 完成 | 2026-05-11 |
| Step 6 | 信息列表展示 | ItemAdapter（Glide图片加载）、ItemListFragment（类型筛选/下拉刷新） | ✅ 完成 | 2026-05-11 |
| Step 7 | 详情页 | DetailActivity（完整信息/地图静态图/一键导航/分享） | ✅ 完成 | 2026-05-11 |
| Step 8 | 收藏功能 | 收藏/取消收藏切换、收藏列表展示 | ✅ 完成 | 2026-05-11 |
| Step 9 | 个人中心 | ProfileFragment、MyPublishActivity（编辑/删除）、MyFavoriteActivity（取消收藏）、退出登录 | ✅ 完成 | 2026-05-11 |
| Step 10 | AI智能分类 | AiHelper本地规则引擎（关键词-类别映射表），发布页AI分类按钮 | ✅ 完成 | 2026-05-11 |
| Step 11 | 地图服务集成 | MapPointActivity（校园预设地点选择/手动输入经纬度）、静态地图URL、导航Intent调起 | ✅ 完成 | 2026-05-11 |
| Step 12 | 构建验证 | clean assembleDebug 构建通过 | ✅ 完成 | 2026-05-11 |

### 已创建文件清单

**Kotlin源码**：
- `constant/Constants.kt` — 全局常量
- `util/Md5Util.kt` — MD5加密工具
- `util/TimeUtil.kt` — 时间格式化工具
- `model/Item.kt` — 物品信息实体
- `model/Favorite.kt` — 收藏实体
- `sharedpref/UserManager.kt` — 用户状态管理
- `db/DbHelper.kt` — SQLite数据库帮助类
- `db/ItemDao.kt` — 物品信息DAO
- `db/FavoriteDao.kt` — 收藏DAO
- `ai/AiHelper.kt` — AI智能分类引擎
- `view/activity/BaseActivity.kt` — Activity基类
- `view/activity/LoginActivity.kt` — 登录页
- `view/activity/RegisterActivity.kt` — 注册页
- `view/activity/MainActivity.kt` — 主界面
- `view/activity/PublishActivity.kt` — 信息发布页
- `view/activity/DetailActivity.kt` — 详情页
- `view/activity/MapPointActivity.kt` — 地图选点页
- `view/activity/MyPublishActivity.kt` — 我的发布页
- `view/activity/MyFavoriteActivity.kt` — 我的收藏页
- `view/fragment/ItemListFragment.kt` — 列表Fragment
- `view/fragment/ProfileFragment.kt` — 个人中心Fragment
- `view/adapter/ItemAdapter.kt` — 列表适配器

**布局文件**：
- `activity_login.xml` / `activity_register.xml`
- `activity_main.xml` / `activity_publish.xml`
- `activity_detail.xml` / `activity_map_point.xml`
- `activity_my_publish.xml` / `activity_my_favorite.xml`
- `fragment_item_list.xml` / `fragment_profile.xml`
- `item_list_item.xml` / `item_location.xml`

**资源与配置**：
- `colors.xml` / `strings.xml` / `themes.xml`（日间+夜间）
- `bottom_nav_menu.xml` / `file_paths.xml`
- `AndroidManifest.xml`（含权限声明、FileProvider、Activity注册）

### 待优化项
- [ ] 高德地图SDK正式集成（替换预设地点方案，需申请Key）
- [ ] 百度AI图像识别集成（可选增强功能）
- [ ] 图片压缩优化
- [ ] 搜索功能增强
- [ ] UI细节打磨（图标、动画）

---

*文档结束*