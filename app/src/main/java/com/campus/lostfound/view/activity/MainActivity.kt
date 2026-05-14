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

class MainActivity : BaseActivity() {

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
    private lateinit var itemDao: ItemDao
    private lateinit var userManager: UserManager
    private lateinit var adapter: PostAdapter

    private var currentSearchQuery = ""
    private var selectedCampus = "全部校区"
    private var selectedCategory: String? = null
    private var selectedTabIndex = 0

    private val categories = listOf("全部", "数码电子", "钱包证件", "图书文具", "生活用品", "衣物配饰")
    private val categoryMap = mapOf(
        "数码电子" to "电子产品", "钱包证件" to "钥匙钱包",
        "图书文具" to "书籍文具", "生活用品" to "生活用品",
        "衣物配饰" to "衣物配饰"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        itemDao = ItemDao(this)
        userManager = UserManager(this)

        if (!userManager.isLoggedIn()) {
            userManager.register("测试用户", "123456", "小明", "20240001", "文昌校区")
            userManager.login("测试用户", "123456")
        }
        addTestData()

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

        setupCampusSelector()
        setupSearch()
        setupBanner()
        setupCategoryTabs()
        setupRecyclerView()
        setupBottomBar()
        setupAiEntry()

        loadData()
    }

    private fun setupCampusSelector() {
        tvSelectedCampus.setOnClickListener { showCampusPopup() }
    }

    private fun showCampusPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_campus, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 12f
        popupWindow.showAsDropDown(tvSelectedCampus, -32, 8, Gravity.START)

        val campusList = mutableListOf("全部校区")
        campusList.addAll(Constants.CAMPUSES)
        val container = popupView.findViewById<LinearLayout>(R.id.container)
        for (campus in campusList) {
            val tv = TextView(this).apply {
                text = campus; textSize = 14f; setTextColor(getColor(R.color.text_primary)); setPadding(48, 16, 48, 16)
                setOnClickListener {
                    selectedCampus = campus; tvSelectedCampus.text = campus; popupWindow.dismiss(); loadData()
                }
            }
            container.addView(tv)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { currentSearchQuery = s?.toString() ?: ""; loadData() }
        })
    }

    private val bannerImages = intArrayOf(R.drawable.banner_campus, R.drawable.banner_campus2)

    private fun setupBanner() {
        vpBanner.adapter = BannerAdapter()
        vpBanner.setCurrentItem(500, false)
        setupDots(0)
        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { setupDots(position % bannerImages.size) }
        })
    }

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

    private inner class BannerAdapter : RecyclerView.Adapter<BannerAdapter.VH>() {
        override fun getItemCount() = 1000
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return VH(iv)
        }
        override fun onBindViewHolder(holder: VH, position: Int) { holder.bind(bannerImages[position % bannerImages.size]) }
        inner class VH(private val iv: ImageView) : RecyclerView.ViewHolder(iv) {
            fun bind(imageRes: Int) { iv.setImageResource(imageRes) }
        }
    }

    private fun setupCategoryTabs() {
        layoutCategoryTabs.removeAllViews()
        for ((index, category) in categories.withIndex()) {
            val wrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                gravity = Gravity.CENTER
            }
            val tv = TextView(this).apply {
                text = category; textSize = 14f; gravity = Gravity.CENTER; setPadding(4, 10, 4, 6)
                if (index == selectedTabIndex) {
                    setTextColor(getColor(R.color.primary))
                    paintFlags = paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
                } else {
                    setTextColor(getColor(R.color.text_secondary))
                }
            }
            val underline = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24, 3).apply { gravity = Gravity.CENTER_HORIZONTAL }
                setBackgroundColor(if (index == selectedTabIndex) getColor(R.color.primary) else Color.TRANSPARENT)
            }
            wrapper.addView(tv); wrapper.addView(underline)
            wrapper.setOnClickListener {
                selectedTabIndex = index; selectedCategory = if (index == 0) null else categoryMap[category]
                setupCategoryTabs(); loadData()
            }
            layoutCategoryTabs.addView(wrapper)
        }
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter({ item ->
            startActivity(Intent(this, DetailActivity::class.java).putExtra("item_id", item.id))
        }, userManager)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    /**
     * 底部栏 Tab 切换：首页 ↔ 我的
     * 通过显示/隐藏 contentHome 和 fragmentProfile 实现页内切换，底部栏始终不动
     */
    private fun setupBottomBar() {
        val tabHome = findViewById<LinearLayout>(R.id.tabHome)
        val tabMine = findViewById<LinearLayout>(R.id.tabMine)

        // 默认首页高亮
        highlightTab(true)

        tabHome.setOnClickListener {
            layoutTopBar.visibility = View.VISIBLE
            layoutTopBar.setBackgroundColor(getColor(android.R.color.white))
            val bar = layoutTopBar as LinearLayout
            for (i in 0 until bar.childCount) { bar.getChildAt(i).visibility = View.VISIBLE }
            contentHome.visibility = View.VISIBLE
            fragmentProfile.visibility = View.GONE
            highlightTab(true)
        }

        tabMine.setOnClickListener {
            layoutTopBar.visibility = View.GONE
            // 隐藏子view避免残留
            val bar = layoutTopBar as LinearLayout
            for (i in 0 until bar.childCount) { bar.getChildAt(i).visibility = View.INVISIBLE }
            contentHome.visibility = View.GONE
            fragmentProfile.visibility = View.VISIBLE
            highlightTab(false)
        }

        fabPublish.setOnClickListener {
            startActivity(Intent(this, PublishActivity::class.java))
        }
    }

    private fun highlightTab(isHome: Boolean) {
        val primary = getColor(R.color.primary)
        val secondary = getColor(R.color.text_secondary)
        if (isHome) {
            ivHome.setColorFilter(primary); tvHome.setTextColor(primary)
            ivMine.setColorFilter(secondary); tvMine.setTextColor(secondary)
        } else {
            ivMine.setColorFilter(primary); tvMine.setTextColor(primary)
            ivHome.setColorFilter(secondary); tvHome.setTextColor(secondary)
        }
    }

    private fun setupAiEntry() {
        tvAiMatch.setOnClickListener { startActivity(Intent(this, ChatActivity::class.java)) }
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
        if (itemDao.queryAll().isNotEmpty()) return
        val now = System.currentTimeMillis()
        listOf(
            Item(type = Constants.ITEM_TYPE_LOST, name = "黑色钱包", category = "钥匙钱包", location = "图书馆二楼", time = "2026-05-10", contact = "13800138001", description = "黑色皮质钱包，内有身份证和银行卡", publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(now - 3600_000), addressText = "图书馆二楼自习区"),
            Item(type = Constants.ITEM_TYPE_FOUND, name = "学生证", category = "证件卡片", location = "操场看台", time = "2026-05-10", contact = "13900139001", description = "计算机学院 李明同学的学生证", publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(now - 1800_000), addressText = "操场看台"),
            Item(type = Constants.ITEM_TYPE_LOST, name = "蓝牙耳机", category = "电子产品", location = "教学楼A座", time = "2026-05-11", contact = "13800138002", description = "白色AirPods Pro，带蓝色保护壳", publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(now - 7200_000), addressText = "教学楼A座301"),
            Item(type = Constants.ITEM_TYPE_FOUND, name = "高等数学课本", category = "书籍文具", location = "自习室", time = "2026-05-11", contact = "13900139002", description = "同济版高等数学上册，书内有详细笔记", publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(now - 5400_000), addressText = "第一自习室"),
            Item(type = Constants.ITEM_TYPE_LOST, name = "校园卡", category = "证件卡片", location = "食堂一楼", time = "2026-05-12", contact = "13800138003", description = "校园一卡通，卡号后四位8832", publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(now - 10800_000), addressText = "第一食堂"),
            Item(type = Constants.ITEM_TYPE_FOUND, name = "运动水杯", category = "生活用品", location = "体育馆", time = "2026-05-12", contact = "13900139003", description = "绿色运动水杯，品牌李宁，在体育馆更衣室捡到", publisher = "测试用户", publishTime = TimeUtil.formatTimestamp(now - 9000_000), addressText = "体育馆更衣室")
        ).forEach { itemDao.insert(it) }
    }
}
