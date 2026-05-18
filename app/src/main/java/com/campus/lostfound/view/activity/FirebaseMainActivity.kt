itemDao = ItemDao(this)package com.campus.lostfound.view.activity

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
        }, com.campus.lostfound.sharedpref.UserManager(this))
        
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
            if (apps.isNotEmpty()) {
                val app = apps[0]
                Log.d(TAG, "✅ Firebase 初始化成功")
                Log.d(TAG, "应用名称: ${app.name}")
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
     * 从 Firebase 加载数据
     */
    private fun loadData() {
        Log.d(TAG, "开始从 Firebase 加载数据")
        tvStatus.text = "正在加载数据..."

        FirebaseHelper.getAllItems(null) { items ->
            Log.d(TAG, "加载到 ${items.size} 条数据")
            adapter.setItems(items)
            tvStatus.text = if (items.isEmpty()) {
                "数据库为空，点击下方按钮添加测试数据"
            } else {
                "✅ 加载成功，共 ${items.size} 条数据"
            }
        }
    }

    /**
     * 添加测试数据到 Firebase
     */
    private fun addTestData() {
        tvStatus.text = "正在添加测试数据..."
        
        // 创建测试用户
        val testUser = User(
            username = "testuser",
            password = "123456",
            nickname = "测试用户",
            phone = "13800138000"
        )

        FirebaseHelper.register(testUser) { success, userId ->
            if (success && userId != null) {
                Log.d(TAG, "用户注册成功: $userId")
                
                // 创建测试物品
                val testItem = Item(
                    type = "lost",
                    name = "校园卡",
                    description = "丢失了一张校园卡，内有身份证和银行卡",
                    location = "图书馆二楼",
                    contact = "13800138000",
                    publisherId = userId,
                    publisherName = "测试用户"
                )

                FirebaseHelper.addItem(testItem) { itemId ->
                    if (itemId != null) {
                        Log.d(TAG, "物品添加成功: $itemId")
                        tvStatus.text = "✅ 测试数据添加成功！物品ID: $itemId"
                        // 重新加载数据
                        loadData()
                    } else {
                        tvStatus.text = "❌ 添加失败"
                    }
                }
            } else {
                // 用户已存在，直接添加物品
                Log.d(TAG, "用户可能已存在，直接添加物品")
                val testItem = Item(
                    type = "found",
                    name = "学生证",
                    description = "在操场捡到的学生证",
                    location = "操场看台",
                    contact = "13900139000",
                    publisherId = "test_user_id",
                    publisherName = "测试用户"
                )
                FirebaseHelper.addItem(testItem) { itemId ->
                    if (itemId != null) {
                        tvStatus.text = "✅ 测试数据添加成功！物品ID: $itemId"
                        loadData()
                    }
                }
            }
        }
    }
}
