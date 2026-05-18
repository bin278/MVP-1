package com.campus.lostfound.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间工具类
 * 提供时间格式化、解析和相对时间计算功能
 */
object TimeUtil {
    // 日期格式器：yyyy-MM-dd
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    // 日期时间格式器：yyyy-MM-dd HH:mm:ss
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    /**
     * 将时间戳格式化为完整的日期时间字符串
     * @param timestamp 毫秒时间戳
     * @return 格式化后的日期时间字符串，如 "2024-01-15 14:30:00"
     */
    fun formatTimestamp(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    /**
     * 获取当前时间戳
     * @return 当前毫秒时间戳
     */
    fun currentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * 将时间戳格式化为日期字符串
     * @param timestamp 毫秒时间戳
     * @return 格式化后的日期字符串，如 "2024-01-15"
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 将日期字符串解析为时间戳
     * @param dateStr 日期字符串，格式为 "yyyy-MM-dd"
     * @return 对应的毫秒时间戳，解析失败返回 0
     */
    fun parseDate(dateStr: String): Long {
        return try {
            dateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 将时间戳转为相对时间描述（如 "3 小时前"、"刚刚"、"2 天前"）
     * @param timestamp 毫秒时间戳
     * @return 相对时间字符串
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        // 处理未来时间
        if (diff < 0) return "刚刚"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 30 -> formatDate(timestamp)     // 超过30天显示具体日期
            days > 0 -> "${days} 天前"             // 显示天数
            hours > 0 -> "${hours} 小时前"         // 显示小时数
            minutes > 0 -> "${minutes} 分钟前"     // 显示分钟数
            else -> "刚刚"                          // 刚刚发布
        }
    }
}