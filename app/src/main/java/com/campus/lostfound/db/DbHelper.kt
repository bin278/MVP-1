package com.campus.lostfound.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.campus.lostfound.constant.Constants

/**
 * SQLite数据库帮助类
 * 负责数据库的创建和版本升级
 */
class DbHelper(context: Context) :
    SQLiteOpenHelper(context, Constants.DB_NAME, null, Constants.DB_VERSION) {

    /**
     * 首次创建数据库时调用
     * 创建 items（物品表）和 favorites（收藏表）两个表
     */
    override fun onCreate(db: SQLiteDatabase) {
        // 创建物品表
        db.execSQL("""
            CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT,
                location TEXT,
                time TEXT,
                contact TEXT NOT NULL,
                description TEXT,
                image_path TEXT,
                publisher TEXT NOT NULL,
                publish_time TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                address_text TEXT
            )
        """.trimIndent())

        // 创建收藏表
        db.execSQL("""
            CREATE TABLE favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                item_id INTEGER NOT NULL,
                create_time TEXT,
                UNIQUE(username, item_id)
            )
        """.trimIndent())
    }

    /**
     * 数据库版本升级时调用
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 版本1升级到版本2：添加地理位置相关字段
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE items ADD COLUMN latitude REAL")
            } catch (_: Exception) {
            }
            try {
                db.execSQL("ALTER TABLE items ADD COLUMN longitude REAL")
            } catch (_: Exception) {
            }
            try {
                db.execSQL("ALTER TABLE items ADD COLUMN address_text TEXT")
            } catch (_: Exception) {
            }
        }
    }
}