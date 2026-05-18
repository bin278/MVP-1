package com.campus.lostfound.sharedpref

import android.content.Context
import android.content.SharedPreferences
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.util.Md5Util

/**
 * 用户管理类
 * 负责用户注册、登录、登出和用户信息管理
 * 使用 SharedPreferences 存储用户数据
 */
class UserManager(context: Context) {

    // SharedPreferences 实例
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    /**
     * 用户信息数据类
     * @param username 用户名（登录账号）
     * @param nickname 昵称（显示名称）
     * @param studentId 学号
     * @param campus 校区
     * @param avatarPath 头像文件路径
     */
    data class UserInfo(
        val username: String,
        val nickname: String = "",
        val studentId: String = "",
        val campus: String = "",
        val avatarPath: String = ""
    )

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码（会进行MD5加密存储）
     * @param nickname 昵称（可选）
     * @param studentId 学号（可选）
     * @param campus 校区（可选）
     * @param avatarPath 头像路径（可选）
     * @return 注册成功返回true，用户名已存在返回false
     */
    fun register(username: String, password: String, nickname: String = "", studentId: String = "", campus: String = "", avatarPath: String = ""): Boolean {
        val key = "user_$username"
        // 检查用户名是否已存在
        if (prefs.contains(key)) {
            return false
        }
        // 存储用户信息，密码使用MD5加密
        prefs.edit()
            .putString(key, Md5Util.md5(password))
            .putString("${key}_nickname", nickname)
            .putString("${key}_student_id", studentId)
            .putString("${key}_campus", campus)
            .putString("${key}_avatar", avatarPath)
            .apply()
        return true
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回true，用户名不存在或密码错误返回false
     */
    fun login(username: String, password: String): Boolean {
        val key = "user_$username"
        // 检查用户名是否存在
        val stored = prefs.getString(key, null) ?: return false
        // 验证密码（MD5比对）
        if (stored != Md5Util.md5(password)) return false
        // 设置登录状态和当前用户
        prefs.edit()
            .putBoolean(Constants.KEY_LOGIN_STATUS, true)
            .putString(Constants.KEY_CURRENT_USER, username)
            .apply()
        return true
    }

    /**
     * 检查用户是否已登录
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(Constants.KEY_LOGIN_STATUS, false)
    }

    /**
     * 获取当前登录用户的用户名
     * @return 当前用户名，未登录返回空字符串
     */
    fun getCurrentUser(): String {
        return prefs.getString(Constants.KEY_CURRENT_USER, "") ?: ""
    }

    /**
     * 获取用户详细信息
     * @param username 用户名（可选，不传则获取当前登录用户）
     * @return 用户信息对象
     */
    fun getUserInfo(username: String? = null): UserInfo {
        val user = username ?: getCurrentUser()
        val key = "user_$user"
        return UserInfo(
            username = user,
            nickname = prefs.getString("${key}_nickname", "") ?: "",
            studentId = prefs.getString("${key}_student_id", "") ?: "",
            campus = prefs.getString("${key}_campus", "") ?: "",
            avatarPath = prefs.getString("${key}_avatar", "") ?: ""
        )
    }

    /**
     * 更新当前登录用户的个人信息（昵称、学号、校区）
     * @param nickname 新的昵称
     * @param studentId 新的学号
     * @param campus 新的校区
     * @return 是否更新成功
     */
    fun updateUserInfo(nickname: String, studentId: String, campus: String): Boolean {
        val username = getCurrentUser()
        if (username.isEmpty()) return false
        val key = "user_$username"
        prefs.edit()
            .putString("${key}_nickname", nickname)
            .putString("${key}_student_id", studentId)
            .putString("${key}_campus", campus)
            .commit()
        return true
    }

    /**
     * 更新当前登录用户的头像路径
     * 使用 commit() 同步写入，确保保存后立即能被读取
     * @param avatarPath 新的头像文件路径
     * @return 是否更新成功
     */
    fun updateAvatar(avatarPath: String): Boolean {
        val username = getCurrentUser()
        if (username.isEmpty()) return false
        val key = "user_$username"
        prefs.edit()
            .putString("${key}_avatar", avatarPath)
            .commit()
        return true
    }

    /**
     * 获取当前登录用户的昵称
     * @return 昵称，未设置返回空字符串
     */
    fun getCurrentNickname(): String {
        return getUserInfo().nickname
    }

    /**
     * 获取当前登录用户的学号
     * @return 学号，未设置返回空字符串
     */
    fun getCurrentStudentId(): String {
        return getUserInfo().studentId
    }

    /**
     * 获取当前登录用户的校区
     * @return 校区，未设置返回空字符串
     */
    fun getCurrentCampus(): String {
        return getUserInfo().campus
    }

    /**
     * 获取当前登录用户的头像路径
     * @return 头像路径，未设置返回空字符串
     */
    fun getCurrentAvatarPath(): String {
        return getUserInfo().avatarPath
    }

    /**
     * 用户登出
     * 清除登录状态和当前用户信息
     */
    fun logout() {
        prefs.edit()
            .putBoolean(Constants.KEY_LOGIN_STATUS, false)
            .remove(Constants.KEY_CURRENT_USER)
            .apply()
    }
}