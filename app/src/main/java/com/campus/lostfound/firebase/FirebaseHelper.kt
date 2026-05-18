package com.campus.lostfound.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase数据库助手类
 * 封装所有与Firebase Realtime Database的交互操作
 * 提供物品和用户的增删改查功能
 */
object FirebaseHelper {

    /**
     * Firebase数据库实例
     * 通过getInstance()获取数据库单例
     */
    private val database = FirebaseDatabase.getInstance()

    /**
     * 物品表引用
     * 路径：/items
     */
    private val itemsRef = database.getReference("items")

    /**
     * 用户表引用
     * 路径：/users
     */
    private val usersRef = database.getReference("users")

    /**
     * 收藏表引用
     * 路径：/favorites
     */
    private val favoritesRef = database.getReference("favorites")

    // ==================== 物品相关操作 ====================

    /**
     * 获取所有物品列表
     * @param type 物品类型筛选，可选值为"lost"或"found"，为null时返回所有物品
     * @return 物品列表，按发布时间倒序排列
     */
    fun getAllItems(type: String? = null, callback: (List<Item>) -> Unit) {
        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Item>()
                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(Item::class.java)
                    item?.id = dataSnapshot.key
                    // 根据类型筛选
                    if (type == null || item?.type == type) {
                        items.add(item!!)
                    }
                }
                // 按发布时间倒序排列
                items.sortByDescending { it.publishTime }
                callback(items)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    /**
     * 获取指定ID的物品详情
     * @param itemId 物品ID
     * @param callback 回调，返回物品对象
     */
    fun getItem(itemId: String, callback: (Item?) -> Unit) {
        itemsRef.child(itemId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val item = snapshot.getValue(Item::class.java)
                item?.id = snapshot.key
                callback(item)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    /**
     * 添加新物品
     * @param item 物品对象（不含ID）
     * @param callback 回调，返回生成的物品ID
     */
    fun addItem(item: Item, callback: (String?) -> Unit) {
        // 生成新的唯一ID
        val newRef = itemsRef.push()
        item.id = newRef.key
        item.publishTime = System.currentTimeMillis()

        newRef.setValue(item) { error, _ ->
            if (error == null) {
                callback(newRef.key)
            } else {
                callback(null)
            }
        }
    }

    /**
     * 更新物品信息
     * @param item 物品对象（包含ID）
     * @param callback 回调，返回是否成功
     */
    fun updateItem(item: Item, callback: (Boolean) -> Unit) {
        val itemId = item.id ?: return callback(false)
        itemsRef.child(itemId).setValue(item) { error, _ ->
            callback(error == null)
        }
    }

    /**
     * 删除物品
     * @param itemId 物品ID
     * @param callback 回调，返回是否成功
     */
    fun deleteItem(itemId: String, callback: (Boolean) -> Unit) {
        itemsRef.child(itemId).removeValue { error, _ ->
            callback(error == null)
        }
        // 同时删除相关的收藏记录
        favoritesRef.orderByChild("itemId").equalTo(itemId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
        )
    }

    /**
     * 获取用户发布的物品列表
     * @param userId 用户ID
     * @param callback 回调，返回物品列表
     */
    fun getItemsByUser(userId: String, callback: (List<Item>) -> Unit) {
        itemsRef.orderByChild("publisherId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<Item>()
                    for (dataSnapshot in snapshot.children) {
                        val item = dataSnapshot.getValue(Item::class.java)
                        item?.id = dataSnapshot.key
                        items.add(item!!)
                    }
                    items.sortByDescending { it.publishTime }
                    callback(items)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    /**
     * 搜索物品
     * @param keyword 搜索关键词（匹配名称或描述）
     * @param callback 回调，返回匹配的物品列表
     */
    fun searchItems(keyword: String, callback: (List<Item>) -> Unit) {
        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Item>()
                val lowerKeyword = keyword.lowercase()
                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(Item::class.java)
                    item?.id = dataSnapshot.key
                    // 匹配名称或描述中包含关键词的物品
                    if (item?.name?.lowercase()?.contains(lowerKeyword) == true ||
                        item?.description?.lowercase()?.contains(lowerKeyword) == true) {
                        items.add(item)
                    }
                }
                items.sortByDescending { it.publishTime }
                callback(items)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // ==================== 用户相关操作 ====================

    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码
     * @param callback 回调，返回用户对象或null
     */
    fun login(username: String, password: String, callback: (User?) -> Unit) {
        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var foundUser: User? = null
                    for (dataSnapshot in snapshot.children) {
                        val user = dataSnapshot.getValue(User::class.java)
                        if (user?.password == password) {
                            user?.id = dataSnapshot.key
                            foundUser = user
                            break
                        }
                    }
                    callback(foundUser)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    /**
     * 用户注册
     * @param user 用户对象（不含ID和创建时间）
     * @param callback 回调，返回是否成功
     */
    fun register(user: User, callback: (Boolean, String?) -> Unit) {
        // 检查用户名是否已存在
        usersRef.orderByChild("username").equalTo(user.username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        callback(false, "用户名已存在")
                        return
                    }
                    // 创建新用户
                    val newRef = usersRef.push()
                    user.id = newRef.key
                    user.createTime = System.currentTimeMillis()

                    newRef.setValue(user) { error, _ ->
                        if (error == null) {
                            callback(true, newRef.key)
                        } else {
                            callback(false, "注册失败")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, "数据库错误")
                }
            })
    }

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @param callback 回调，返回用户对象
     */
    fun getUser(userId: String, callback: (User?) -> Unit) {
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.id = snapshot.key
                callback(user)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    /**
     * 更新用户信息
     * @param user 用户对象（包含ID）
     * @param callback 回调，返回是否成功
     */
    fun updateUser(user: User, callback: (Boolean) -> Unit) {
        val userId = user.id ?: return callback(false)
        usersRef.child(userId).setValue(user) { error, _ ->
            callback(error == null)
        }
    }

    // ==================== 收藏相关操作 ====================

    /**
     * 收藏物品
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param callback 回调，返回是否成功
     */
    fun addFavorite(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        val favoriteId = "${userId}_$itemId"
        val favorite = mapOf(
            "userId" to userId,
            "itemId" to itemId,
            "createTime" to System.currentTimeMillis()
        )
        favoritesRef.child(favoriteId).setValue(favorite) { error, _ ->
            callback(error == null)
        }
    }

    /**
     * 取消收藏
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param callback 回调，返回是否成功
     */
    fun removeFavorite(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        val favoriteId = "${userId}_$itemId"
        favoritesRef.child(favoriteId).removeValue { error, _ ->
            callback(error == null)
        }
    }

    /**
     * 检查是否已收藏
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param callback 回调，返回是否已收藏
     */
    fun isFavorite(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        val favoriteId = "${userId}_$itemId"
        favoritesRef.child(favoriteId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            }
        )
    }

    /**
     * 获取用户收藏的物品列表
     * @param userId 用户ID
     * @param callback 回调，返回物品列表
     */
    fun getUserFavorites(userId: String, callback: (List<Item>) -> Unit) {
        favoritesRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val itemIds = mutableListOf<String>()
                    for (dataSnapshot in snapshot.children) {
                        val itemId = dataSnapshot.child("itemId").getValue(String::class.java)
                        itemId?.let { itemIds.add(it) }
                    }
                    if (itemIds.isEmpty()) {
                        callback(emptyList())
                        return
                    }
                    // 获取每个收藏物品的详情
                    val items = mutableListOf<Item>()
                    var loadedCount = 0
                    for (itemId in itemIds) {
                        getItem(itemId) { item ->
                            item?.let { items.add(it) }
                            loadedCount++
                            if (loadedCount == itemIds.size) {
                                items.sortByDescending { it.publishTime }
                                callback(items)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }
}
