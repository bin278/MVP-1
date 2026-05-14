package com.campus.lostfound.view.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.campus.lostfound.R
import com.campus.lostfound.model.ChatMessage
import com.campus.lostfound.model.MatchItem
import com.campus.lostfound.view.activity.DetailActivity
import com.google.android.material.card.MaterialCardView

/**
 * AI 对话消息适配器
 * 支持三种视图类型：用户消息、加载中、AI 匹配结果
 */
class ChatAdapter(
    private val onMatchItemClick: (MatchItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val TYPE_USER = 0      // 用户消息（右侧气泡）
        private const val TYPE_LOADING = 1   // 加载中动画
        private const val TYPE_BOT_RESULT = 2 // AI 匹配结果卡片
    }

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLastIfLoading() {
        if (messages.isNotEmpty() && messages.last() is ChatMessage.Loading) {
            val lastIndex = messages.size - 1
            messages.removeAt(lastIndex)
            notifyItemRemoved(lastIndex)
        }
    }

    fun clear() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ChatMessage.UserMessage -> TYPE_USER
            is ChatMessage.Loading -> TYPE_LOADING
            is ChatMessage.BotResult -> TYPE_BOT_RESULT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            }
            TYPE_LOADING -> {
                val view = inflater.inflate(R.layout.item_chat_loading, parent, false)
                LoadingViewHolder(view)
            }
            TYPE_BOT_RESULT -> {
                val view = inflater.inflate(R.layout.item_chat_bot, parent, false)
                BotResultViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val msg = messages[position]) {
            is ChatMessage.UserMessage -> (holder as UserViewHolder).bind(msg.text)
            is ChatMessage.Loading -> { /* 加载动画由布局自身处理 */ }
            is ChatMessage.BotResult -> (holder as BotResultViewHolder).bind(msg, onMatchItemClick)
        }
    }

    override fun getItemCount() = messages.size

    // ---------- ViewHolder 内部类 ----------

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvUserMessage)

        fun bind(text: String) {
            tvText.text = text
        }
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class BotResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
        private val container: ViewGroup = itemView.findViewById(R.id.matchContainer)

        fun bind(msg: ChatMessage.BotResult, onItemClick: (MatchItem) -> Unit) {
            tvSummary.text = msg.summary
            container.removeAllViews()

            msg.matches.forEach { match ->
                val card = createMatchCard(match, onItemClick)
                container.addView(card)
            }
        }

        /**
         * 动态创建单个匹配结果卡片
         */
        private fun createMatchCard(match: MatchItem, onItemClick: (MatchItem) -> Unit): View {
            val ctx = itemView.context
            val card = LayoutInflater.from(ctx)
                .inflate(R.layout.item_match_card, container, false) as MaterialCardView

            card.findViewById<TextView>(R.id.tvMatchScore).text = "匹配度 ${match.score}%"
            card.findViewById<TextView>(R.id.tvMatchReason).text = match.reason
            card.findViewById<TextView>(R.id.tvMatchSuggestion).text = match.suggestion

            // 根据匹配度设置卡片边框颜色
            val borderColor = when {
                match.score >= 90 -> R.color.green
                match.score >= 70 -> R.color.primary
                else -> R.color.gray
            }
            card.strokeColor = ContextCompat.getColor(ctx, borderColor)
            card.strokeWidth = 2

            card.findViewById<TextView>(R.id.tvViewDetail).setOnClickListener {
                onItemClick(match)
            }

            return card
        }
    }
}
