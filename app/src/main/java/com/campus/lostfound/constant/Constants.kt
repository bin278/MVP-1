package com.campus.lostfound.constant

/**
 * 应用全局常量配置
 */
object Constants {
    // 数据库相关
    const val DB_NAME = "lost_found.db"         // 数据库文件名
    const val DB_VERSION = 2                    // 数据库版本号

    // SharedPreferences相关
    const val PREF_NAME = "campus_lost_found"    // SharedPreferences名称
    const val KEY_LOGIN_STATUS = "login_status"  // 登录状态键
    const val KEY_CURRENT_USER = "current_user"  // 当前用户键
    const val KEY_NICKNAME = "nickname"          // 昵称键
    const val KEY_STUDENT_ID = "student_id"      // 学号键
    const val KEY_CAMPUS = "campus"              // 校区键

    // 物品类型
    const val ITEM_TYPE_LOST = "lost"            // 丢失物品
    const val ITEM_TYPE_FOUND = "found"          // 招领物品

    // 文件存储
    const val IMAGE_DIR = "images"               // 图片存储目录

    // 地图API Key（请替换为您自己的高德地图API Key）
    const val AMAP_API_KEY = "a82677ccfb3f2256fa651d39cc78c591"

    // 物品分类列表
    val CATEGORIES = arrayOf(
        "电子产品", "证件卡片", "钥匙钱包", "书籍文具",
        "衣物配饰", "生活用品", "运动器材", "其他"
    )

    // 校区列表
    val CAMPUSES = arrayOf(
        "大学城校区", "石牌校区", "南海校区", "汕尾校区"
    )
}
