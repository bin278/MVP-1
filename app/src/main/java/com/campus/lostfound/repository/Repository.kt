package com.campus.lostfound.repository

import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.Item
import com.campus.lostfound.firebase.User

/**
 * 数据仓库类
 * 统一管理物品数据的来源（Firebase）
 * 提供简洁的API接口给上层调用
 */
object ItemRepository {

    /**
     * 获取所有物品列表
     * @param type 物品类型："lost" 或 "found"，为null时返回所有
     * @param callback 回调函数，返回物品列表
     */
    fun getAllItems(type: String? = null, callback: (List<Item>) -> Unit) {
        FirebaseHelper.getAllItems(type, callback)
    }

    /**
     * 获取物品详情
     * @param itemId 物品ID
     * @param callback 回调函数，返回物品对象
     */
    fun getItem(itemId: String, callback: (Item?) -> Unit) {
        FirebaseHelper.getItem(itemId, callback)
    }

    /**
     * 添加物品
     * @param item 物品对象
     * @param callback 回调函数，返回物品ID
     */
    fun addItem(item: Item, callback: (String?) -> Unit) {
        FirebaseHelper.addItem(item, callback)
    }

    /**
     * 更新物品
     * @param item 物品对象
     * @param callback 回调函数，返回是否成功
     */
    fun updateItem(item: Item, callback: (Boolean) -> Unit) {
        FirebaseHelper.updateItem(item, callback)
    }

    /**
     * 删除物品
     * @param itemId 物品ID
     * @param callback 回调函数，返回是否成功
     */
    fun deleteItem(itemId: String, callback: (Boolean) -> Unit) {
        FirebaseHelper.deleteItem(itemId, callback)
    }

    /**
     * 获取用户发布的物品
     * @param userId 用户ID
     * @param callback 回调函数，返回物品列表
     */
    fun getItemsByUser(userId: String, callback: (List<Item>) -> Unit) {
        FirebaseHelper.getItemsByUser(userId, callback)
    }

    /**
     * 搜索物品
     * @param keyword 搜索关键词
     * @param callback 回调函数，返回匹配的物品列表
     */
    fun searchItems(keyword: String, callback: (List<Item>) -> Unit) {
        FirebaseHelper.searchItems(keyword, callback)
    }
}

/**
 * 用户数据仓库类
 * 统一管理用户数据的来源（Firebase）
 */
object UserRepository {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param callback 回调函数，返回用户对象
     */
    fun login(username: String, password: String, callback: (User?) -> Unit) {
        FirebaseHelper.login(username, password, callback)
    }

    /**
     * 用户注册
     * @param user 用户对象
     * @param callback 回调函数，返回是否成功和用户ID
     */
    fun register(user: User, callback: (Boolean, String?) -> Unit) {
        FirebaseHelper.register(user, callback)
    }

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @param callback 回调函数，返回用户对象
     */
    fun getUser(userId: String, callback: (User?) -> Unit) {
        FirebaseHelper.getUser(userId, callback)
    }

    /**
     * 更新用户信息
     * @param user 用户对象
     * @param callback 回调函数，返回是否成功
     */
    fun updateUser(user: User, callback: (Boolean) -> Unit) {
        FirebaseHelper.updateUser(user, callback)
    }
}

/**
 * 收藏数据仓库类
 * 统一管理收藏数据的来源（Firebase）
 */
object FavoriteRepository {

    /**
     * 添加收藏
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param callback 回调函数，返回是否成功
     */
    fun addFavorite(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        FirebaseHelper.addFavorite(userId, itemId, callback)
    }

    /**
     * 取消收藏
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param callback 回调函数，返回是否成功
     */
    fun removeFavorite(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        FirebaseHelper.removeFavorite(userId, itemId, callback)
    }

    /**
     * 检查是否已收藏
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param callback 回调函数，返回是否已收藏
     */
    fun isFavorite(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        FirebaseHelper.isFavorite(userId, itemId, callback)
    }

    /**
     * 获取用户收藏列表
     * @param userId 用户ID
     * @param callback 回调函数，返回收藏的物品列表
     */
    fun getUserFavorites(userId: String, callback: (List<Item>) -> Unit) {
        FirebaseHelper.getUserFavorites(userId, callback)
    }
}
