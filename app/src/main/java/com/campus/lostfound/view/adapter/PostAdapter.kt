package com.campus.lostfound.view.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.io.File

class PostAdapter(
    private val onItemClick: (Item) -> Unit,
    private val userManager: UserManager
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    private val items = mutableListOf<Item>()

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
        private val ivImage1: ShapeableImageView = view.findViewById(R.id.ivImage1)
        private val ivImage2: ShapeableImageView = view.findViewById(R.id.ivImage2)
        private val tvViews: TextView = view.findViewById(R.id.tvViews)
        private val btnStatus: MaterialButton = view.findViewById(R.id.tvStatus)

        fun bind(item: Item) {
            val publisherInfo = userManager.getUserInfo(item.publisher)
            val nickname = publisherInfo.nickname.ifEmpty { item.publisher }
            val campus = publisherInfo.campus.ifEmpty { "校园" }

            if (publisherInfo.avatarPath.isNotEmpty()) {
                val avatarFile = File(publisherInfo.avatarPath)
                if (avatarFile.exists()) {
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatar.visibility = View.GONE
                    Glide.with(itemView.context)
                        .load(avatarFile)
                        .circleCrop()
                        .into(ivAvatar)
                } else {
                    ivAvatar.visibility = View.GONE
                    tvAvatar.visibility = View.VISIBLE
                    tvAvatar.text = nickname.take(1)
                }
            } else {
                ivAvatar.visibility = View.GONE
                tvAvatar.visibility = View.VISIBLE
                tvAvatar.text = nickname.take(1)
            }

            tvNickname.text = nickname
            tvCampusTag.text = campus
            tvPublishTime.text = item.publishTime

            val typeLabel = if (item.type == Constants.ITEM_TYPE_LOST) "丢失" else "捡到"
            tvDescription.text = "${typeLabel}：${item.name}，${item.description}"

            val imageFiles = getImageFiles(item.imagePath)
            if (imageFiles.isEmpty()) {
                layoutImages.visibility = View.GONE
            } else {
                layoutImages.visibility = View.VISIBLE
                val requestOptions = RequestOptions()
                    .transform(RoundedCorners(12))
                    .centerCrop()
                Glide.with(itemView.context)
                    .load(imageFiles[0])
                    .apply(requestOptions)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivImage1)

                if (imageFiles.size > 1) {
                    ivImage2.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(imageFiles[1])
                        .apply(requestOptions)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivImage2)
                } else {
                    ivImage2.visibility = View.GONE
                }
            }

            val viewCount = (item.id % 37 + 10).toInt()
            tvViews.text = itemView.context.getString(R.string.views_count, viewCount)
            btnStatus.text = itemView.context.getString(R.string.not_claimed)

            itemView.setOnClickListener { onItemClick(item) }
        }

        private fun getImageFiles(imagePath: String): List<File> {
            val files = mutableListOf<File>()
            if (imagePath.isEmpty()) return files
            val mainFile = File(imagePath)
            if (mainFile.exists()) files.add(mainFile)
            val parent = mainFile.parentFile ?: return files
            val siblings = parent.listFiles { f ->
                f.name.startsWith(mainFile.nameWithoutExtension) && f != mainFile
            }
            siblings?.take(1)?.let { files.addAll(it) }
            return files
        }
    }
}