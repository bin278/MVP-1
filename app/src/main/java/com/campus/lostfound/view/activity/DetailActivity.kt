package com.campus.lostfound.view.activity

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.db.FavoriteDao
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File

/**
 * 物品详情页面
 * 展示物品图片轮播、基本信息、联系方式、地图导航、收藏和分享功能
 */
class DetailActivity : BaseActivity() {

    // 物品数据访问对象
    private lateinit var itemDao: ItemDao
    // 收藏数据访问对象
    private lateinit var favoriteDao: FavoriteDao
    // 用户管理器
    private lateinit var userManager: UserManager
    // 当前物品对象
    private var item: Item? = null
    // 是否为收藏状态
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // 初始化数据访问对象
        itemDao = ItemDao(this)
        favoriteDao = FavoriteDao(this)
        userManager = UserManager(this)

        // 获取传入的物品ID
        val itemId = intent.getLongExtra("item_id", -1)
        // 验证物品ID有效性
        if (itemId <= 0) { finish(); return }

        // 根据ID查询物品详情
        item = itemDao.queryById(itemId)
        // 验证物品存在性
        if (item == null) { finish(); return }

        // 初始化各功能模块
        displayItem(item!!)
        setupFavoriteButton(itemId)
        setupMapSection(item!!)
        setupShareButton(item!!)
        setupDialButton(item!!)
    }

    /**
     * 显示物品详细信息
     * @param item 物品对象
     */
    private fun displayItem(item: Item) {
        // 获取视图引用
        val vpImages = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpDetailImages)
        val layoutDots = findViewById<LinearLayout>(R.id.layoutImageDots)
        val tvType = findViewById<TextView>(R.id.tvDetailType)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvLocation = findViewById<TextView>(R.id.tvDetailLocation)
        val tvAddress = findViewById<TextView>(R.id.tvDetailAddress)
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvContact = findViewById<TextView>(R.id.tvDetailContact)
        val tvPublisher = findViewById<TextView>(R.id.tvDetailPublisher)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)

        // 设置物品基本信息
        tvName.text = item.name
        tvCategory.text = item.category.ifEmpty { "未分类" }
        tvLocation.text = item.location.ifEmpty { "未标记地点" }
        
        // 设置详细地址（可选）
        if (item.addressText.isNotEmpty()) {
            tvAddress.text = "详细: ${item.addressText}"
            tvAddress.visibility = View.VISIBLE
        } else {
            tvAddress.visibility = View.GONE
        }
        
        tvTime.text = item.time.ifEmpty { "未指定时间" }
        tvContact.text = item.contact
        tvPublisher.text = item.publisher
        tvDescription.text = item.description.ifEmpty { "暂无描述" }

        // 设置类型标签样式：失物红色 / 招领蓝色
        if (item.type == Constants.ITEM_TYPE_LOST) {
            tvType.text = "失物"
            tvType.background = GradientDrawable().apply {
                cornerRadius = 12f; setColor(getColor(R.color.lost_tag))
            }
        } else {
            tvType.text = "招领"
            tvType.background = GradientDrawable().apply {
                cornerRadius = 12f; setColor(getColor(R.color.found_tag))
            }
        }

        // 加载多图显示
        val imageFiles = if (item.imagePath.isNotEmpty()) {
            item.imagePath.split("|||").map { File(it) }.filter { it.exists() }
        } else emptyList()

        if (imageFiles.isNotEmpty()) {
            vpImages.visibility = View.VISIBLE
            layoutDots.visibility = View.VISIBLE
            vpImages.adapter = DetailImageAdapter(imageFiles)
            // 初始化指示器圆点
            layoutDots.removeAllViews()
            imageFiles.forEachIndexed { i, _ ->
                val dot = ImageView(this).apply {
                    val size = 8.dp
                    layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(4.dp, 0, 4.dp, 0) }
                    setImageResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
                }
                layoutDots.addView(dot)
            }
            // 设置页面切换监听，更新指示器
            vpImages.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    for (j in 0 until layoutDots.childCount) {
                        (layoutDots.getChildAt(j) as ImageView).setImageResource(
                            if (j == pos) R.drawable.dot_active else R.drawable.dot_inactive
                        )
                    }
                }
            })
        } else {
            vpImages.visibility = View.GONE
            layoutDots.visibility = View.GONE
        }
    }

    /**
     * 设置拨号按钮：点击后打开系统拨号界面
     * @param item 物品对象
     */
    private fun setupDialButton(item: Item) {
        val btnDial = findViewById<MaterialButton>(R.id.btnDial)
        btnDial.setOnClickListener {
            val phoneNumber = item.contact
            if (phoneNumber.isNotEmpty()) {
                // 打开系统拨号界面
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "暂无联系方式", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 设置收藏按钮：切换收藏状态
     * @param itemId 物品ID
     */
    private fun setupFavoriteButton(itemId: Long) {
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)
        val username = userManager.getCurrentUser()
        // 查询当前收藏状态
        isFavorite = favoriteDao.isFavorite(username, itemId)
        // 更新按钮显示状态
        updateFavoriteButton(btnFavorite)

        btnFavorite.setOnClickListener {
            if (isFavorite) {
                // 取消收藏
                favoriteDao.removeFavorite(username, itemId)
                isFavorite = false
                Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show()
            } else {
                // 添加收藏
                favoriteDao.addFavorite(username, itemId)
                isFavorite = true
                Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show()
            }
            // 更新按钮显示状态
            updateFavoriteButton(btnFavorite)
        }
    }

    /**
     * 更新收藏按钮文字和图标
     * @param btnFavorite 收藏按钮
     */
    private fun updateFavoriteButton(btnFavorite: MaterialButton) {
        if (isFavorite) {
            btnFavorite.text = "取消收藏"
            btnFavorite.setIconResource(android.R.drawable.btn_star_big_on)
        } else {
            btnFavorite.text = "收藏"
            btnFavorite.setIconResource(android.R.drawable.btn_star_big_off)
        }
    }

    /**
     * 设置地图区域：展示静态地图和导航入口
     * @param item 物品对象
     */
    private fun setupMapSection(item: Item) {
        val cardMap = findViewById<MaterialCardView>(R.id.cardMap)
        val ivStaticMap = findViewById<ImageView>(R.id.ivStaticMap)
        val btnNavigate = findViewById<MaterialButton>(R.id.btnNavigate)
        val tvMapCoords = findViewById<TextView>(R.id.tvMapCoords)

        // 只有当物品有有效坐标时才显示地图区域
        if (item.latitude != 0.0 && item.longitude != 0.0) {
            cardMap.visibility = View.VISIBLE
            // 显示坐标信息
            tvMapCoords.text = "坐标: ${String.format("%.6f", item.latitude)}, ${String.format("%.6f", item.longitude)}"
            // 加载静态地图图片
            loadStaticMap(item.longitude, item.latitude, ivStaticMap)
            // 设置点击事件：图片和按钮都可触发导航
            ivStaticMap.setOnClickListener { openNavigation(item.latitude, item.longitude) }
            btnNavigate.setOnClickListener { openNavigation(item.latitude, item.longitude) }
        }
    }

    /**
     * 加载静态地图（高德 API，失败回退 OpenStreetMap）
     * @param lng 经度
     * @param lat 纬度
     * @param ivStaticMap 显示地图的ImageView
     */
    private fun loadStaticMap(lng: Double, lat: Double, ivStaticMap: ImageView) {
        // 高德地图静态图URL
        val amapUrl = "https://restapi.amap.com/v3/staticmap?location=$lng,$lat&zoom=15&size=400*200&markers=mid,0xFF0000,A:$lng,$lat&key=${Constants.AMAP_API_KEY}"
        // 计算OSM瓦片坐标
        val zoom = 15
        val tileX = lon2tile(lng, zoom)
        val tileY = lat2tile(lat, zoom)
        val osmTileUrl = "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
        
        // 使用Glide加载，优先使用高德地图，失败则使用OpenStreetMap
        Glide.with(this)
            .load(amapUrl)
            .error(Glide.with(this).load(osmTileUrl).error(android.R.drawable.ic_menu_mapmode))
            .into(ivStaticMap)
    }

    /**
     * 将经度转换为OSM瓦片X坐标
     * @param lon 经度
     * @param zoom 缩放级别
     * @return 瓦片X坐标
     */
    private fun lon2tile(lon: Double, zoom: Int): Int = 
        ((lon + 180) / 360 * Math.pow(2.0, zoom.toDouble())).toInt()

    /**
     * 将纬度转换为OSM瓦片Y坐标
     * @param lat 纬度
     * @param zoom 缩放级别
     * @return 瓦片Y坐标
     */
    private fun lat2tile(lat: Double, zoom: Int): Int {
        val latRad = Math.toRadians(lat)
        return ((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * Math.pow(2.0, zoom.toDouble())).toInt()
    }

    /**
     * 打开地图导航：优先高德 → 百度 → 系统地图 → 复制坐标
     * @param lat 纬度
     * @param lng 经度
     */
    private fun openNavigation(lat: Double, lng: Double) {
        val label = item?.addressText?.ifEmpty { item?.location } ?: "目标地点"
        
        // 优先使用高德地图
        if (isAppInstalled("com.autonavi.minimap")) {
            val uri = Uri.parse("androidamap://route/plan/?dlat=$lat&dlon=$lng&dname=$label&dev=0&t=0")
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.autonavi.minimap"))
        } else {
            try {
                // 尝试百度地图
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("baidumap://map/direction?destination=latlng:$lat,$lng|name=$label&mode=driving")))
            } catch (e: Exception) {
                try {
                    // 尝试系统地图
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")))
                } catch (e2: Exception) {
                    // 最后将坐标复制到剪贴板
                    Toast.makeText(this, "未安装地图应用", Toast.LENGTH_SHORT).show()
                    (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .setPrimaryClip(android.content.ClipData.newPlainText("坐标", "$lat,$lng"))
                    Toast.makeText(this, "坐标已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 检查应用是否已安装
     * @param packageName 应用包名
     * @return 是否已安装
     */
    private fun isAppInstalled(packageName: String): Boolean = try {
        packageManager.getApplicationInfo(packageName, 0); true
    } catch (e: Exception) { false }

    /**
     * 设置分享按钮：通过系统分享菜单发送物品信息
     * @param item 物品对象
     */
    private fun setupShareButton(item: Item) {
        findViewById<MaterialButton>(R.id.btnShare).setOnClickListener {
            // 构建分享文本
            val text = "【${if (item.type == "lost") "失物" else "招领"}】${item.name}\n" +
                "地点: ${item.addressText.ifEmpty { item.location }}\n" +
                "联系方式: ${item.contact}\n" +
                "描述: ${item.description}"
            // 创建分享意图
            val intent = Intent(Intent.ACTION_SEND).apply { 
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text) 
            }
            startActivity(Intent.createChooser(intent, "分享"))
        }
    }

    /**
     * 物品图片 ViewPager 适配器
     * 负责显示物品的多张图片
     */
    private inner class DetailImageAdapter(private val files: List<File>) :
        RecyclerView.Adapter<DetailImageAdapter.VH>() {
        
        override fun getItemCount() = files.size
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return VH(iv)
        }
        
        override fun onBindViewHolder(holder: VH, pos: Int) {
            Glide.with(this@DetailActivity).load(files[pos]).centerCrop().into(holder.iv)
        }
        
        inner class VH(val iv: ImageView) : RecyclerView.ViewHolder(iv)
    }

    /**
     * Int扩展函数：将dp值转换为像素值
     */
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}