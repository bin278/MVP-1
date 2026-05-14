package com.campus.lostfound.model

/**
 * 对话消息数据类，支持三种消息类型
 */
sealed class ChatMessage {
    /** 用户输入的消息 */
    data class UserMessage(val text: String) : ChatMessage()

    /** AI 助手返回的匹配结果 */
    data class BotResult(
        val summary: String,
        val matches: List<MatchItem>
    ) : ChatMessage()

    /** 加载中状态 */
    data object Loading : ChatMessage()
}
