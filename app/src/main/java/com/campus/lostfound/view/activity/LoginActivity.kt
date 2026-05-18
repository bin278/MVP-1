package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.campus.lostfound.R
import com.campus.lostfound.databinding.ActivityLoginBinding
import com.campus.lostfound.sharedpref.UserManager

/**
 * 登录页面
 * 处理用户登录逻辑，包括用户名密码验证和自动登录检查
 * 使用 Firebase 进行用户认证
 */
class LoginActivity : BaseActivity() {

    // ViewBinding实例，用于访问布局中的视图
    private lateinit var binding: ActivityLoginBinding
    // 用户管理器，处理用户登录状态和验证
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userManager = UserManager(this)

        // 检查用户是否已登录，如果已登录直接跳转到主页面
        if (userManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 初始化视图绑定
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置登录按钮点击事件
        binding.btnLogin.setOnClickListener { performLogin() }

        // 设置"去注册"文本点击事件，跳转到注册页面
        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * 执行登录操作
     * 验证用户名和密码，调用UserManager进行Firebase登录验证
     */
    private fun performLogin() {
        // 获取用户名和密码输入
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // 验证输入不为空
        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.input_required))
            return
        }

        // 显示加载状态
        showLoading(true)

        // 使用 Firebase 异步登录
        userManager.login(username, password) { success ->
            if (success) {
                // 登录成功，显示提示并跳转到主页面
                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // 登录失败，显示错误信息
                showError(getString(R.string.login_fail))
                showLoading(false)
            }
        }
    }

    /**
     * 显示或隐藏加载状态
     * @param loading 是否显示加载中状态
     */
    private fun showLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    /**
     * 显示错误提示
     * @param message 错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}