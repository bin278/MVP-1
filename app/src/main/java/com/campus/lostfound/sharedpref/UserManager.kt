package com.campus.lostfound.sharedpref

import android.content.Context
import android.content.SharedPreferences
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.util.Md5Util

class UserManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    data class UserInfo(
        val username: String,
        val nickname: String = "",
        val studentId: String = "",
        val campus: String = "",
        val avatarPath: String = ""
    )

    fun register(username: String, password: String, nickname: String = "", studentId: String = "", campus: String = "", avatarPath: String = ""): Boolean {
        val key = "user_$username"
        if (prefs.contains(key)) {
            return false
        }
        prefs.edit()
            .putString(key, Md5Util.md5(password))
            .putString("${key}_nickname", nickname)
            .putString("${key}_student_id", studentId)
            .putString("${key}_campus", campus)
            .putString("${key}_avatar", avatarPath)
            .apply()
        return true
    }

    fun login(username: String, password: String): Boolean {
        val key = "user_$username"
        val stored = prefs.getString(key, null) ?: return false
        if (stored != Md5Util.md5(password)) return false
        prefs.edit()
            .putBoolean(Constants.KEY_LOGIN_STATUS, true)
            .putString(Constants.KEY_CURRENT_USER, username)
            .apply()
        return true
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(Constants.KEY_LOGIN_STATUS, false)
    }

    fun getCurrentUser(): String {
        return prefs.getString(Constants.KEY_CURRENT_USER, "") ?: ""
    }

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

    fun getCurrentNickname(): String {
        return getUserInfo().nickname
    }

    fun getCurrentStudentId(): String {
        return getUserInfo().studentId
    }

    fun getCurrentCampus(): String {
        return getUserInfo().campus
    }

    fun getCurrentAvatarPath(): String {
        return getUserInfo().avatarPath
    }

    fun logout() {
        prefs.edit()
            .putBoolean(Constants.KEY_LOGIN_STATUS, false)
            .remove(Constants.KEY_CURRENT_USER)
            .apply()
    }
}