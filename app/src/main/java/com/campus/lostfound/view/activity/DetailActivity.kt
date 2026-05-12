package com.campus.lostfound.view.activity

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

class DetailActivity : BaseActivity() {

    private lateinit var itemDao: ItemDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var userManager: UserManager
    private var item: Item? = null
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        itemDao = ItemDao(this)
        favoriteDao = FavoriteDao(this)
        userManager = UserManager(this)

        val itemId = intent.getLongExtra("item_id", -1)
        if (itemId <= 0) {
            finish()
            return
        }

        item = itemDao.queryById(itemId)
        if (item == null) {
            finish()
            return
        }

        displayItem(item!!)
        setupFavoriteButton(itemId)
        setupMapSection(item!!)
        setupShareButton(item!!)
    }

    private fun displayItem(item: Item) {
        val ivImage = findViewById<ImageView>(R.id.ivDetailImage)
        val tvType = findViewById<TextView>(R.id.tvDetailType)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvLocation = findViewById<TextView>(R.id.tvDetailLocation)
        val tvAddress = findViewById<TextView>(R.id.tvDetailAddress)
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvContact = findViewById<TextView>(R.id.tvDetailContact)
        val tvPublisher = findViewById<TextView>(R.id.tvDetailPublisher)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)

        tvName.text = item.name
        tvCategory.text = item.category.ifEmpty { "未分类" }
        tvLocation.text = item.location.ifEmpty { "未标记地点" }
        if (item.addressText.isNotEmpty()) {
            tvAddress.text = "详细地址: ${item.addressText}"
            tvAddress.visibility = View.VISIBLE
        } else {
            tvAddress.visibility = View.GONE
        }
        tvTime.text = item.time.ifEmpty { "未指定时间" }
        tvContact.text = item.contact
        tvPublisher.text = item.publisher
        tvDescription.text = item.description.ifEmpty { "无描述" }

        if (item.type == Constants.ITEM_TYPE_LOST) {
            tvType.text = "失物"
            val bg = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(getColor(R.color.lost_tag))
            }
            tvType.background = bg
        } else {
            tvType.text = "招领"
            val bg = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(getColor(R.color.found_tag))
            }
            tvType.background = bg
        }

        if (item.imagePath.isNotEmpty()) {
            val file = File(item.imagePath)
            if (file.exists()) {
                Glide.with(this).load(file).centerCrop().into(ivImage)
            }
        }
    }

    private fun setupFavoriteButton(itemId: Long) {
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)
        val username = userManager.getCurrentUser()
        isFavorite = favoriteDao.isFavorite(username, itemId)
        updateFavoriteButton(btnFavorite)

        btnFavorite.setOnClickListener {
            if (isFavorite) {
                favoriteDao.removeFavorite(username, itemId)
                isFavorite = false
                Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show()
            } else {
                favoriteDao.addFavorite(username, itemId)
                isFavorite = true
                Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show()
            }
            updateFavoriteButton(btnFavorite)
        }
    }

    private fun updateFavoriteButton(btnFavorite: MaterialButton) {
        if (isFavorite) {
            btnFavorite.text = "取消收藏"
            btnFavorite.setIconResource(android.R.drawable.btn_star_big_on)
        } else {
            btnFavorite.text = "收藏"
            btnFavorite.setIconResource(android.R.drawable.btn_star_big_off)
        }
    }

    private fun setupMapSection(item: Item) {
        val cardMap = findViewById<MaterialCardView>(R.id.cardMap)
        val ivStaticMap = findViewById<ImageView>(R.id.ivStaticMap)
        val btnNavigate = findViewById<MaterialButton>(R.id.btnNavigate)
        val tvMapCoords = findViewById<TextView>(R.id.tvMapCoords)

        if (item.latitude != 0.0 && item.longitude != 0.0) {
            cardMap.visibility = View.VISIBLE

            val staticMapUrl = "https://restapi.amap.com/v3/staticmap?location=${item.longitude},${item.latitude}&zoom=15&size=400*200&markers=mid,0xFF0000,A:${item.longitude},${item.latitude}&key=${Constants.AMAP_API_KEY}"
            Glide.with(this).load(staticMapUrl).into(ivStaticMap)

            tvMapCoords.text = "坐标: ${String.format("%.6f", item.latitude)}, ${String.format("%.6f", item.longitude)}"

            ivStaticMap.setOnClickListener {
                openNavigation(item.latitude, item.longitude)
            }

            btnNavigate.setOnClickListener {
                openNavigation(item.latitude, item.longitude)
            }
        }
    }

    private fun openNavigation(lat: Double, lng: Double) {
        val addressLabel = item?.addressText?.ifEmpty { item?.location } ?: "目标地点"
        
        if (isAppInstalled("com.autonavi.minimap")) {
            val uri = Uri.parse("androidamap://route/plan/?dlat=$lat&dlon=$lng&dname=$addressLabel&dev=0&t=0")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.autonavi.minimap")
            startActivity(intent)
        } else {
            try {
                val uri = Uri.parse("baidumap://map/direction?destination=latlng:$lat,$lng|name:$addressLabel&mode=driving")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (e2: Exception) {
                try {
                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($addressLabel)")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } catch (e3: Exception) {
                    Toast.makeText(this, "未安装地图应用，坐标已复制到剪贴板", Toast.LENGTH_LONG).show()
                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("坐标", "$lat, $lng")
                    clipboard.setPrimaryClip(clip)
                }
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun setupShareButton(item: Item) {
        val btnShare = findViewById<MaterialButton>(R.id.btnShare)
        btnShare.setOnClickListener {
            val shareText = "【${if (item.type == "lost") "失物" else "招领"}】${item.name}\n地点: ${item.addressText.ifEmpty { item.location }}\n联系方式: ${item.contact}\n描述: ${item.description}"
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(Intent.createChooser(intent, "分享"))
        }
    }
}
