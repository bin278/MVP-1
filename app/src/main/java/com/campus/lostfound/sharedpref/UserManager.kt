package com.campus.lostfound.sharedpref

import android.content.Context
import android.content.SharedPreferences
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.firebase.FirebaseHelper
import com.campus.lostfound.firebase.User

/**
 * 用户管理类
 * 负责用户注册、登录、登出和用户信息管理
 * 使用 Firebase Realtime Database 存储用户数据，SharedPreferences 存储登录状态
 */
class UserManager(context: Context) {

    // SharedPreferences 实例，用于存储登录状态
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    /**
     * 用户信息数据类
     * @param username 用户名（登录账号）
     * @param nickname 昵称（显示名称）
     * @param studentId 学号
     * @param campus 校区
     * @param avatarPath 头像文件路径
     * @param userId 用户ID（Firebase生成）
     */
    data class UserInfo(
        val username: String,
        val nickname: String = "",
        val studentId: String = "",
        val campus: String = "",
        val avatarPath: String = "",
        val userId: String = ""
    )

    /**
     * 用户注册（使用 Firebase）
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称（可选）
     * @param studentId 学号（可选）
     * @param campus 校区（可选）
     * @param avatarPath 头像路径（可选）
     * @param callback 回调，返回是否成功和错误信息
     */
    fun register(username: String, password: String, nickname: String = "", studentId: String = "", campus: String = "", avatarPath: String = "", callback: (Boolean, String?) -> Unit) {
        val user = User(
            username = username,
            password = password,
            nickname = nickname,
            studentId = studentId,
            campus = campus,
            avatarUrl = avatarPath
        )
        FirebaseHelper.register(user) { success, userId ->
            if (success) {
                callback(true, userId)
            } else {
                callback(false, userId)
            }
        }
    }

    /**
     * 用户登录（使用 Firebase）
     * @param username 用户名
     * @param password 密码
     * @param callback 回调，返回是否成功
     */
    fun login(username: String, password: String, callback: (Boolean) -> Unit) {
        FirebaseHelper.login(username, password) { user ->
            if (user != null) {
                // 登录成功，保存登录状态和用户信息
                prefs.edit()
                    .putBoolean(Constants.KEY_LOGIN_STATUS, true)
                    .putString(Constants.KEY_CURRENT_USER, username)
                    .putString(Constants.KEY_CURRENT_USER_ID, user.id)
                    .putString("user_${username}_nickname", user.nickname ?: "")
                    .putString("user_${username}_student_id", user.studentId ?: "")
                    .putString("user_${username}_campus", user.campus ?: "")
                    .putString("user_${username}_avatar", user.avatarUrl ?: "")
                    .apply()
                callback(true)
            } else {
                callback(false)
            }
        }
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
     * 获取当前登录用户的ID
     * @return 用户ID，未登录返回空字符串
     */
    fun getUserId(): String {
        return prefs.getString(Constants.KEY_CURRENT_USER_ID, "") ?: ""
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
            avatarPath = prefs.getString("${key}_avatar", "") ?: "",
            userId = getUserId()
        )
    }

    /**
     * 更新当前登录用户的个人信息（昵称、学号、校区）
     * 同时更新 Firebase 中的用户数据
     * @param nickname 新的昵称
     * @param studentId 新的学号
     * @param campus 新的校区
     * @param callback 回调，返回是否更新成功
     */
    fun updateUserInfo(nickname: String, studentId: String, campus: String, callback: (Boolean) -> Unit) {
        val username = getCurrentUser()
        val userId = getUserId()
        if (username.isEmpty() || userId.isEmpty()) {
            callback(false)
            return
        }

        // 先更新本地缓存
        val key = "user_$username"
        prefs.edit()
            .putString("${key}_nickname", nickname)
            .putString("${key}_student_id", studentId)
            .putString("${key}_campus", campus)
            .commit()

        // 更新 Firebase
        val user = getUserInfo(username)
        val firebaseUser = User(
            id = userId,
            username = username,
            nickname = nickname,
            studentId = studentId,
            campus = campus,
            avatarUrl = user.avatarPath
        )
        FirebaseHelper.updateUser(firebaseUser) { success ->
            callback(success)
        }
    }

    /**
     * 更新当前登录用户的头像路径
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

        // 同时更新 Firebase
        val userId = getUserId()
        if (userId.isNotEmpty()) {
            val user = getUserInfo(username)
            val firebaseUser = User(
                id = userId,
                username = username,
                nickname = user.nickname,
                studentId = user.studentId,
                campus = user.campus,
                avatarUrl = avatarPath
            )
            FirebaseHelper.updateUser(firebaseUser) {}
        }
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
     * 获取当前登录用户的校区（别名方法，兼容Firebase使用）
     * @return 校区，未设置返回空字符串
     */
    fun getUserCampus(): String {
        return getCurrentCampus()
    }

    /**
     * 获取当前登录用户的名称（优先返回昵称，没有则返回用户名）
     * @return 用户名称
     */
    fun getUserName(): String {
        val userInfo = getUserInfo()
        return if (userInfo.nickname.isNotEmpty()) userInfo.nickname else userInfo.username
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
            .remove(Constants.KEY_CURRENT_USER_ID)
            .apply()
    }
}