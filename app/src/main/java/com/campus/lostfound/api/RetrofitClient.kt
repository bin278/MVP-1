package com.campus.lostfound.api

import android.os.Build
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit 客户端单例
 * 用于创建 API 请求实例
 * 
 * 地址说明：
 * - 模拟器：使用 10.0.2.2 映射宿主机的 localhost
 * - 真机：使用电脑的局域网 IP（手机和电脑必须连同一个 WiFi）
 * 
 * 自动判断：通过 Build.FINGERPRINT 是否包含 "generic" 判断是否为模拟器
 */
object RetrofitClient {

    // 电脑的局域网 IP（真机使用时需要改成你电脑的实际 IP）
    // 获取方法：命令行输入 ipconfig，看"无线局域网适配器 WLAN"的 IPv4 地址
    // 例如 Flask 启动时显示的：http://192.168.5.10:5000
    private const val REAL_DEVICE_IP = "192.168.5.10"
    // Flask 服务端口
    private const val SERVER_PORT = 5000

    // 自动检测：模拟器用 10.0.2.2，真机用局域网 IP
    private val BASE_URL: String by lazy {
        if (isEmulator()) {
            "http://10.0.2.2:$SERVER_PORT/"
        } else {
            "http://$REAL_DEVICE_IP:$SERVER_PORT/"
        }
    }

    // 懒加载创建 Retrofit 实例
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * 判断当前是否在模拟器上运行
     * 通过检查系统指纹（FINGERPRINT）、型号（MODEL）、制造商（MANUFACTURER）判断
     * @return true 表示运行在模拟器上
     */
    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER

        return (
            fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            model.contains("google_sdk") ||
            model.contains("Emulator") ||
            model.contains("Android SDK built for x86") ||
            manufacturer.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        )
    }
}
