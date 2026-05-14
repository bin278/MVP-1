package com.campus.lostfound.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    fun formatTimestamp(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun currentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

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

        if (diff < 0) return "刚刚"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 30 -> formatDate(timestamp)
            days > 0 -> "${days} 天前"
            hours > 0 -> "${hours} 小时前"
            minutes > 0 -> "${minutes} 分钟前"
            else -> "刚刚"
        }
    }
}
