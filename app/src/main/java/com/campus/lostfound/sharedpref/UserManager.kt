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
        val campus: String = ""
    )

    fun register(username: String, password: String, nickname: String = "", studentId: String = "", campus: String = ""): Boolean {
        val key = "user_$username"
        if (prefs.contains(key)) {
            return false
        }
        prefs.edit()
            .putString(key, Md5Util.md5(password))
            .putString("${key}_nickname", nickname)
            .putString("${key}_student_id", studentId)
            .putString("${key}_campus", campus)
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
            campus = prefs.getString("${key}_campus", "") ?: ""
        )
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

    fun logout() {
        prefs.edit()
            .putBoolean(Constants.KEY_LOGIN_STATUS, false)
            .remove(Constants.KEY_CURRENT_USER)
            .apply()
    }
}
