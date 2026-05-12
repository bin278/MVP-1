package com.campus.lostfound.constant

object Constants {
    const val DB_NAME = "lost_found.db"
    const val DB_VERSION = 2

    const val PREF_NAME = "campus_lost_found"
    const val KEY_LOGIN_STATUS = "login_status"
    const val KEY_CURRENT_USER = "current_user"

    const val ITEM_TYPE_LOST = "lost"
    const val ITEM_TYPE_FOUND = "found"

    const val IMAGE_DIR = "images"

    const val AMAP_API_KEY = "YOUR_AMAP_API_KEY"

    val CATEGORIES = arrayOf(
        "电子产品", "证件卡片", "钥匙钱包", "书籍文具",
        "衣物配饰", "生活用品", "运动器材", "其他"
    )
}
