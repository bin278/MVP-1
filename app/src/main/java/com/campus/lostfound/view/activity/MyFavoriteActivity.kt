package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.campus.lostfound.R
import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.adapter.FirebasePostAdapter

/**
 * 我的收藏页面
 * 展示当前用户收藏的所有失物招领信息
 * 使用 Firebase Realtime Database 存储数据
 */
class MyFavoriteActivity : BaseActivity() {

    // 用户管理器
    private lateinit var userManager: UserManager
    // 列表适配器
    private lateinit var adapter: FirebasePostAdapter
    // 是否正在加载中
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_favorite)
        title = "我的收藏"

        // 初始化用户管理器
        userManager = UserManager(this)

        // 获取视图引用
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        // 创建并配置适配器（使用 Firebase 版本）
        adapter = FirebasePostAdapter({ item ->
            // 点击跳转到物品详情页
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("item_id", item.id)
            startActivity(intent)
        }, userManager)

        // 配置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 设置下拉刷新监听
        swipeRefresh.setOnRefreshListener { loadData() }

        // 加载数据
        loadData()
    }

    /**
     * 加载用户收藏的物品列表（使用 Firebase）
     */
    private fun loadData() {
        if (isLoading) return
        
        isLoading = true
        
        // 获取当前用户ID
        val userId = userManager.getUserId()
        
        // 获取视图引用
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        // 使用 Firebase 获取用户收藏的物品
        FirebaseHelper.getUserFavorites(userId) { items ->
            // 更新适配器数据
            adapter.setItems(items)

            // 根据数据是否为空显示/隐藏对应视图
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            
            // 设置空状态提示
            if (items.isEmpty()) {
                tvEmpty.text = "你还没有收藏任何物品\n看到感兴趣的物品就收藏吧！"
            }
            
            // 停止刷新动画
            swipeRefresh.isRefreshing = false
            isLoading = false
        }
    }

    /**
     * 页面恢复时重新加载数据
     */
    override fun onResume() {
        super.onResume()
        loadData()
    }
}