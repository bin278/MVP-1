package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.util.TimeUtil
import com.campus.lostfound.view.adapter.PostAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : BaseActivity() {

    private lateinit var etSearch: EditText
    private lateinit var tvSelectedCampus: TextView
    private lateinit var vpBanner: ViewPager2
    private lateinit var layoutDots: LinearLayout
    private lateinit var layoutCategoryTabs: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabPublish: FloatingActionButton
    private lateinit var itemDao: ItemDao
    private lateinit var userManager: UserManager
    private lateinit var adapter: PostAdapter

    private var currentSearchQuery: String = ""
    private var selectedCampus: String = "全部校区"
    private var selectedCategory: String? = null
    private var selectedTabIndex = 0

    private val categories = listOf("全部", "数码电子", "钱包证件", "图书文具", "生活用品", "衣物配饰", "运动器材", "其他")
    private val categoryMap = mapOf(
        "数码电子" to "电子产品", "钱包证件" to "钥匙钱包",
        "图书文具" to "书籍文具", "生活用品" to "生活用品",
        "衣物配饰" to "衣物配饰", "运动器材" to "运动器材", "其他" to "其他"
    )

    private val bannerColors = arrayOf(
        R.color.primary, R.color.primary_dark,
        R.color.accent, R.color.primary_gradient_start
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        itemDao = ItemDao(this)
        userManager = UserManager(this)

        if (!userManager.isLoggedIn()) {
            userManager.register("测试用户", "123456", "小明", "20240001", "大学城校区")
            userManager.login("测试用户", "123456")
        }
        addTestData()

        etSearch = findViewById(R.id.etSearch)
        tvSelectedCampus = findViewById(R.id.tvSelectedCampus)
        vpBanner = findViewById(R.id.vpBanner)
        layoutDots = findViewById(R.id.layoutDots)
        layoutCategoryTabs = findViewById(R.id.layoutCategoryTabs)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)
        bottomNav = findViewById(R.id.bottomNav)
        fabPublish = findViewById(R.id.fabPublish)

        setupCampusSelector()
        setupSearch()
        setupBanner()
        setupCategoryTabs()
        setupRecyclerView()
        setupBottomNav()
        setupFab()

        loadData()
    }

    private fun setupCampusSelector() {
        tvSelectedCampus.setOnClickListener {
            showCampusPopup()
        }
    }

    private fun showCampusPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_campus, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.elevation = 12f
        popupWindow.showAsDropDown(tvSelectedCampus, -32, 8, Gravity.START)

        val campusList = mutableListOf("全部校区")
        campusList.addAll(Constants.CAMPUSES)

        val container = popupView.findViewById<LinearLayout>(R.id.container)
        for (campus in campusList) {
            val tv = TextView(this).apply {
                text = campus
                textSize = 14f
                setTextColor(getColor(R.color.text_primary))
                setPadding(48, 16, 48, 16)
                setOnClickListener {
                    selectedCampus = campus
                    tvSelectedCampus.text = campus
                    popupWindow.dismiss()
                    loadData()
                }
            }
            container.addView(tv)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString() ?: ""
                loadData()
            }
        })
    }

    private fun setupBanner() {
        val bannerAdapter = BannerAdapter(bannerColors.size)
        vpBanner.adapter = bannerAdapter
        setupDots(0)
        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
            }
        })
    }

    private fun setupDots(selected: Int) {
        layoutDots.removeAllViews()
        for (i in 0 until bannerColors.size) {
            val dot = ImageView(this).apply {
                setImageResource(if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive)
                layoutParams = LinearLayout.LayoutParams(12, 12).apply {
                    setMargins(6, 0, 6, 0)
                }
            }
            layoutDots.addView(dot)
        }
    }

    private inner class BannerAdapter(private val count: Int) :
        RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                textSize = 18f
                setTextColor(getColor(R.color.white))
            }
            return BannerViewHolder(tv)
        }

        override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = count

        inner class BannerViewHolder(private val tv: TextView) : RecyclerView.ViewHolder(tv) {
            fun bind(position: Int) {
                tv.setBackgroundColor(getColor(bannerColors[position]))
                tv.text = when (position) {
                    0 -> "📢 校园失物招领\n让每件遗失物品找到归宿"
                    1 -> "🔍 快速查找\n搜索失物，一键发布"
                    2 -> "📍 精准定位\n地图导航，快速取回"
                    else -> "💙 诚信校园\n拾金不昧，互帮互助"
                }
            }
        }
    }

    private fun setupCategoryTabs() {
        layoutCategoryTabs.removeAllViews()
        for ((index, category) in categories.withIndex()) {
            val tv = TextView(this).apply {
                text = category
                textSize = 14f
                setPadding(20, 12, 20, 12)
                if (index == selectedTabIndex) {
                    setTextColor(getColor(R.color.primary))
                    paintFlags = paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
                } else {
                    setTextColor(getColor(R.color.text_secondary))
                    paintFlags = paintFlags and android.graphics.Paint.FAKE_BOLD_TEXT_FLAG.inv()
                }
                setOnClickListener {
                    selectedTabIndex = index
                    selectedCategory = if (index == 0) null else categoryMap[category]
                    setupCategoryTabs()
                    loadData()
                }
            }
            layoutCategoryTabs.addView(tv)
        }
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter({ item ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("item_id", item.id)
            startActivity(intent)
        }, userManager)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupBottomNav() {
        bottomNav.menu.findItem(R.id.nav_home)?.isChecked = true
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_publish -> {
                    startActivity(Intent(this, PublishActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> true
            }
        }
    }

    private fun setupFab() {
        fabPublish.setOnClickListener {
            startActivity(Intent(this, PublishActivity::class.java))
        }
    }

    private fun loadData() {
        val allItems = itemDao.queryAll()
        val filtered = allItems.filter { item ->
            val matchesSearch = currentSearchQuery.isEmpty() ||
                item.name.contains(currentSearchQuery, ignoreCase = true) ||
                item.description.contains(currentSearchQuery, ignoreCase = true) ||
                item.addressText.contains(currentSearchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesCampus = selectedCampus == "全部校区" ||
                userManager.getUserInfo(item.publisher).campus == selectedCampus
            matchesSearch && matchesCategory && matchesCampus
        }
        adapter.setItems(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadData()
    }

    private fun addTestData() {
        val existingItems = itemDao.queryAll()
        if (existingItems.isNotEmpty()) return

        val testItems = listOf(
            Item(type = Constants.ITEM_TYPE_LOST, name = "黑色钱包", category = "钥匙钱包",
                location = "图书馆二楼", time = "2026-05-10", contact = "13800138001",
                description = "黑色皮质钱包，内有身份证、银行卡和现金若干",
                publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 3600),
                addressText = "图书馆二楼自习区"),
            Item(type = Constants.ITEM_TYPE_FOUND, name = "学生证", category = "证件卡片",
                location = "操场看台", time = "2026-05-10", contact = "13900139001",
                description = "在操场看台捡到一张学生证，计算机学院 李明同学",
                publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 1800),
                addressText = "操场看台"),
            Item(type = Constants.ITEM_TYPE_LOST, name = "蓝牙耳机", category = "电子产品",
                location = "教学楼A座", time = "2026-05-11", contact = "13800138002",
                description = "白色AirPods Pro，带蓝色保护壳，可能在A座301教室遗失",
                publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 7200),
                addressText = "教学楼A座301"),
            Item(type = Constants.ITEM_TYPE_FOUND, name = "高等数学课本", category = "书籍文具",
                location = "自习室", time = "2026-05-11", contact = "13900139002",
                description = "同济版高等数学上册，书内有详细笔记",
                publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 5400),
                addressText = "第一自习室"),
            Item(type = Constants.ITEM_TYPE_LOST, name = "校园卡", category = "证件卡片",
                location = "食堂一楼", time = "2026-05-12", contact = "13800138003",
                description = "校园一卡通，卡号后四位8832，在食堂一楼遗失",
                publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 10800),
                addressText = "第一食堂"),
            Item(type = Constants.ITEM_TYPE_FOUND, name = "运动水杯", category = "生活用品",
                location = "体育馆", time = "2026-05-12", contact = "13900139003",
                description = "绿色运动水杯，品牌李宁，在体育馆更衣室捡到",
                publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 9000),
                addressText = "体育馆更衣室")
        )
        testItems.forEach { itemDao.insert(it) }
    }
}