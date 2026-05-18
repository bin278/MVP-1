package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.campus.lostfound.R
import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.adapter.FirebasePostAdapter

/**
 * 我的发布页面
 * 展示当前用户发布的所有失物招领信息，支持长按删除功能
 * 使用 Firebase Realtime Database 存储数据
 */
class MyPublishActivity : BaseActivity() {

    // 用户管理器
    private lateinit var userManager: UserManager
    // 列表适配器
    private lateinit var adapter: FirebasePostAdapter
    // 是否正在加载中
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_publish)
        // 设置页面标题
        title = "我的发布"

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

        // 设置长按删除监听
        adapter.setOnItemLongClickListener { item ->
            // 显示长按提示（调试用）
            Toast.makeText(this, "长按删除：${item.name}", Toast.LENGTH_SHORT).show()
            // 显示确认删除对话框
            showDeleteConfirmDialog(item)
        }

        // 配置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 设置下拉刷新监听
        swipeRefresh.setOnRefreshListener { loadData() }

        // 加载数据
        loadData()
    }

    /**
     * 显示删除确认对话框
     * @param item 要删除的物品
     */
    private fun showDeleteConfirmDialog(item: Item) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.delete_msg)
            .setPositiveButton(R.string.confirm) { _, _ ->
                // 确认删除
                deleteItem(item)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * 删除物品（使用 Firebase）
     * @param item 要删除的物品
     */
    private fun deleteItem(item: Item) {
        val itemId = item.id ?: return
        
        // 显示加载状态
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.isRefreshing = true

        // 调用 FirebaseHelper 删除物品
        FirebaseHelper.deleteItem(itemId) { success ->
            if (success) {
                // 删除成功
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                // 刷新列表
                loadData()
            } else {
                // 删除失败
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    /**
     * 加载用户发布的物品列表（使用 Firebase）
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

        // 使用 Firebase 获取用户发布的物品
        FirebaseHelper.getItemsByUser(userId) { items ->
            // 更新适配器数据
            adapter.setItems(items)

            // 根据数据是否为空显示/隐藏对应视图
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            
            // 设置空状态提示
            if (items.isEmpty()) {
                tvEmpty.text = "你还没有发布任何物品\n点击底部按钮发布吧！"
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