package com.campus.lostfound

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * 自定义应用类
 * 在应用启动时进行初始化并记录日志
 */
class LostFoundApp : Application() {
    
    companion object {
        const val TAG = "LostFoundApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "==================== 应用启动 ====================")
        Log.d(TAG, "应用包名: ${applicationInfo.packageName}")
        Log.d(TAG, "Android版本: ${android.os.Build.VERSION.SDK_INT}")
        
        // 尝试初始化 Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ FirebaseApp.initializeApp() 调用成功")
            
            val apps = FirebaseApp.getApps(this)
            Log.d(TAG, "Firebase 应用数量: ${apps.size}")
            apps.forEachIndexed { index, app ->
                Log.d(TAG, "  应用[$index]: ${app.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase 初始化失败: ${e.message}")
            e.printStackTrace()
        }
        
        Log.d(TAG, "==================== 初始化完成 ====================")
    }
}
