package com.campus.lostfound.db

import android.content.ContentValues
import android.content.Context
import com.campus.lostfound.model.Favorite
import com.campus.lostfound.model.Item
import com.campus.lostfound.util.TimeUtil

class FavoriteDao(context: Context) {

    private val dbHelper = DbHelper(context)

    fun addFavorite(username: String, itemId: Long): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("item_id", itemId)
            put("create_time", TimeUtil.formatTimestamp(TimeUtil.currentTimestamp()))
        }
        return db.insert("favorites", null, values)
    }

    fun removeFavorite(username: String, itemId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete("favorites", "username = ? AND item_id = ?", arrayOf(username, itemId.toString()))
    }

    fun isFavorite(username: String, itemId: Long): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query("favorites", null, "username = ? AND item_id = ?",
            arrayOf(username, itemId.toString()), null, null, null)
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getFavoritesByUsername(username: String): List<Item> {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT items.* FROM items
            INNER JOIN favorites ON items.id = favorites.item_id
            WHERE favorites.username = ?
            ORDER BY favorites.create_time DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(username))
        val list = mutableListOf<Item>()
        while (cursor.moveToNext()) {
            list.add(ItemDao.cursorToItemStatic(cursor))
        }
        cursor.close()
        return list
    }
}
