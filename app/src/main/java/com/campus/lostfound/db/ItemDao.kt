package com.campus.lostfound.db

import android.content.ContentValues
import android.content.Context
import com.campus.lostfound.model.Item

/**
 * 物品数据访问对象（DAO）
 * 负责物品表的增删改查操作
 */
class ItemDao(context: Context) {

    // 数据库帮助类实例
    private val dbHelper = DbHelper(context)

    /**
     * 插入新物品记录
     * @param item 物品对象
     * @return 插入成功返回新记录的ID，失败返回-1
     */
    fun insert(item: Item): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("type", item.type)
            put("name", item.name)
            put("category", item.category)
            put("location", item.location)
            put("time", item.time)
            put("contact", item.contact)
            put("description", item.description)
            put("image_path", item.imagePath)
            put("publisher", item.publisher)
            put("publish_time", item.publishTime)
            put("latitude", item.latitude)
            put("longitude", item.longitude)
            put("address_text", item.addressText)
        }
        return db.insert("items", null, values)
    }

    /**
     * 更新物品记录
     * @param item 物品对象（必须包含有效的id）
     * @return 受影响的行数
     */
    fun update(item: Item): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("type", item.type)
            put("name", item.name)
            put("category", item.category)
            put("location", item.location)
            put("time", item.time)
            put("contact", item.contact)
            put("description", item.description)
            put("image_path", item.imagePath)
            put("latitude", item.latitude)
            put("longitude", item.longitude)
            put("address_text", item.addressText)
        }
        return db.update("items", values, "id = ?", arrayOf(item.id.toString()))
    }

    /**
     * 删除物品记录（级联删除相关收藏）
     * @param id 物品ID
     * @return 受影响的行数
     */
    fun delete(id: Long): Int {
        val db = dbHelper.writableDatabase
        // 先删除相关的收藏记录
        db.delete("favorites", "item_id = ?", arrayOf(id.toString()))
        // 再删除物品记录
        return db.delete("items", "id = ?", arrayOf(id.toString()))
    }

    /**
     * 根据ID查询物品
     * @param id 物品ID
     * @return 物品对象，如果不存在返回null
     */
    fun queryById(id: Long): Item? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("items", null, "id = ?", arrayOf(id.toString()), null, null, null)
        var item: Item? = null
        if (cursor.moveToFirst()) {
            item = cursorToItem(cursor)
        }
        cursor.close()
        return item
    }

    /**
     * 查询物品列表（支持按类型和分类筛选）
     * @param type 物品类型（可选）："lost" 或 "found"
     * @param category 物品分类（可选）
     * @return 物品列表，按发布时间降序排列
     */
    fun queryAll(type: String? = null, category: String? = null): List<Item> {
        val db = dbHelper.readableDatabase
        val selection = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        // 添加类型筛选条件
        if (type != null) {
            selection.add("type = ?")
            selectionArgs.add(type)
        }
        // 添加分类筛选条件
        if (category != null) {
            selection.add("category = ?")
            selectionArgs.add(category)
        }

        val whereClause = if (selection.isNotEmpty()) selection.joinToString(" AND ") else null
        val whereArgs = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        // 执行查询，按发布时间降序排列
        val cursor = db.query("items", null, whereClause, whereArgs, null, null, "publish_time DESC")
        val list = mutableListOf<Item>()
        while (cursor.moveToNext()) {
            list.add(cursorToItem(cursor))
        }
        cursor.close()
        return list
    }

    /**
     * 根据发布者查询物品列表
     * @param publisher 发布者用户名
     * @return 该用户发布的物品列表，按发布时间降序排列
     */
    fun queryByPublisher(publisher: String): List<Item> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("items", null, "publisher = ?", arrayOf(publisher), null, null, "publish_time DESC")
        val list = mutableListOf<Item>()
        while (cursor.moveToNext()) {
            list.add(cursorToItem(cursor))
        }
        cursor.close()
        return list
    }

    /**
     * 将 Cursor 转换为 Item 对象
     * @param cursor 数据库游标
     * @return Item 对象
     */
    private fun cursorToItem(cursor: android.database.Cursor): Item {
        return cursorToItemStatic(cursor)
    }

    /**
     * 静态方法：将 Cursor 转换为 Item 对象
     * @param cursor 数据库游标
     * @return Item 对象
     */
    companion object {
        fun cursorToItemStatic(cursor: android.database.Cursor): Item {
            return Item(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                type = cursor.getString(cursor.getColumnIndexOrThrow("type")) ?: "",
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: "",
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")) ?: "",
                location = cursor.getString(cursor.getColumnIndexOrThrow("location")) ?: "",
                time = cursor.getString(cursor.getColumnIndexOrThrow("time")) ?: "",
                contact = cursor.getString(cursor.getColumnIndexOrThrow("contact")) ?: "",
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path")) ?: "",
                publisher = cursor.getString(cursor.getColumnIndexOrThrow("publisher")) ?: "",
                publishTime = cursor.getString(cursor.getColumnIndexOrThrow("publish_time")) ?: "",
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                addressText = cursor.getString(cursor.getColumnIndexOrThrow("address_text")) ?: ""
            )
        }
    }
}