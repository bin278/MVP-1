package com.campus.lostfound.db

import android.content.ContentValues
import android.content.Context
import com.campus.lostfound.model.Item

class ItemDao(context: Context) {

    private val dbHelper = DbHelper(context)

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

    fun delete(id: Long): Int {
        val db = dbHelper.writableDatabase
        db.delete("favorites", "item_id = ?", arrayOf(id.toString()))
        return db.delete("items", "id = ?", arrayOf(id.toString()))
    }

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

    fun queryAll(type: String? = null, category: String? = null): List<Item> {
        val db = dbHelper.readableDatabase
        val selection = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (type != null) {
            selection.add("type = ?")
            selectionArgs.add(type)
        }
        if (category != null) {
            selection.add("category = ?")
            selectionArgs.add(category)
        }

        val whereClause = if (selection.isNotEmpty()) selection.joinToString(" AND ") else null
        val whereArgs = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        val cursor = db.query("items", null, whereClause, whereArgs, null, null, "publish_time DESC")
        val list = mutableListOf<Item>()
        while (cursor.moveToNext()) {
            list.add(cursorToItem(cursor))
        }
        cursor.close()
        return list
    }

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

    private fun cursorToItem(cursor: android.database.Cursor): Item {
        return cursorToItemStatic(cursor)
    }

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
