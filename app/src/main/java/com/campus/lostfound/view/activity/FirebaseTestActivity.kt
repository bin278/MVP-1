package com.campus.lostfound.view.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.campus.lostfound.R
import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.Item
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

/**
 * Firebase 连接测试页面
 * 用于验证 Firebase 数据库连接是否正常
 */
class FirebaseTestActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnTest: Button
    private val TAG = "FirebaseTest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase_test)

        tvStatus = findViewById(R.id.tv_status)
        btnTest = findViewById(R.id.btn_test)

        // 日志：Activity 创建成功
        Log.d(TAG, "FirebaseTestActivity 创建成功")

        // 检查 Firebase 是否初始化
        checkFirebaseInit()

        btnTest.setOnClickListener {
            testFirebaseConnection()
        }
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
                Log.d(TAG, "应用ID: ${app.options.applicationId}")
                Log.d(TAG, "API Key: ${app.options.apiKey}")
                tvStatus.text = "Firebase 初始化成功\n应用ID: ${app.options.applicationId}"
            } else {
                Log.e(TAG, "❌ Firebase 未初始化")
                tvStatus.text = "❌ Firebase 未初始化，请检查配置"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase 初始化异常: ${e.message}")
            tvStatus.text = "❌ Firebase 初始化异常: ${e.message}"
        }
    }

    /**
     * 测试 Firebase 连接
     */
    private fun testFirebaseConnection() {
        tvStatus.text = "正在连接 Firebase..."
        Log.d(TAG, "开始测试 Firebase 连接")

        // 设置持久化（可选）
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // 测试1：获取物品列表
        FirebaseHelper.getAllItems(null) { items ->
            if (items.isNotEmpty()) {
                Log.d(TAG, "✅ 成功获取 ${items.size} 条物品数据")
                tvStatus.text = "✅ 连接成功！\n找到 ${items.size} 条物品数据"
            } else {
                Log.d(TAG, "数据库为空，尝试添加测试数据")
                tvStatus.text = "⚠️ 数据库为空，尝试添加测试数据..."
                addTestData()
            }
        }
    }

    /**
     * 添加测试数据到 Firebase
     */
    private fun addTestData() {
        val testItem = Item(
            type = "lost",
            name = "测试物品",
            description = "这是一个测试物品",
            location = "测试地点",
            contact = "13800138000",
            publisherId = "test_user",
            publisherName = "测试用户"
        )

        Log.d(TAG, "准备添加测试物品")

        FirebaseHelper.addItem(testItem) { itemId ->
            if (itemId != null) {
                Log.d(TAG, "✅ 添加成功！物品ID: $itemId")
                tvStatus.text = "✅ 添加成功！物品ID: $itemId"
            } else {
                Log.e(TAG, "❌ 添加失败，请检查配置")
                tvStatus.text = "❌ 连接失败，请检查配置\n1. 确认 google-services.json 正确\n2. 确认 Firebase 数据库已启用\n3. 确认网络连接正常"
            }
        }
    }
}
