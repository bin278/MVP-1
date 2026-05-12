package com.campus.lostfound.view.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.model.Item
import java.io.File

class ItemAdapter(private val onItemClick: (Item) -> Unit) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivImage: ImageView = view.findViewById(R.id.ivImage)
        private val tvType: TextView = view.findViewById(R.id.tvType)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(item: Item) {
            tvName.text = item.name

            val displayLocation = if (item.addressText.isNotEmpty()) item.addressText else item.location
            tvLocation.text = displayLocation.ifEmpty { "未标记地点" }
            tvTime.text = item.publishTime

            if (item.type == Constants.ITEM_TYPE_LOST) {
                tvType.text = "失物"
                val bg = GradientDrawable().apply {
                    cornerRadius = 20f
                    setColor(itemView.context.getColor(R.color.lost_tag))
                }
                tvType.background = bg
            } else {
                tvType.text = "招领"
                val bg = GradientDrawable().apply {
                    cornerRadius = 20f
                    setColor(itemView.context.getColor(R.color.found_tag))
                }
                tvType.background = bg
            }

            if (item.imagePath.isNotEmpty()) {
                val file = File(item.imagePath)
                if (file.exists()) {
                    Glide.with(itemView.context)
                        .load(file)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivImage)
                } else {
                    ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClickListener?.invoke(item)
                true
            }
        }
    }
}
