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
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.adapter.PostAdapter

/**
 * 我的发布页面
 * 展示当前用户发布的所有失物招领信息，支持长按删除功能
 */
class MyPublishActivity : BaseActivity() {

    // 物品数据访问对象
    private lateinit var itemDao: ItemDao
    // 用户管理器
    private lateinit var userManager: UserManager
    // 列表适配器
    private lateinit var adapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_publish)
        // 设置页面标题
        title = "我的发布"

        // 初始化数据访问对象
        itemDao = ItemDao(this)
        userManager = UserManager(this)

        // 获取视图引用
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        // 创建并配置适配器
        adapter = PostAdapter({ item ->
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
     * 删除物品
     * @param item 要删除的物品
     */
    private fun deleteItem(item: Item) {
        // 调用ItemDao删除物品
        val success = itemDao.delete(item.id) > 0
        if (success) {
            // 删除成功
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
            // 刷新列表
            loadData()
        } else {
            // 删除失败
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 加载用户发布的物品列表
     */
    private fun loadData() {
        // 查询当前用户发布的物品
        val items = itemDao.queryByPublisher(userManager.getCurrentUser())
        // 更新适配器数据
        adapter.setItems(items)

        // 获取视图引用
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        // 根据数据是否为空显示/隐藏对应视图
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        // 停止刷新动画
        swipeRefresh.isRefreshing = false
    }

    /**
     * 页面恢复时重新加载数据
     */
    override fun onResume() {
        super.onResume()
        loadData()
    }
}