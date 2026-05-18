package com.campus.lostfound.view.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.databinding.ActivityRegisterBinding
import com.campus.lostfound.sharedpref.UserManager
import java.io.File
import java.io.FileOutputStream

/**
 * 注册页面
 * 处理用户注册逻辑，支持用户名、昵称、学号、校区和头像的设置
 */
class RegisterActivity : BaseActivity() {

    // ViewBinding实例，用于访问布局中的视图
    private lateinit var binding: ActivityRegisterBinding
    // 用户管理器，处理用户注册逻辑
    private lateinit var userManager: UserManager
    // 用户头像文件路径
    private var avatarPath: String = ""

    /**
     * 相机拍照结果回调
     * 处理拍照后的头像保存和预览
     */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            cameraAvatarUri?.let { uri ->
                val savedPath = saveAvatarFromUri(uri)
                if (savedPath != null) {
                    avatarPath = savedPath
                    showAvatarPreview()
                }
            }
            cameraAvatarUri = null
        }
    }

    /**
     * 相册选择结果回调
     * 处理从相册选择图片后的头像保存和预览
     */
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val savedPath = saveAvatarFromUri(uri)
                if (savedPath != null) {
                    avatarPath = savedPath
                    showAvatarPreview()
                }
            }
        }
    }

    // 相机拍照时的临时URI
    private var cameraAvatarUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userManager = UserManager(this)

        // 初始化视图绑定
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置校区选择适配器
        val campusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.CAMPUSES)
        binding.actvCampus.setAdapter(campusAdapter)

        // 设置头像点击事件（卡片和文字都可点击）
        val avatarClickListener = { showAvatarPickerDialog() }
        binding.cardAvatar.setOnClickListener { avatarClickListener() }
        binding.tvChangeAvatar.setOnClickListener { avatarClickListener() }

        // 设置注册按钮点击事件
        binding.btnRegister.setOnClickListener { performRegister() }

        // 设置"去登录"文本点击事件，返回登录页面
        binding.tvGoLogin.setOnClickListener { finish() }
    }

    /**
     * 执行注册操作
     * 验证所有输入字段，调用UserManager完成注册
     */
    private fun performRegister() {
        // 获取用户输入
        val username = binding.etUsername.text.toString().trim()
        val nickname = binding.etNickname.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val campus = binding.actvCampus.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // 验证必填字段不为空
        if (username.isEmpty() || password.isEmpty() || nickname.isEmpty() || studentId.isEmpty() || campus.isEmpty()) {
            showError(getString(R.string.input_required))
            return
        }

        // 验证密码长度（至少6位）
        if (password.length < 6) {
            showError(getString(R.string.password_too_short))
            return
        }

        // 验证两次密码输入一致
        if (password != confirmPassword) {
            showError(getString(R.string.password_mismatch))
            return
        }

        // 显示加载状态
        showLoading(true)

        // 模拟异步注册操作（延迟300ms）
        binding.root.postDelayed({
            try {
                // 调用UserManager进行注册
                if (userManager.register(username, password, nickname, studentId, campus, avatarPath)) {
                    // 注册成功，显示提示并跳转到登录页面
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    // 注册失败（用户名已存在）
                    showError(getString(R.string.register_fail))
                    showLoading(false)
                }
            } catch (e: Exception) {
                // 处理异常情况
                showError(getString(R.string.register_fail))
                showLoading(false)
            }
        }, 300)
    }

    /**
     * 显示或隐藏加载状态
     * @param loading 是否显示加载中状态
     */
    private fun showLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    /**
     * 显示错误提示
     * @param message 错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示头像选择对话框
     * 提供相机拍照和相册选择两种方式
     */
    private fun showAvatarPickerDialog() {
        val options = arrayOf(getString(R.string.select_from_camera), getString(R.string.select_from_gallery))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_avatar))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCameraForAvatar()    // 从相机拍照
                    1 -> openGalleryForAvatar()    // 从相册选择
                }
            }
            .show()
    }

    /**
     * 打开相机拍照获取头像
     */
    private fun openCameraForAvatar() {
        try {
            // 创建图片保存目录
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constants.IMAGE_DIR)
            if (!dir.exists()) dir.mkdirs()
            // 创建临时图片文件
            val imageFile = File(dir, "AVATAR_${System.currentTimeMillis()}.jpg")
            // 获取文件URI（使用FileProvider兼容Android 7.0+）
            cameraAvatarUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
            // 启动相机拍照
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraAvatarUri)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开相册选择头像
     */
    private fun openGalleryForAvatar() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    /**
     * 将URI中的图片保存到本地文件
     * @param uri 图片URI
     * @return 保存后的文件路径，失败返回null
     */
    private fun saveAvatarFromUri(uri: Uri): String? {
        return try {
            // 创建保存目录
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constants.IMAGE_DIR)
            if (!dir.exists()) dir.mkdirs()
            // 创建目标文件
            val destFile = File(dir, "AVATAR_${System.currentTimeMillis()}.jpg")
            // 复制文件
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 显示头像预览
     * 使用Glide加载并显示圆形头像
     */
    private fun showAvatarPreview() {
        Glide.with(this)
            .load(File(avatarPath))
            .circleCrop()
            .placeholder(android.R.drawable.ic_menu_camera)
            .into(binding.ivAvatar)
    }
}