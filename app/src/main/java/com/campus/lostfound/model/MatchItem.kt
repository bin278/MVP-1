package com.campus.lostfound.model

/**
 * 单条匹配结果条目
 * @param id 物品数据库ID
 * @param score 匹配度分数 (0-100)
 * @param reason 匹配理由
 * @param suggestion 给用户的建议
 */
data class MatchItem(
    val id: Long,
    val score: Int,
    val reason: String,
    val suggestion: String
)
