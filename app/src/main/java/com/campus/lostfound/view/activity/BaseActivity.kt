package com.campus.lostfound.view.activity

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 所有 Activity 的基类
 * 提供统一的基础配置和功能扩展
 */
open class BaseActivity : AppCompatActivity() {

    /**
     * 显示 Toast 消息
     * @param message 要显示的消息内容
     * @param duration 显示时长，默认短时间
     */
    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}
