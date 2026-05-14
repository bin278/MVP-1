package com.campus.lostfound.model

/**
 * 智能匹配请求体
 * @param query 用户的自然语言查询描述
 */
data class MatchRequest(
    val query: String
)
