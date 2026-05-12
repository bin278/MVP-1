package com.campus.lostfound.sharedpref

import android.content.Context
import android.content.SharedPreferences
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.util.Md5Util

class UserManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    fun register(username: String, password: String): Boolean {
        val key = "user_$username"
        if (prefs.contains(key)) {
            return false
        }
        prefs.edit().putString(key, Md5Util.md5(password)).apply()
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

    fun logout() {
        prefs.edit()
            .putBoolean(Constants.KEY_LOGIN_STATUS, false)
            .remove(Constants.KEY_CURRENT_USER)
            .apply()
    }
}
