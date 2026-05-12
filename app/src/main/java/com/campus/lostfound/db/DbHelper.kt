package com.campus.lostfound.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.campus.lostfound.constant.Constants

class DbHelper(context: Context) :
    SQLiteOpenHelper(context, Constants.DB_NAME, null, Constants.DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
