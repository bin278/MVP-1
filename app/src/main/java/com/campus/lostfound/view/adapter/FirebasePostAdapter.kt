package com.campus.lostfound.view.adapter

import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.campus.lostfound.R
import com.campus.lostfound.firebase.Item
import com.campus.lostfound.sharedpref.UserManager
import com.google.android.material.button.MaterialButton

/**
 * Firebase物品列表适配器
 * 负责展示失物招领列表项，支持点击跳转和长按删除功能
 */
class FirebasePostAdapter(
    private val onItemClick: (Item) -> Unit,
    private val userManager: UserManager
) : RecyclerView.Adapter<FirebasePostAdapter.ViewHolder>() {

    // 物品列表数据
    private val items = mutableListOf<Item>()
    // 长按事件监听器
    private var onItemLongClickListener: ((Item) -> Unit)? = null

    /**
     * 设置长按事件监听器
     * @param listener 长按回调函数
     */
    fun setOnItemLongClickListener(listener: (Item) -> Unit) {
        onItemLongClickListener = listener
    }

    /**
     * 更新物品列表数据
     * @param newItems 新的物品列表
     */
    fun setItems(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * 创建视图持有者
     * @param parent 父容器
     * @param viewType 视图类型
     * @return 视图持有者
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    /**
     * 绑定数据到视图
     * @param holder 视图持有者
     * @param position 列表位置
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    /**
     * 获取列表项数量
     * @return 物品数量
     */
    override fun getItemCount() = items.size

    /**
     * 视图持有者类
     * 负责单个列表项的视图绑定和事件处理
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 视图引用
        private val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        private val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        private val tvNickname: TextView = view.findViewById(R.id.tvNickname)
        private val tvCampusTag: TextView = view.findViewById(R.id.tvCampusTag)
        private val tvPublishTime: TextView = view.findViewById(R.id.tvPublishTime)
        private val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        private val layoutImages: LinearLayout = view.findViewById(R.id.layoutImages)
        private val ivImage1: ImageView = view.findViewById(R.id.ivImage1)
        private val ivImage2: ImageView = view.findViewById(R.id.ivImage2)
        private val tvViews: TextView = view.findViewById(R.id.tvViews)
        private val btnStatus: MaterialButton = view.findViewById(R.id.tvStatus)

        /**
         * 绑定物品数据到视图
         * @param item 物品对象
         */
        fun bind(item: Item) {
            val context = itemView.context
            
            // 设置发布者信息
            val publisherName = item.publisherName ?: item.publisherId ?: "用户"
            tvNickname.text = publisherName
            tvCampusTag.text = "校园"

            // 头像：显示首字
            showTextAvatar(publisherName)

            // 设置相对时间
            val publishTime = item.publishTime
            tvPublishTime.text = if (publishTime != null) {
                val relativeTime = (System.currentTimeMillis() - publishTime) / 1000
                when {
                    relativeTime < 60 -> "刚刚"
                    relativeTime < 3600 -> "${relativeTime / 60} 分钟前"
                    relativeTime < 86400 -> "${relativeTime / 3600} 小时前"
                    else -> "${relativeTime / 86400} 天前"
                }
            } else {
                "未知时间"
            }

            // 设置描述：物品名称加粗 + 地点 + 描述
            val desc = buildDescription(item)
            tvDescription.text = desc

            // 图片加载（从URL加载）
            val imageUrl = item.imageUrl
            if (imageUrl.isNullOrEmpty()) {
                layoutImages.visibility = View.GONE
            } else {
                layoutImages.visibility = View.VISIBLE
                val requestOptions = RequestOptions()
                    .transform(RoundedCorners(8))
                    .centerCrop()
                // 加载第一张图片
                Glide.with(context)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivImage1)
                ivImage2.visibility = View.GONE
            }

            // 设置浏览量（模拟数据）
            val viewCount = (item.id?.hashCode()?.mod(37) ?: 0) + 10
            tvViews.text = context.getString(R.string.views_count, viewCount)
            btnStatus.text = context.getString(R.string.not_claimed)

            // 设置点击事件：跳转到详情页
            itemView.setOnClickListener { onItemClick(item) }
            // 设置长按事件：触发删除回调
            itemView.setOnLongClickListener {
                onItemLongClickListener?.invoke(item)
                true
            }
        }

        /**
         * 显示文字头像（取昵称首字）
         * @param nickname 用户昵称
         */
        private fun showTextAvatar(nickname: String) {
            ivAvatar.visibility = View.GONE
            tvAvatar.visibility = View.VISIBLE
            tvAvatar.text = nickname.take(1)
        }

        /**
         * 构建描述文本：物品名称加粗 + 分类 + 地点 + 描述
         * @param item 物品对象
         * @return 格式化后的描述文本
         */
        private fun buildDescription(item: Item): CharSequence {
            val name = item.name ?: ""
            val location = item.location ?: ""
            val description = item.description ?: ""
            
            val fullText = buildString {
                append(name)
                if (location.isNotEmpty()) {
                    append(" 📍").append(location)
                }
                if (description.isNotEmpty()) {
                    append("。").append(description)
                }
            }
            // 创建带样式的文本：物品名称加粗
            val spannable = SpannableString(fullText)
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                name.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spannable
        }
    }
}
