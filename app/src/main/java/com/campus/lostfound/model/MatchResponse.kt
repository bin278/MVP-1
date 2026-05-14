package com.campus.lostfound.model

/**
 * 智能匹配响应体（DeepSeek API 返回的匹配结果）
 */
data class MatchResponse(
    val matches: List<MatchItem> = emptyList(),
    val summary: String = "",
    val error: String? = null
)
