package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campus.lostfound.R
import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.Item
import com.campus.lostfound.firebase.User
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.adapter.FirebasePostAdapter
import com.google.firebase.FirebaseApp

/**
 * Firebase 测试主页面
 * 用于验证 Firebase 数据库连接和数据展示
 */
class FirebaseMainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var btnAddTest: Button
    private lateinit var adapter: FirebasePostAdapter
    private val TAG = "FirebaseMain"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "========== onCreate 开始 ==========")
        Log.d(TAG, "当前线程: ${Thread.currentThread().name}")
        
        setContentView(R.layout.activity_firebase_main)
        Log.d(TAG, "布局加载完成")

        recyclerView = findViewById(R.id.recyclerView)
        tvStatus = findViewById(R.id.tvStatus)
        btnAddTest = findViewById(R.id.btnAddTest)
        Log.d(TAG, "视图绑定完成")

        // 初始化适配器
        adapter = FirebasePostAdapter({ item ->
            Log.d(TAG, "点击物品: ${item.name}")
        }, UserManager(this))
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        Log.d(TAG, "适配器初始化完成")

        // 检查 Firebase 初始化
        Log.d(TAG, "开始检查 Firebase 初始化")
        checkFirebaseInit()

        // 加载数据
        Log.d(TAG, "开始加载数据")
        loadData()

        // 添加测试数据按钮
        btnAddTest.setOnClickListener {
            addTestData()
        }
        Log.d(TAG, "========== onCreate 结束 ==========")
    }

    /**
     * 检查 Firebase 是否初始化成功
     */
    private fun checkFirebaseInit() {
        try {
            val apps = FirebaseApp.getApps(this)
            Log.d(TAG, "Firebase 应用数量: ${apps.size}")
            
            if (apps.isNotEmpty()) {
                val app = apps[0]
                Log.d(TAG, "✅ Firebase 初始化成功")
                Log.d(TAG, "应用名称: ${app.name}")
                Log.d(TAG, "应用ID: ${app.options.applicationId}")
                tvStatus.text = "✅ Firebase 连接成功"
            } else {
                Log.e(TAG, "❌ Firebase 未初始化")
                tvStatus.text = "❌ Firebase 未初始化"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase 初始化异常: ${e.message}")
            tvStatus.text = "❌ Firebase 初始化异常: ${e.message}"
        }
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        FirebaseHelper.getAllItems(null) { items ->
            Log.d(TAG, "加载到 ${items.size} 条数据")
            adapter.setItems(items)
            
            if (items.isEmpty()) {
                tvStatus.text = "当前数据库为空，请添加测试数据"
            }
        }
    }

    /**
     * 添加测试数据
     */
    private fun addTestData() {
        Log.d(TAG, "开始添加测试数据")
        
        // 创建测试用户
        val testUser = User(
            username = "testuser",
            password = "123456",
            nickname = "测试用户",
            studentId = "20240001",
            campus = "文昌校区"
        )

        // 注册用户
        FirebaseHelper.register(testUser) { success, userId ->
            if (success && userId != null) {
                Log.d(TAG, "用户注册成功: $userId")
                
                // 创建测试物品
                val testItems = listOf(
                    Item(
                        type = "lost",
                        name = "iPhone 15 Pro",
                        category = "电子产品",
                        location = "图书馆三楼",
                        campus = "文昌校区",
                        time = "2024-01-15",
                        phone = "13800138000",
                        description = "黑色 iPhone 15 Pro，屏幕有轻微划痕",
                        publisherId = userId,
                        publisherName = "测试用户"
                    ),
                    Item(
                        type = "found",
                        name = "学生证",
                        category = "钥匙钱包",
                        location = "食堂一楼",
                        campus = "文昌校区",
                        time = "2024-01-14",
                        phone = "13900139000",
                        description = "姓名：张三，学号：20240002",
                        publisherId = userId,
                        publisherName = "测试用户"
                    )
                )

                var addedCount = 0
                testItems.forEach { item ->
                    FirebaseHelper.addItem(item) { itemId ->
                        if (itemId != null) {
                            Log.d(TAG, "物品添加成功: $itemId")
                            addedCount++
                            if (addedCount == testItems.size) {
                                Log.d(TAG, "✅ 测试数据添加成功！")
                                tvStatus.text = "✅ 测试数据添加成功！"
                                loadData()
                            }
                        } else {
                            Log.e(TAG, "物品添加失败")
                        }
                    }
                }
            } else {
                Log.e(TAG, "用户注册失败")
                tvStatus.text = "用户注册失败，可能已存在"
                // 用户可能已存在，直接添加物品
                FirebaseHelper.login("testuser", "123456") { user ->
                    if (user != null) {
                        addTestItems(user.id ?: "")
                    }
                }
            }
        }
    }

    /**
     * 添加测试物品（用户已存在时）
     */
    private fun addTestItems(userId: String) {
        val testItems = listOf(
            Item(
                type = "lost",
                name = "蓝牙耳机",
                category = "电子产品",
                location = "教学楼A栋",
                campus = "文昌校区",
                time = "2024-01-16",
                phone = "13800138000",
                description = "白色 AirPods Pro",
                publisherId = userId,
                publisherName = "测试用户"
            )
        )

        testItems.forEach { item ->
            FirebaseHelper.addItem(item) { itemId ->
                if (itemId != null) {
                    Log.d(TAG, "物品添加成功: $itemId")
                    tvStatus.text = "✅ 测试数据添加成功！"
                    loadData()
                }
            }
        }
    }
}
