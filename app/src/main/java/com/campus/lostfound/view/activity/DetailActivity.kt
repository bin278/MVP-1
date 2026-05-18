package com.campus.lostfound.view.activity

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.Item
import com.campus.lostfound.sharedpref.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File

/**
 * 物品详情页面 - Firebase版本
 * 展示物品图片轮播、基本信息、联系方式、地图导航、收藏和分享功能
 */
class DetailActivity : BaseActivity() {

    // 日志标签
    private val TAG = "DetailActivity"

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
        userManager = UserManager(this)

        // 获取传入的物品ID（从 Firebase 传入的是字符串ID）
        val itemId = intent.getStringExtra("itemId")
        val itemType = intent.getStringExtra("itemType")

        Log.d(TAG, "接收到物品ID: $itemId, 类型: $itemType")

        // 验证物品ID有效性
        if (itemId.isNullOrEmpty()) {
            showToast("物品ID无效")
            finish()
            return
        }

        // 根据ID从 Firebase 查询物品详情
        FirebaseHelper.getItemById(itemId) { firebaseItem ->
            if (firebaseItem == null) {
                showToast("物品不存在")
                finish()
                return@getItemById
            }
            item = firebaseItem
            // 初始化各功能模块
            displayItem(firebaseItem)
            setupFavoriteButton(firebaseItem)
            setupMapSection(firebaseItem)
            setupShareButton(firebaseItem)
            setupDialButton(firebaseItem)
        }
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
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val tvContact = findViewById<TextView>(R.id.tvDetailContact)
        val tvPublisher = findViewById<TextView>(R.id.tvDetailPublisher)

        // 设置物品类型标签（丢失/捡到）
        tvType.text = if (item.type == "lost") "丢失物品" else "捡到物品"
        tvType.setBackgroundResource(if (item.type == "lost") R.drawable.bg_type_tag_lost else R.drawable.bg_type_tag_found)

        // 设置物品名称
        tvName.text = item.name ?: "未知物品"

        // 设置分类标签
        tvCategory.text = item.category ?: "其他"
        val categoryDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f
            setColor(getColor(R.color.gray_bg))
        }
        tvCategory.background = categoryDrawable

        // 设置地点信息
        tvLocation.text = "${item.campus ?: ""} - ${item.location ?: ""}"

        // 设置发布时间
        tvTime.text = item.time ?: ""

        // 设置物品描述
        tvDescription.text = item.description ?: "暂无描述"

        // 设置联系方式
        tvContact.text = item.phone ?: "未提供"

        // 设置发布者信息
        tvPublisher.text = item.publisherName ?: "匿名用户"

        // 设置图片轮播
        setupImagePager(vpImages, layoutDots, item.images ?: emptyList())
    }

    /**
     * 设置图片轮播
     * @param vpImages ViewPager2组件
     * @param layoutDots 指示器布局
     * @param images 图片路径列表
     */
    private fun setupImagePager(vpImages: androidx.viewpager2.widget.ViewPager2, layoutDots: LinearLayout, images: List<String>) {
        // 如果没有图片，使用默认图片
        val imagePaths = if (images.isEmpty()) {
            listOf("")
        } else {
            images
        }

        vpImages.adapter = DetailImageAdapter(imagePaths)
        setupImageDots(layoutDots, imagePaths.size, 0)

        vpImages.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupImageDots(layoutDots, imagePaths.size, position)
            }
        })
    }

    /**
     * 设置图片指示器圆点
     * @param layoutDots 指示器布局
     * @param count 圆点总数
     * @param selected 当前选中的圆点索引
     */
    private fun setupImageDots(layoutDots: LinearLayout, count: Int, selected: Int) {
        layoutDots.removeAllViews()
        for (i in 0 until count) {
            val dot = ImageView(this).apply {
                setImageResource(if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive)
                layoutParams = LinearLayout.LayoutParams(10, 10).apply { setMargins(5, 0, 5, 0) }
            }
            layoutDots.addView(dot)
        }
    }

    /**
     * 详情页图片适配器
     */
    private inner class DetailImageAdapter(private val images: List<String>) : RecyclerView.Adapter<DetailImageAdapter.VH>() {
        override fun getItemCount() = images.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return VH(iv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(images[position])
        }

        inner class VH(private val iv: ImageView) : RecyclerView.ViewHolder(iv) {
            fun bind(imagePath: String) {
                if (imagePath.isEmpty()) {
                    // 没有图片时显示默认图片
                    iv.setImageResource(R.drawable.ic_launcher_foreground)
                } else if (imagePath.startsWith("http")) {
                    // 网络图片
                    Glide.with(iv.context).load(imagePath).into(iv)
                } else {
                    // 本地图片
                    val file = File(imagePath)
                    if (file.exists()) {
                        Glide.with(iv.context).load(file).into(iv)
                    } else {
                        iv.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            }
        }
    }

    /**
     * 设置收藏按钮
     */
    private fun setupFavoriteButton(item: Item) {
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)

        // 检查是否已收藏
        FirebaseHelper.isFavorite(item.id ?: "", userManager.getUserId()) { isFav ->
            isFavorite = isFav
            updateFavoriteButton(btnFavorite, isFavorite)
        }

        btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteButton(btnFavorite, isFavorite)

            if (isFavorite) {
                FirebaseHelper.addFavorite(item.id ?: "", userManager.getUserId()) { success ->
                    if (!success) {
                        showToast("收藏失败")
                        isFavorite = false
                        updateFavoriteButton(btnFavorite, isFavorite)
                    }
                }
            } else {
                FirebaseHelper.removeFavorite(item.id ?: "", userManager.getUserId()) { success ->
                    if (!success) {
                        showToast("取消收藏失败")
                        isFavorite = true
                        updateFavoriteButton(btnFavorite, isFavorite)
                    }
                }
            }
        }
    }

    /**
     * 更新收藏按钮状态
     */
    private fun updateFavoriteButton(btn: MaterialButton, isFav: Boolean) {
        if (isFav) {
            btn.setBackgroundColor(getColor(R.color.red))
            btn.setIconResource(android.R.drawable.btn_star_big_on)
            btn.setTextColor(getColor(R.color.white))
        } else {
            btn.setBackgroundColor(getColor(R.color.gray_bg))
            btn.setIconResource(android.R.drawable.btn_star_big_off)
            btn.setTextColor(getColor(R.color.text_primary))
        }
    }

    /**
     * 设置地图区域
     */
    private fun setupMapSection(item: Item) {
        val cardMap = findViewById<MaterialCardView>(R.id.cardMap)
        val tvMapCoords = findViewById<TextView>(R.id.tvMapCoords)
        val btnNavigate = findViewById<MaterialButton>(R.id.btnNavigate)

        if (item.latitude != null && item.longitude != null) {
            cardMap.visibility = View.VISIBLE
            tvMapCoords.text = "坐标: ${item.latitude}, ${item.longitude}"

            // 设置导航按钮点击事件
            btnNavigate.setOnClickListener {
                navigateToLocation(item.latitude!!, item.longitude!!, item.location ?: "位置")
            }
        } else {
            cardMap.visibility = View.GONE
        }
    }

    /**
     * 跳转到地图应用导航
     * @param latitude 纬度
     * @param longitude 经度
     * @param locationName 位置名称
     */
    private fun navigateToLocation(latitude: Double, longitude: Double, locationName: String) {
        // 尝试使用高德地图导航
        val amapUri = Uri.parse("amapuri://route/plan?sid=BGVISUAL&dlat=$latitude&dlon=$longitude&dname=$locationName&dev=0&t=0")
        val amapIntent = Intent(Intent.ACTION_VIEW, amapUri)
        
        if (amapIntent.resolveActivity(packageManager) != null) {
            startActivity(amapIntent)
            return
        }

        // 尝试使用百度地图导航
        val baiduUri = Uri.parse("baidumap://map/direction?destination=latlng:$latitude,$longitude|name:$locationName&mode=driving")
        val baiduIntent = Intent(Intent.ACTION_VIEW, baiduUri)
        
        if (baiduIntent.resolveActivity(packageManager) != null) {
            startActivity(baiduIntent)
            return
        }

        // 尝试使用腾讯地图导航
        val tencentUri = Uri.parse("qqmap://map/routeplan?type=drive&to=$latitude,$longitude,$locationName")
        val tencentIntent = Intent(Intent.ACTION_VIEW, tencentUri)
        
        if (tencentIntent.resolveActivity(packageManager) != null) {
            startActivity(tencentIntent)
            return
        }

        // 使用系统地图（通用方案）
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($locationName)")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
        
        if (geoIntent.resolveActivity(packageManager) != null) {
            startActivity(geoIntent)
        } else {
            showToast("未找到地图应用")
        }
    }

    /**
     * 设置分享按钮
     */
    private fun setupShareButton(item: Item) {
        val btnShare = findViewById<MaterialButton>(R.id.btnShare)
        btnShare.setOnClickListener {
            val shareText = """
                ${if (item.type == "lost") "【失物招领】寻找" else "【捡到物品】"}, ${item.name ?: "物品"}
                分类：${item.category ?: ""}
                地点：${item.campus ?: ""} - ${item.location ?: ""}
                描述：${item.description ?: ""}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "分享到"))
        }
    }

    /**
     * 设置拨号按钮
     */
    private fun setupDialButton(item: Item) {
        val btnDial = findViewById<MaterialButton>(R.id.btnDial)
        btnDial.setOnClickListener {
            val phone = item.phone ?: ""
            if (phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } else {
                showToast("未提供联系方式")
            }
        }
    }
}
