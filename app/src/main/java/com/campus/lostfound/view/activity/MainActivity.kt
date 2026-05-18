package com.campus.lostfound.view.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.adapter.FirebasePostAdapter
import com.google.firebase.FirebaseApp

/**
 * 主页面（首页）- Firebase版本
 * 包含搜索、校区选择、轮播图、分类标签、物品列表和底部导航
 */
class MainActivity : BaseActivity() {

    // 日志标签
    private val TAG = "MainActivity"

    // 视图引用
    private lateinit var etSearch: EditText
    private lateinit var tvSelectedCampus: TextView
    private lateinit var tvAiMatch: TextView
    private lateinit var vpBanner: ViewPager2
    private lateinit var layoutDots: LinearLayout
    private lateinit var layoutCategoryTabs: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var contentHome: View
    private lateinit var fragmentProfile: View
    private lateinit var layoutTopBar: View
    private lateinit var fabPublish: ImageView
    private lateinit var ivHome: ImageView
    private lateinit var tvHome: TextView
    private lateinit var ivMine: ImageView
    private lateinit var tvMine: TextView
    
    // 数据访问对象（使用 Firebase）
    private lateinit var userManager: UserManager
    private lateinit var adapter: FirebasePostAdapter
    
    // 广播接收器，用于接收物品更新通知
    private lateinit var itemUpdateReceiver: android.content.BroadcastReceiver

    // 筛选条件
    private var currentSearchQuery = ""
    private var selectedCampus = "全部校区"
    private var selectedCategory: String? = null
    private var selectedTabIndex = 0

    // 分类列表（显示名称与数据库映射）
    private val categories = listOf("全部", "数码电子", "钱包证件", "图书文具", "生活用品", "衣物配饰")
    private val categoryMap = mapOf(
        "数码电子" to "电子产品", "钱包证件" to "钥匙钱包",
        "图书文具" to "书籍文具", "生活用品" to "生活用品",
        "衣物配饰" to "衣物配饰"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查 Firebase 初始化
        checkFirebaseInit()

        // 初始化数据访问对象
        userManager = UserManager(this)

        // 如果未登录，自动创建测试用户（使用 Firebase）
        if (!userManager.isLoggedIn()) {
            userManager.register("测试用户", "123456", "小明", "20240001", "文昌校区") { success, _ ->
                if (success) {
                    userManager.login("测试用户", "123456") { }
                }
            }
        }

        // 绑定视图引用
        etSearch = findViewById(R.id.etSearch)
        tvSelectedCampus = findViewById(R.id.tvSelectedCampus)
        tvAiMatch = findViewById(R.id.tvAiMatch)
        vpBanner = findViewById(R.id.vpBanner)
        layoutDots = findViewById(R.id.layoutDots)
        layoutCategoryTabs = findViewById(R.id.layoutCategoryTabs)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)
        contentHome = findViewById(R.id.contentHome)
        fragmentProfile = findViewById(R.id.fragmentProfile)
        layoutTopBar = findViewById(R.id.layoutTopBar)
        fabPublish = findViewById(R.id.fabPublish)
        ivHome = findViewById(R.id.ivHome)
        tvHome = findViewById(R.id.tvHome)
        ivMine = findViewById(R.id.ivMine)
        tvMine = findViewById(R.id.tvMine)

        // 初始化各功能模块
        setupCampusSelector()
        setupSearch()
        setupBanner()
        setupCategoryTabs()
        
        // 初始化广播接收器
        setupItemUpdateReceiver()
        setupRecyclerView()
        setupBottomBar()
        setupAiEntry()

        // 加载数据（从 Firebase）
        loadData()
    }

    /**
     * 检查 Firebase 是否初始化成功
     */
    private fun checkFirebaseInit() {
        try {
            val apps = FirebaseApp.getApps(this)
            if (apps.isNotEmpty()) {
                Log.d(TAG, "✅ Firebase 初始化成功")
            } else {
                Log.e(TAG, "❌ Firebase 未初始化")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase 初始化异常: ${e.message}")
        }
    }

    /**
     * 设置校区选择器：点击弹出校区选择弹窗
     */
    private fun setupCampusSelector() {
        tvSelectedCampus.setOnClickListener { showCampusPopup() }
    }

    /**
     * 显示校区选择弹窗
     */
    private fun showCampusPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_campus, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 12f
        popupWindow.showAsDropDown(tvSelectedCampus, -32, 8, Gravity.START)

        // 构建校区列表（包含"全部校区"选项）
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

    /**
     * 设置搜索功能：监听输入变化实时筛选
     */
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

    // 轮播图图片资源
    private val bannerImages = intArrayOf(R.drawable.banner_campus, R.drawable.banner_campus2)

    /**
     * 设置轮播图
     */
    private fun setupBanner() {
        vpBanner.adapter = BannerAdapter()
        vpBanner.setCurrentItem(500, false)  // 设置较大的起始位置，实现无限循环效果
        setupDots(0)
        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { 
                setupDots(position % bannerImages.size) 
            }
        })
    }

    /**
     * 设置轮播图指示器圆点
     * @param selected 当前选中的圆点索引
     */
    private fun setupDots(selected: Int) {
        layoutDots.removeAllViews()
        for (i in bannerImages.indices) {
            val dot = ImageView(this).apply {
                setImageResource(if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive)
                layoutParams = LinearLayout.LayoutParams(10, 10).apply { setMargins(5, 0, 5, 0) }
            }
            layoutDots.addView(dot)
        }
    }

    /**
     * 轮播图适配器
     */
    private inner class BannerAdapter : RecyclerView.Adapter<BannerAdapter.VH>() {
        // 设置1000个item实现无限循环
        override fun getItemCount() = 1000
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return VH(iv)
        }
        
        override fun onBindViewHolder(holder: VH, position: Int) { 
            holder.bind(bannerImages[position % bannerImages.size]) 
        }
        
        inner class VH(private val iv: ImageView) : RecyclerView.ViewHolder(iv) {
            fun bind(imageRes: Int) { iv.setImageResource(imageRes) }
        }
    }

    /**
     * 设置分类标签栏
     */
    private fun setupCategoryTabs() {
        layoutCategoryTabs.removeAllViews()
        categories.forEachIndexed { index, category ->
            val tv = TextView(this).apply {
                text = category
                textSize = 14f
                setPadding(24, 12, 24, 12)
                setTextColor(if (index == selectedTabIndex) getColor(R.color.primary) else getColor(R.color.text_secondary))
                setBackgroundResource(if (index == selectedTabIndex) R.drawable.bg_campus_tag else android.R.color.transparent)
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

    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = FirebasePostAdapter({ item ->
            // 点击跳转到详情页
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("itemId", item.id)
            intent.putExtra("itemType", item.type)
            startActivity(intent)
        }, userManager)

        // 设置长按删除事件
        adapter.setOnItemLongClickListener { item ->
            // 判断是否是当前用户发布的物品
            val currentUserId = userManager.getUserId()
            if (item.publisherId == currentUserId) {
                showDeleteConfirmDialog(item)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(item: Item) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除这条发布吗？")
            .setPositiveButton("删除") { _, _ ->
                // 从 Firebase 删除
                FirebaseHelper.deleteItem(item.id ?: "") { success ->
                    if (success) {
                        showToast("删除成功")
                        loadData()
                    } else {
                        showToast("删除失败")
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 加载数据（从 Firebase）
     */
    private fun loadData() {
        Log.d(TAG, "开始从 Firebase 加载数据")
        
        // 检查用户是否已登录
        if (!userManager.isLoggedIn()) {
            Log.e(TAG, "用户未登录，无法加载数据")
            tvEmpty.text = "请先登录"
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }
        
        // 如果有搜索关键词，使用搜索功能
        if (currentSearchQuery.isNotEmpty()) {
            FirebaseHelper.searchItems(currentSearchQuery) { items ->
                Log.d(TAG, "搜索结果: ${items.size} 条")
                updateRecyclerView(items)
            }
        } else {
            // 获取所有物品，然后根据分类筛选
            FirebaseHelper.getAllItems(null) { allItems ->
                Log.d(TAG, "从 Firebase 获取到 ${allItems.size} 条数据")
                // 根据分类筛选
                val filteredItems = if (selectedCategory != null) {
                    val filtered = allItems.filter { it.category == selectedCategory }
                    Log.d(TAG, "分类筛选后: ${filtered.size} 条")
                    filtered
                } else {
                    allItems
                }
                updateRecyclerView(filteredItems)
            }
        }
    }

    /**
     * 更新 RecyclerView 显示
     */
    private fun updateRecyclerView(items: List<Item>) {
        if (items.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (currentSearchQuery.isNotEmpty()) {
                "没有找到相关物品\n试试其他关键词吧~"
            } else {
                "暂无失物招领信息\n点击下方按钮发布第一条信息吧！"
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            adapter.setItems(items)
        }
    }

    /**
     * 设置底部导航栏
     */
    private fun setupBottomBar() {
        // 首页按钮
        val homeLayout = findViewById<LinearLayout>(R.id.tabHome)
        homeLayout.setOnClickListener {
            contentHome.visibility = View.VISIBLE
            fragmentProfile.visibility = View.GONE
            ivHome.setImageResource(R.drawable.ic_home_selected)
            tvHome.setTextColor(getColor(R.color.primary))
            ivMine.setImageResource(R.drawable.ic_mine_normal)
            tvMine.setTextColor(getColor(R.color.text_secondary))
        }

        // 我的按钮
        val mineLayout = findViewById<LinearLayout>(R.id.tabMine)
        mineLayout.setOnClickListener {
            contentHome.visibility = View.GONE
            fragmentProfile.visibility = View.VISIBLE
            ivHome.setImageResource(R.drawable.ic_home_normal)
            tvHome.setTextColor(getColor(R.color.text_secondary))
            ivMine.setImageResource(R.drawable.ic_mine_selected)
            tvMine.setTextColor(getColor(R.color.primary))
        }

        // 发布按钮
        fabPublish.setOnClickListener {
            startActivity(Intent(this, PublishActivity::class.java))
        }
    }

    /**
     * 设置 AI 智能匹配入口
     */
    private fun setupAiEntry() {
        tvAiMatch.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }

    /**
     * 设置物品更新广播接收器
     */
    private fun setupItemUpdateReceiver() {
        itemUpdateReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                Log.d(TAG, "收到物品更新广播，开始刷新数据")
                // 延迟500ms刷新，给Firebase数据同步时间
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    loadData()
                }, 500)
            }
        }
        
        // 注册广播接收器（Android 12+ 需要指定导出标志）
        val filter = android.content.IntentFilter("com.campus.lostfound.ACTION_ITEM_UPDATED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(itemUpdateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(itemUpdateReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        // 返回页面时刷新数据
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销广播接收器
        unregisterReceiver(itemUpdateReceiver)
    }
}
