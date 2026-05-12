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
import com.campus.lostfound.db.FavoriteDao
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.adapter.ItemAdapter

class MyFavoriteActivity : BaseActivity() {

    private lateinit var favoriteDao: FavoriteDao
    private lateinit var userManager: UserManager
    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_favorite)
        title = "我的收藏"

        favoriteDao = FavoriteDao(this)
        userManager = UserManager(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        adapter = ItemAdapter { item ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("item_id", item.id)
            startActivity(intent)
        }

        adapter.setOnItemLongClickListener { item ->
            AlertDialog.Builder(this)
                .setTitle("取消收藏")
                .setMessage("确认取消收藏「${item.name}」？")
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    favoriteDao.removeFavorite(userManager.getCurrentUser(), item.id)
                    Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show()
                    loadData()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    private fun loadData() {
        val items = favoriteDao.getFavoritesByUsername(userManager.getCurrentUser())
        adapter.setItems(items)

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        swipeRefresh.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}
