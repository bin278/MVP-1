package com.campus.lostfound.util

import java.security.MessageDigest

/**
 * MD5加密工具类
 * 提供字符串的MD5加密功能，用于密码等敏感信息的加密存储
 */
object Md5Util {

    /**
     * 对输入字符串进行MD5加密
     * @param input 要加密的字符串
     * @return 加密后的32位十六进制字符串
     */
    fun md5(input: String): String {
        // 获取MD5消息摘要实例
        val digest = MessageDigest.getInstance("MD5")
        // 计算消息摘要，返回字节数组
        val bytes = digest.digest(input.toByteArray())
        // 将字节数组转换为十六进制字符串
        return bytes.joinToString("") { "%02x".format(it) }
    }
}