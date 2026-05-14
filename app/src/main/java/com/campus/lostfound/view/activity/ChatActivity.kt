package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.campus.lostfound.R
import com.campus.lostfound.ai.AiHelper
import com.campus.lostfound.api.RetrofitClient
import com.campus.lostfound.databinding.ActivityChatBinding
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.model.ChatMessage
import com.campus.lostfound.model.MatchItem
import com.campus.lostfound.model.MatchRequest
import com.campus.lostfound.model.MatchResponse
import com.campus.lostfound.view.adapter.ChatAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * AI 智能匹配对话页面
 * 用户通过自然语言描述丢失/捡到的物品，系统调用 DeepSeek API 进行语义匹配
 * 若后端不可用，则降级为本地关键词匹配
 */
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var itemDao: ItemDao
    private lateinit var aiHelper: AiHelper
    // 是否正在等待回复，防止重复发送
    private var isWaitingResponse = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemDao = ItemDao(this)
        aiHelper = AiHelper()

        // 初始化消息列表
        chatAdapter = ChatAdapter { matchItem -> onMatchItemClick(matchItem) }
        binding.rvChatMessages.layoutManager = LinearLayoutManager(this)
        binding.rvChatMessages.adapter = chatAdapter

        // 返回按钮
        binding.ivBack.setOnClickListener { finish() }

        // 发送按钮
        binding.btnSend.setOnClickListener { sendMessage() }

        // 快捷标签点击事件
        binding.tvQuickLost.setOnClickListener {
            sendQuickMessage(getString(R.string.ai_quick_lost))
        }
        binding.tvQuickFound.setOnClickListener {
            sendQuickMessage(getString(R.string.ai_quick_found))
        }
        binding.tvQuickBackpack.setOnClickListener {
            sendQuickMessage(getString(R.string.ai_quick_backpack))
        }
    }

    /**
     * 快捷标签快速发送
     */
    private fun sendQuickMessage(text: String) {
        if (isWaitingResponse) return
        binding.etInput.setText(text)
        sendMessage()
    }

    /**
     * 发送用户输入的消息并触发匹配
     */
    private fun sendMessage() {
        val text = binding.etInput.text.toString().trim()
        if (text.isEmpty() || isWaitingResponse) return

        // 添加用户消息到列表
        chatAdapter.addMessage(ChatMessage.UserMessage(text))
        // 添加加载中状态
        chatAdapter.addMessage(ChatMessage.Loading)
        // 清空输入框
        binding.etInput.setText("")
        // 滚动到底部
        binding.rvChatMessages.post {
            binding.rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }

        isWaitingResponse = true
        binding.btnSend.isEnabled = false

        // 尝试调用后端 AI 服务
        callAiApi(text)
    }

    /**
     * 调用后端 Flask 服务的 DeepSeek 匹配接口
     * 失败时降级为本地关键词匹配
     */
    private fun callAiApi(query: String) {
        RetrofitClient.instance.matchItems(MatchRequest(query))
            .enqueue(object : Callback<MatchResponse> {
                override fun onResponse(call: Call<MatchResponse>, response: Response<MatchResponse>) {
                    chatAdapter.removeLastIfLoading()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.error != null) {
                            // 后端返回了错误，降级到本地匹配
                            showFallbackResult(query)
                            return
                        }
                        chatAdapter.addMessage(ChatMessage.BotResult(body.summary, body.matches))
                    } else {
                        showFallbackResult(query)
                    }
                    resetSendState()
                    scrollToBottom()
                }

                override fun onFailure(call: Call<MatchResponse>, t: Throwable) {
                    chatAdapter.removeLastIfLoading()
                    // 网络失败，降级到本地关键词匹配
                    showFallbackResult(query)
                    resetSendState()
                    scrollToBottom()
                }
            })
    }

    /**
     * 本地关键词匹配降级方案
     * 当后端 AI 服务不可用时使用本地 AiHelper 进行简单匹配
     */
    private fun showFallbackResult(query: String) {
        val items = itemDao.queryAll()
        if (items.isEmpty()) {
            chatAdapter.addMessage(
                ChatMessage.BotResult(
                    summary = getString(R.string.ai_no_match),
                    matches = emptyList()
                )
            )
            return
        }

        // 使用 AiHelper 的 keyword 匹配 + 简单评分
        val keywords = query.split(" ", "，", "。", "、", "的", "在", "了", "是")
            .filter { it.length >= 2 }

        val matched = items.mapNotNull { item ->
            var score = 0
            val reasons = mutableListOf<String>()

            // 名称匹配
            for (kw in keywords) {
                if (item.name.contains(kw, ignoreCase = true)) {
                    score += 30
                    reasons.add("物品名称包含「$kw」")
                }
            }
            // 分类匹配
            val category = aiHelper.classify(query)
            if (category == item.category && category != "其他") {
                score += 20
                reasons.add("类别吻合")
            }
            // 描述匹配
            for (kw in keywords) {
                if (item.description.contains(kw, ignoreCase = true)) {
                    score += 15
                }
            }
            // 地点关键词匹配
            val locationKeywords = listOf("图书馆", "食堂", "操场", "教学楼", "体育馆", "自习室", "宿舍")
            for (loc in locationKeywords) {
                if (query.contains(loc) && item.location.contains(loc)) {
                    score += 20
                    reasons.add("地点「$loc」一致")
                    break
                }
            }

            if (score >= 30) {
                MatchItem(
                    id = item.id,
                    score = score.coerceAtMost(95),
                    reason = reasons.take(2).joinToString("；").ifEmpty { "关键词匹配" },
                    suggestion = "请查看详情确认是否为您寻找的物品"
                )
            } else null
        }.sortedByDescending { it.score }.take(3)

        val summary = if (matched.isNotEmpty()) {
            "${getString(R.string.ai_service_unavailable)}\n共找到 ${matched.size} 条可能匹配："
        } else {
            getString(R.string.ai_no_match)
        }

        chatAdapter.addMessage(ChatMessage.BotResult(summary = summary, matches = matched))
    }

    /**
     * 用户点击"查看详情"跳转到物品详情页
     */
    private fun onMatchItemClick(matchItem: MatchItem) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("item_id", matchItem.id)
        startActivity(intent)
    }

    /**
     * 滚动 RecyclerView 到底部
     */
    private fun scrollToBottom() {
        binding.rvChatMessages.post {
            val lastIndex = chatAdapter.itemCount - 1
            if (lastIndex >= 0) {
                binding.rvChatMessages.smoothScrollToPosition(lastIndex)
            }
        }
    }

    /**
     * 重置发送状态，恢复按钮和标志位
     */
    private fun resetSendState() {
        isWaitingResponse = false
        binding.btnSend.isEnabled = true
    }
}
