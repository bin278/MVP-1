package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.util.TimeUtil
import com.campus.lostfound.view.fragment.ItemListFragment
import com.campus.lostfound.view.fragment.ProfileFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

class MainActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var etSearch: EditText
    private lateinit var itemDao: ItemDao
    private lateinit var userManager: UserManager

    private val tabTitles = listOf("全部", "失物", "招领")
    private var currentSearchQuery: String = ""
    // 保存三个列表Fragment的引用
    private val fragmentList = mutableListOf<ItemListFragment?>(null, null, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        itemDao = ItemDao(this)
        userManager = UserManager(this)

        // 创建测试用户
        if (!userManager.isLoggedIn()) {
            userManager.register("测试用户", "123456")
            userManager.login("测试用户", "123456")
        }

        // 添加测试数据
        addTestData()

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        bottomNav = findViewById(R.id.bottomNav)
        etSearch = findViewById(R.id.etSearch)

        // 设置搜索框文本变化监听
        setupSearchListener()

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ItemListFragment.newInstance(null, currentSearchQuery).also { fragmentList[0] = it }
                    1 -> ItemListFragment.newInstance("lost", currentSearchQuery).also { fragmentList[1] = it }
                    2 -> ItemListFragment.newInstance("found", currentSearchQuery).also { fragmentList[2] = it }
                    3 -> ProfileFragment()
                    else -> ItemListFragment.newInstance(null, currentSearchQuery)
                }
            }
        }

        for (title in tabTitles) {
            tabLayout.addTab(tabLayout.newTab().setText(title))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { viewPager.currentItem = it }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> viewPager.currentItem = 0
                R.id.nav_lost -> viewPager.currentItem = 1
                R.id.nav_publish -> {
                    startActivity(Intent(this, PublishActivity::class.java))
                    return@setOnItemSelectedListener true
                }
                R.id.nav_found -> viewPager.currentItem = 2
                R.id.nav_profile -> viewPager.currentItem = 3
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val navItemId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_lost
                    2 -> R.id.nav_found
                    3 -> R.id.nav_profile
                    else -> R.id.nav_home
                }
                bottomNav.menu.findItem(navItemId)?.isChecked = true
            }
        })
    }

    /**
     * 设置搜索框监听
     */
    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString() ?: ""
                // 直接更新所有三个列表Fragment
                updateAllFragmentsSearch()
            }
        })
    }

    /**
     * 更新所有Fragment的搜索关键词
     */
    private fun updateAllFragmentsSearch() {
        for (i in 0..2) {
            fragmentList[i]?.updateSearchQuery(currentSearchQuery)
        }
    }

    private fun addTestData() {
        // 检查是否已有数据，避免重复添加
        val existingItems = itemDao.queryAll()
        if (existingItems.isNotEmpty()) return

        // 添加失物数据
        val lostItems = listOf(
            Item(
                id = 0,
                type = Constants.ITEM_TYPE_LOST,
                name = "黑色钱包",
                category = "钥匙钱包",
                location = "图书馆二楼",
                time = "2026-05-10",
                contact = "13800138001",
                description = "钱包里有身份证、银行卡和现金",
                imagePath = "",
                publisher = "测试用户",
                publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 3600),
                latitude = 0.0,
                longitude = 0.0,
                addressText = "图书馆二楼"
            ),
            Item(
                id = 0,
                type = Constants.ITEM_TYPE_LOST,
                name = "蓝色雨伞",
                category = "生活用品",
                location = "教学楼A座",
                time = "2026-05-11",
                contact = "13800138002",
                description = "长柄蓝色雨伞，伞柄有轻微磨损",
                imagePath = "",
                publisher = "测试用户",
                publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 7200),
                latitude = 0.0,
                longitude = 0.0,
                addressText = "教学楼A座"
            ),
            Item(
                id = 0,
                type = Constants.ITEM_TYPE_LOST,
                name = "苹果笔记本电脑",
                category = "电子产品",
                location = "食堂",
                time = "2026-05-12",
                contact = "13800138003",
                description = "银色13寸MacBook Pro，键盘有贴纸",
                imagePath = "",
                publisher = "测试用户",
                publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 10800),
                latitude = 0.0,
                longitude = 0.0,
                addressText = "食堂"
            )
        )

        // 添加招领数据
        val foundItems = listOf(
            Item(
                id = 0,
                type = Constants.ITEM_TYPE_FOUND,
                name = "学生证",
                category = "证件卡片",
                location = "操场看台",
                time = "2026-05-10",
                contact = "13900139001",
                description = "拾到学生证一张，姓名李明",
                imagePath = "",
                publisher = "测试用户",
                publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 1800),
                latitude = 0.0,
                longitude = 0.0,
                addressText = "操场看台"
            ),
            Item(
                id = 0,
                type = Constants.ITEM_TYPE_FOUND,
                name = "运动水杯",
                category = "生活用品",
                location = "体育馆",
                time = "2026-05-11",
                contact = "13900139002",
                description = "绿色运动水杯，品牌是李宁",
                imagePath = "",
                publisher = "测试用户",
                publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 5400),
                latitude = 0.0,
                longitude = 0.0,
                addressText = "体育馆"
            ),
            Item(
                id = 0,
                type = Constants.ITEM_TYPE_FOUND,
                name = "高等数学教材",
                category = "书籍文具",
                location = "自习室",
                time = "2026-05-12",
                contact = "13900139003",
                description = "高等数学同济版上册，有笔记",
                imagePath = "",
                publisher = "测试用户",
                publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp() - 9000),
                latitude = 0.0,
                longitude = 0.0,
                addressText = "自习室"
            )
        )

        // 插入数据库
        (lostItems + foundItems).forEach { itemDao.insert(it) }
    }
}