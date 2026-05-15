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
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.util.TimeUtil
import com.google.android.material.button.MaterialButton
import java.io.File

class PostAdapter(
    private val onItemClick: (Item) -> Unit,
    private val userManager: UserManager
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    private val items = mutableListOf<Item>()
    private var onItemLongClickListener: ((Item) -> Unit)? = null

    fun setOnItemLongClickListener(listener: (Item) -> Unit) {
        onItemLongClickListener = listener
    }

    fun setItems(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        fun bind(item: Item) {
            val context = itemView.context
            val publisherInfo = userManager.getUserInfo(item.publisher)
            val nickname = publisherInfo.nickname.ifEmpty { item.publisher }
            val campus = publisherInfo.campus.ifEmpty { "校园" }

            // 头像：有图片用 Glide 圆形加载，无图片显示首字
            if (publisherInfo.avatarPath.isNotEmpty()) {
                val avatarFile = File(publisherInfo.avatarPath)
                if (avatarFile.exists()) {
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatar.visibility = View.GONE
                    Glide.with(context)
                        .load(avatarFile)
                        .circleCrop()
                        .into(ivAvatar)
                } else {
                    showTextAvatar(nickname)
                }
            } else {
                showTextAvatar(nickname)
            }

            tvNickname.text = nickname
            tvCampusTag.text = campus

            // 相对时间（如 "3 小时前"）
            val publishTimeMillis = TimeUtil.parseDate(item.publishTime)
            tvPublishTime.text = if (publishTimeMillis > 0) {
                TimeUtil.formatRelativeTime(publishTimeMillis)
            } else {
                item.publishTime
            }

            // 描述：物品名称加粗 + 地点 + 描述（不保留"丢失："前缀）
            val desc = buildDescription(item)
            tvDescription.text = desc

            // 图片加载
            val imageFiles = getImageFiles(item.imagePath)
            if (imageFiles.isEmpty()) {
                layoutImages.visibility = View.GONE
            } else {
                layoutImages.visibility = View.VISIBLE
                val requestOptions = RequestOptions()
                    .transform(RoundedCorners(8))
                    .centerCrop()
                Glide.with(context)
                    .load(imageFiles[0])
                    .apply(requestOptions)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivImage1)

                if (imageFiles.size > 1) {
                    ivImage2.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(imageFiles[1])
                        .apply(requestOptions)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivImage2)
                } else {
                    ivImage2.visibility = View.GONE
                }
            }

            val viewCount = (item.id % 37 + 10).toInt()
            tvViews.text = context.getString(R.string.views_count, viewCount)
            btnStatus.text = context.getString(R.string.not_claimed)

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClickListener?.invoke(item)
                true
            }
        }

        /** 显示文字头像（首字） */
        private fun showTextAvatar(nickname: String) {
            ivAvatar.visibility = View.GONE
            tvAvatar.visibility = View.VISIBLE
            tvAvatar.text = nickname.take(1)
        }

        /** 构建描述文本：物品名称加粗 + 特征 + 地点 + 描述 */
        private fun buildDescription(item: Item): CharSequence {
            val sb = StringBuilder()
            sb.append(item.name)
            if (item.category.isNotEmpty() && item.category != "其他") {
                sb.append(" · ").append(item.category)
            }
            val descPart = item.description
            val locationText = if (item.addressText.isNotEmpty()) item.addressText else item.location
            val fullText = buildString {
                append(sb)
                if (locationText.isNotEmpty()) {
                    append(" 📍").append(locationText)
                }
                if (descPart.isNotEmpty()) {
                    append("。").append(descPart)
                }
            }
            val spannable = SpannableString(fullText)
            // 物品名称加粗
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                item.name.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spannable
        }

        private fun getImageFiles(imagePath: String): List<File> {
            if (imagePath.isEmpty()) return emptyList()
            // 多图路径以 ||| 分隔
            return imagePath.split("|||")
                .map { File(it) }
                .filter { it.exists() }
        }
    }
}
