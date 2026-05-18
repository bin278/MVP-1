-- ============================================
-- 校园失物招领系统数据库脚本
-- 数据库名称：campus_lost_found
-- 字符集：utf8mb4（支持中文表情符号）
-- 创建时间：2026-05-17
-- ============================================

-- 如果数据库已存在则删除（可选，谨慎使用）
-- DROP DATABASE IF EXISTS campus_lost_found;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS campus_lost_found
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE campus_lost_found;

-- ============================================
-- 用户信息表
-- 存储用户注册信息、登录凭证和个人资料
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    -- 用户ID，自增主键
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID，自增主键',

    -- 用户名（登录账号），唯一不能重复
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（登录账号）',

    -- 密码（MD5加密，32位十六进制字符串）
    password VARCHAR(64) NOT NULL COMMENT '密码（MD5加密）',

    -- 昵称（显示名称），可选
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称（显示名称）',

    -- 学号，可选
    student_id VARCHAR(20) DEFAULT '' COMMENT '学号',

    -- 校区：文昌校区、柳东校区、柳石校区
    campus VARCHAR(50) DEFAULT '' COMMENT '校区',

    -- 头像文件路径，可选
    avatar_path TEXT DEFAULT '' COMMENT '头像文件路径',

    -- 创建时间索引，加速按用户名查询
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ============================================
-- 物品表（失物招领信息）
-- 存储用户发布的失物和招领信息
-- ============================================
CREATE TABLE IF NOT EXISTS items (
    -- 物品ID，自增主键
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '物品ID，自增主键',

    -- 类型：lost（丢失物品）/ found（招领物品）
    type VARCHAR(10) NOT NULL COMMENT '类型：lost（失物）/ found（招领）',

    -- 物品名称
    name VARCHAR(100) NOT NULL COMMENT '物品名称',

    -- 物品分类：电子产品、证件卡片、钥匙钱包、书籍文具、衣物配饰、生活用品、运动器材、其他
    category VARCHAR(50) DEFAULT '' COMMENT '物品分类',

    -- 地点名称
    location VARCHAR(200) DEFAULT '' COMMENT '地点名称',

    -- 丢失或发现时间
    time VARCHAR(50) DEFAULT '' COMMENT '丢失/发现时间',

    -- 联系方式（手机号）
    contact VARCHAR(50) NOT NULL COMMENT '联系方式（手机号）',

    -- 详细描述
    description TEXT DEFAULT '' COMMENT '详细描述',

    -- 图片文件路径（多图用 ||| 分隔）
    image_path TEXT DEFAULT '' COMMENT '图片路径（多图用|||分隔）',

    -- 发布者用户名
    publisher VARCHAR(100) NOT NULL COMMENT '发布者用户名',

    -- 发布时间（格式：yyyy-MM-dd HH:mm:ss）
    publish_time VARCHAR(50) NOT NULL COMMENT '发布时间',

    -- 纬度（GPS坐标）
    latitude DOUBLE DEFAULT 0.0 COMMENT '纬度',

    -- 经度（GPS坐标）
    longitude DOUBLE DEFAULT 0.0 COMMENT '经度',

    -- 详细地址文字描述
    address_text VARCHAR(500) DEFAULT '' COMMENT '详细地址文字',

    -- 索引：加速按发布者查询
    INDEX idx_publisher (publisher),

    -- 索引：加速按类型查询
    INDEX idx_type (type),

    -- 索引：加速按发布时间排序
    INDEX idx_publish_time (publish_time),

    -- 索引：加速按分类查询
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领物品表';

-- ============================================
-- 收藏表
-- 存储用户收藏的物品记录
-- ============================================
CREATE TABLE IF NOT EXISTS favorites (
    -- 收藏记录ID，自增主键
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID，自增主键',

    -- 用户名
    username VARCHAR(50) NOT NULL COMMENT '用户名',

    -- 物品ID，外键关联items表
    item_id INT NOT NULL COMMENT '物品ID',

    -- 收藏时间（格式：yyyy-MM-dd HH:mm:ss）
    create_time VARCHAR(50) DEFAULT '' COMMENT '收藏时间',

    -- 外键约束：删除物品时级联删除收藏记录
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,

    -- 唯一约束：同一用户不能重复收藏同一物品
    UNIQUE KEY uk_user_item (username, item_id),

    -- 索引：加速按用户名查询收藏列表
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ============================================
-- 插入测试用户数据
-- 用户名：测试用户
-- 密码：123456（MD5加密后：e10adc3949ba59abbe56e057f20f883e）
-- ============================================
INSERT INTO users (username, password, nickname, student_id, campus)
VALUES
    ('测试用户', 'e10adc3949ba59abbe56e057f20f883e', '小明', '20240001', '文昌校区'),
    ('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '20240000', '柳东校区');

-- ============================================
-- 插入测试物品数据
-- 发布时间基于 2026-05-17 计算
-- ============================================
INSERT INTO items (type, name, category, location, time, contact, description, publisher, publish_time, latitude, longitude, address_text)
VALUES
    -- 失物案例
    (
        'lost',
        '黑色钱包',
        '钥匙钱包',
        '图书馆二楼',
        '2026-05-10',
        '13800138001',
        '黑色皮质钱包，内有身份证和银行卡，身份证姓名张伟',
        '测试用户',
        '2026-05-15 10:30:00',
        109.423,
        24.317,
        '图书馆二楼自习区'
    ),
    (
        'lost',
        '蓝牙耳机',
        '电子产品',
        '教学楼A座',
        '2026-05-11',
        '13800138002',
        '白色AirPods Pro无线蓝牙耳机，带蓝色保护壳，耳机盒有刻字',
        '测试用户',
        '2026-05-15 09:00:00',
        109.428,
        24.315,
        '教学楼A座301教室'
    ),
    (
        'lost',
        '校园卡',
        '证件卡片',
        '食堂一楼',
        '2026-05-12',
        '13800138003',
        '校园一卡通，卡号后四位8832，卡上姓名为李明',
        '测试用户',
        '2026-05-14 12:00:00',
        109.425,
        24.318,
        '第一食堂一楼入口处'
    ),

    -- 招领案例
    (
        'found',
        '学生证',
        '证件卡片',
        '操场看台',
        '2026-05-10',
        '13900139001',
        '计算机学院 李明同学的学生证，拾取地点在操场看台第三排',
        'admin',
        '2026-05-15 11:00:00',
        109.430,
        24.320,
        '操场看台'
    ),
    (
        'found',
        '高等数学课本',
        '书籍文具',
        '自习室',
        '2026-05-11',
        '13900139002',
        '同济版高等数学上册第七版，书中有多处详细笔记和标注，书内有书签',
        'admin',
        '2026-05-15 08:30:00',
        109.426,
        24.316,
        '第一自习室靠窗位置'
    ),
    (
        'found',
        '运动水杯',
        '生活用品',
        '体育馆',
        '2026-05-12',
        '13900139003',
        '绿色李宁品牌运动水杯，600ml容量，在体育馆更衣室拾取',
        '测试用户',
        '2026-05-14 16:00:00',
        109.432,
        24.314,
        '体育馆二楼更衣室'
    ),
    (
        'found',
        '笔记本电脑充电器',
        '电子产品',
        '图书馆五楼',
        '2026-05-13',
        '13900139004',
        '联想ThinkPad原装充电器，65W，黑色，线缆完好',
        'admin',
        '2026-05-14 20:00:00',
        109.423,
        24.317,
        '图书馆五楼研讨室'
    ),
    (
        'found',
        '雨伞',
        '生活用品',
        '教学楼B座',
        '2026-05-13',
        '13900139005',
        '黑色自动折叠伞，在B座二楼走廊拾取，伞柄有轻微磨损',
        '测试用户',
        '2026-05-14 15:30:00',
        109.429,
        24.315,
        '教学楼B座二楼走廊'
    );

-- ============================================
-- 插入测试收藏数据
-- ============================================
INSERT INTO favorites (username, item_id, create_time)
VALUES
    ('测试用户', 4, '2026-05-15 12:00:00'),
    ('测试用户', 5, '2026-05-15 12:30:00'),
    ('admin', 1, '2026-05-15 13:00:00');

-- ============================================
-- 创建视图：物品列表视图（方便查询）
-- ============================================
CREATE OR REPLACE VIEW v_items_with_publisher AS
SELECT
    i.*,
    u.nickname AS publisher_nickname,
    u.campus AS publisher_campus
FROM items i
LEFT JOIN users u ON i.publisher = u.username;

-- ============================================
-- 创建存储过程：分页查询物品
-- ============================================
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS sp_get_items_paged(
    IN p_type VARCHAR(10),
    IN p_category VARCHAR(50),
    IN p_page INT,
    IN p_page_size INT
)
BEGIN
    DECLARE v_offset INT;

    SET v_offset = (p_page - 1) * p_page_size;

    SELECT * FROM items
    WHERE (p_type IS NULL OR p_type = '' OR type = p_type)
      AND (p_category IS NULL OR p_category = '' OR category = p_category)
    ORDER BY publish_time DESC
    LIMIT p_page_size OFFSET v_offset;
END //

DELIMITER ;

-- ============================================
-- 创建存储过程：获取用户发布的物品数量
-- ============================================
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS sp_get_user_item_count(
    IN p_username VARCHAR(50)
)
BEGIN
    SELECT COUNT(*) AS item_count
    FROM items
    WHERE publisher = p_username;
END //

DELIMITER ;

-- ============================================
-- 完 SQL 脚本执行完毕
-- 使用方法：
-- 1. MySQL命令行：mysql -u root -p < campus_lost_found.sql
-- 2. Navicat/DBeaver：直接打开执行
-- 3. MySQL Workbench：文件 -> 运行SQL脚本
-- ============================================
