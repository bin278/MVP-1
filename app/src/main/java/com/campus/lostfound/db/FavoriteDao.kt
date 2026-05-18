package com.campus.lostfound.db

import android.content.ContentValues
import android.content.Context
import com.campus.lostfound.model.Favorite
import com.campus.lostfound.model.Item
import com.campus.lostfound.util.TimeUtil

/**
 * 收藏数据访问对象（DAO）
 * 负责收藏表的增删改查操作
 */
class FavoriteDao(context: Context) {

    // 数据库帮助类实例
    private val dbHelper = DbHelper(context)

    /**
     * 添加收藏
     * @param username 用户名
     * @param itemId 物品ID
     * @return 插入成功返回新记录的ID，失败返回-1（如已存在）
     */
    fun addFavorite(username: String, itemId: Long): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("item_id", itemId)
            put("create_time", TimeUtil.formatTimestamp(TimeUtil.currentTimestamp()))
        }
        return db.insert("favorites", null, values)
    }

    /**
     * 取消收藏
     * @param username 用户名
     * @param itemId 物品ID
     * @return 受影响的行数
     */
    fun removeFavorite(username: String, itemId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete("favorites", "username = ? AND item_id = ?", arrayOf(username, itemId.toString()))
    }

    /**
     * 检查物品是否已被收藏
     * @param username 用户名
     * @param itemId 物品ID
     * @return 是否已收藏
     */
    fun isFavorite(username: String, itemId: Long): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query("favorites", null, "username = ? AND item_id = ?",
            arrayOf(username, itemId.toString()), null, null, null)
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    /**
     * 获取用户的收藏列表
     * @param username 用户名
     * @return 用户收藏的物品列表，按收藏时间降序排列
     */
    fun getFavoritesByUsername(username: String): List<Item> {
        val db = dbHelper.readableDatabase
        // 使用联表查询获取收藏的物品详情
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