package com.campus.lostfound.view.activity

import android.content.Intent
import android.graphics.Color
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

/**
 * 主页面（首页）
 * 包含搜索、校区选择、轮播图、分类标签、物品列表和底部导航
 */
class MainActivity : BaseActivity() {

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
    
    // 数据访问对象
    private lateinit var itemDao: ItemDao
    private lateinit var userManager: UserManager
    private lateinit var adapter: PostAdapter

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

        // 初始化数据访问对象
        itemDao = ItemDao(this)
        userManager = UserManager(this)

        // 如果未登录，自动创建测试用户
        if (!userManager.isLoggedIn()) {
            userManager.register("测试用户", "123456", "小明", "20240001", "文昌校区")
            userManager.login("测试用户", "123456")
        }
        // 添加测试数据（首次安装时）
        addTestData()

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
        setupRecyclerView()
        setupBottomBar()
        setupAiEntry()

        // 加载数据
        loadData()
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
        for ((index, category) in categories.withIndex()) {
            // 创建标签容器
            val wrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                gravity = Gravity.CENTER
            }
            // 创建标签文字
            val tv = TextView(this).apply {
                text = category
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(4, 10, 4, 6)
                // 设置选中/未选中状态
                if (index == selectedTabIndex) {
                    setTextColor(getColor(R.color.primary))
                    paintFlags = paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
                } else {
                    setTextColor(getColor(R.color.text_secondary))
                }
            }
            // 创建底部下划线
            val underline = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24, 3).apply { gravity = Gravity.CENTER_HORIZONTAL }
                setBackgroundColor(if (index == selectedTabIndex) getColor(R.color.primary) else Color.TRANSPARENT)
            }
            // 组装标签
            wrapper.addView(tv)
            wrapper.addView(underline)
            // 设置点击事件
            wrapper.setOnClickListener {
                selectedTabIndex = index
                selectedCategory = if (index == 0) null else categoryMap[category]
                setupCategoryTabs()
                loadData()
            }
            layoutCategoryTabs.addView(wrapper)
        }
    }

    /**
     * 设置物品列表RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = PostAdapter({ item ->
            // 点击跳转到详情页
            startActivity(Intent(this, DetailActivity::class.java).putExtra("item_id", item.id))
        }, userManager)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    /**
     * 设置底部导航栏
     * 通过显示/隐藏 contentHome 和 fragmentProfile 实现页内切换
     */
    private fun setupBottomBar() {
        val tabHome = findViewById<LinearLayout>(R.id.tabHome)
        val tabMine = findViewById<LinearLayout>(R.id.tabMine)

        // 默认首页高亮
        highlightTab(true)

        // 首页点击事件
        tabHome.setOnClickListener {
            layoutTopBar.visibility = View.VISIBLE
            layoutTopBar.setBackgroundColor(getColor(android.R.color.white))
            // 显示顶部栏所有子视图
            val bar = layoutTopBar as LinearLayout
            for (i in 0 until bar.childCount) { 
                bar.getChildAt(i).visibility = View.VISIBLE 
            }
            contentHome.visibility = View.VISIBLE
            fragmentProfile.visibility = View.GONE
            highlightTab(true)
        }

        // "我的"页点击事件
        tabMine.setOnClickListener {
            layoutTopBar.visibility = View.GONE
            // 隐藏子view避免残留
            val bar = layoutTopBar as LinearLayout
            for (i in 0 until bar.childCount) { 
                bar.getChildAt(i).visibility = View.INVISIBLE 
            }
            contentHome.visibility = View.GONE
            fragmentProfile.visibility = View.VISIBLE
            highlightTab(false)
        }

        // 发布按钮点击事件
        fabPublish.setOnClickListener {
            startActivity(Intent(this, PublishActivity::class.java))
        }
    }

    /**
     * 高亮底部导航标签
     * @param isHome 是否高亮首页标签
     */
    private fun highlightTab(isHome: Boolean) {
        val primary = getColor(R.color.primary)
        val secondary = getColor(R.color.text_secondary)
        if (isHome) {
            ivHome.setColorFilter(primary)
            tvHome.setTextColor(primary)
            ivMine.setColorFilter(secondary)
            tvMine.setTextColor(secondary)
        } else {
            ivMine.setColorFilter(primary)
            tvMine.setTextColor(primary)
            ivHome.setColorFilter(secondary)
            tvHome.setTextColor(secondary)
        }
    }

    /**
     * 设置AI智能匹配入口
     */
    private fun setupAiEntry() {
        tvAiMatch.setOnClickListener { 
            startActivity(Intent(this, ChatActivity::class.java)) 
        }
    }

    /**
     * 加载并筛选物品数据
     */
    private fun loadData() {
        val allItems = itemDao.queryAll()
        // 根据搜索关键词、分类、校区筛选
        val filtered = allItems.filter { item ->
            // 搜索匹配
            val matchesSearch = currentSearchQuery.isEmpty() ||
                item.name.contains(currentSearchQuery, ignoreCase = true) ||
                item.description.contains(currentSearchQuery, ignoreCase = true) ||
                item.addressText.contains(currentSearchQuery, ignoreCase = true)
            // 分类匹配
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            // 校区匹配
            val matchesCampus = selectedCampus == "全部校区" ||
                userManager.getUserInfo(item.publisher).campus == selectedCampus
            
            matchesSearch && matchesCategory && matchesCampus
        }
        // 更新列表数据
        adapter.setItems(filtered)
        // 显示/隐藏空状态提示
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * 页面恢复时重新加载数据
     */
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadData()
    }

    /**
     * 添加测试数据（首次安装时执行）
     */
    private fun addTestData() {
        // 如果已有数据则跳过
        if (itemDao.queryAll().isNotEmpty()) return
        
        val now = System.currentTimeMillis()
        // 创建测试物品列表
        listOf(
            Item(
                type = Constants.ITEM_TYPE_LOST, 
                name = "黑色钱包", 
                category = "钥匙钱包", 
                location = "图书馆二楼", 
                time = "2026-05-10", 
                contact = "13800138001", 
                description = "黑色皮质钱包，内有身份证和银行卡", 
                publisher = "测试用户", 
                publishTime = TimeUtil.formatTimestamp(now - 3600_000), 
                addressText = "图书馆二楼自习区"
            ),
            Item(
                type = Constants.ITEM_TYPE_FOUND, 
                name = "学生证", 
                category = "证件卡片", 
                location = "操场看台", 
                time = "2026-05-10", 
                contact = "13900139001", 
                description = "计算机学院 李明同学的学生证", 
                publisher = "测试用户", 
                publishTime = TimeUtil.formatTimestamp(now - 1800_000), 
                addressText = "操场看台"
            ),
            Item(
                type = Constants.ITEM_TYPE_LOST, 
                name = "蓝牙耳机", 
                category = "电子产品", 
                location = "教学楼A座", 
                time = "2026-05-11", 
                contact = "13800138002", 
                description = "白色AirPods Pro，带蓝色保护壳", 
                publisher = "测试用户", 
                publishTime = TimeUtil.formatTimestamp(now - 7200_000), 
                addressText = "教学楼A座301"
            ),
            Item(
                type = Constants.ITEM_TYPE_FOUND, 
                name = "高等数学课本", 
                category = "书籍文具", 
                location = "自习室", 
                time = "2026-05-11", 
                contact = "13900139002", 
                description = "同济版高等数学上册，书内有详细笔记", 
                publisher = "测试用户", 
                publishTime = TimeUtil.formatTimestamp(now - 5400_000), 
                addressText = "第一自习室"
            ),
            Item(
                type = Constants.ITEM_TYPE_LOST, 
                name = "校园卡", 
                category = "证件卡片", 
                location = "食堂一楼", 
                time = "2026-05-12", 
                contact = "13800138003", 
                description = "校园一卡通，卡号后四位8832", 
                publisher = "测试用户", 
                publishTime = TimeUtil.formatTimestamp(now - 10800_000), 
                addressText = "第一食堂"
            ),
            Item(
                type = Constants.ITEM_TYPE_FOUND, 
                name = "运动水杯", 
                category = "生活用品", 
                location = "体育馆", 
                time = "2026-05-12", 
                contact = "13900139003", 
                description = "绿色运动水杯，品牌李宁，在体育馆更衣室捡到", 
                publisher = "测试用户", 
                publishTime = TimeUtil.formatTimestamp(now - 9000_000), 
                addressText = "体育馆更衣室"
            )
        ).forEach { itemDao.insert(it) }
    }
}