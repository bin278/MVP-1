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
import com.campus.lostfound.databinding.ActivityEditProfileBinding
import com.campus.lostfound.sharedpref.UserManager
import java.io.File
import java.io.FileOutputStream

/**
 * 编辑个人信息页面
 * 用户可以在此修改头像、昵称、学号和校区，用户名不可修改
 */
class EditProfileActivity : BaseActivity() {

    // ViewBinding 用于安全访问视图
    private lateinit var binding: ActivityEditProfileBinding
    // 用户管理器，负责读写用户数据
    private lateinit var userManager: UserManager
    // 当前选中的头像文件路径
    private var avatarPath: String = ""

    // 拍照返回结果处理器
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

    // 相册选择返回结果处理器
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

    // 拍照时临时保存的 Uri
    private var cameraAvatarUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userManager = UserManager(this)

        // 使用 ViewBinding 初始化视图，替代 findViewById
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 加载当前用户信息到表单（包括头像）
        loadCurrentUserInfo()
        // 设置校区下拉选择器
        setupCampusDropdown()
        // 设置头像点击事件
        setupAvatarClick()
        // 设置保存按钮点击事件
        binding.btnSave.setOnClickListener { performSave() }
    }

    /**
     * 从 SharedPreferences 加载当前用户的个人信息并填入对应输入框
     * 用户名设为只读状态，不允许修改
     * 同时加载当前头像
     */
    private fun loadCurrentUserInfo() {
        val userInfo = userManager.getUserInfo()
        binding.etUsername.setText(userInfo.username)
        binding.etNickname.setText(userInfo.nickname)
        binding.etStudentId.setText(userInfo.studentId)
        // 校区下拉框需要先设置文本再设置适配器
        binding.actvCampus.setText(userInfo.campus)

        // 加载已有头像路径
        avatarPath = userInfo.avatarPath

        // 如果头像文件存在则显示，否则显示默认头像
        if (avatarPath.isNotEmpty()) {
            val avatarFile = File(avatarPath)
            if (avatarFile.exists()) {
                showAvatarPreview()
            }
        }
    }

    /**
     * 设置头像点击事件：点击头像卡片或"选择头像"文字均可触发
     */
    private fun setupAvatarClick() {
        val avatarClickListener = { showAvatarPickerDialog() }
        binding.cardAvatar.setOnClickListener { avatarClickListener() }
        binding.tvChangeAvatar.setOnClickListener { avatarClickListener() }
    }

    /**
     * 弹出头像选择对话框，用户可选择拍照或从相册选择
     */
    private fun showAvatarPickerDialog() {
        val options = arrayOf(
            getString(R.string.select_from_camera),
            getString(R.string.select_from_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_avatar))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCameraForAvatar()   // 拍照
                    1 -> openGalleryForAvatar()  // 从相册选择
                }
            }
            .show()
    }

    /**
     * 打开系统相机拍照获取头像
     */
    private fun openCameraForAvatar() {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constants.IMAGE_DIR)
            if (!dir.exists()) dir.mkdirs()
            val imageFile = File(dir, "AVATAR_${System.currentTimeMillis()}.jpg")
            cameraAvatarUri = FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", imageFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraAvatarUri)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开系统相册选择头像
     */
    private fun openGalleryForAvatar() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    /**
     * 将 Uri 指向的图片保存到应用私有目录，返回保存后的文件路径
     * @param uri 来源图片的 Uri
     * @return 保存后的文件绝对路径，失败返回 null
     */
    private fun saveAvatarFromUri(uri: Uri): String? {
        return try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constants.IMAGE_DIR)
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "AVATAR_${System.currentTimeMillis()}.jpg")
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
     * 使用 Glide 加载头像预览，裁剪为圆形
     */
    private fun showAvatarPreview() {
        Glide.with(this)
            .load(File(avatarPath))
            .circleCrop()
            .placeholder(R.drawable.bg_avatar_default)
            .into(binding.ivAvatar)
    }

    /**
     * 为校区输入框配置下拉选择适配器
     * 使用 Constants 中定义的校区列表
     */
    private fun setupCampusDropdown() {
        val campusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            Constants.CAMPUSES
        )
        // 给校区下拉框设置阈值，0 表示输入任意字符即显示下拉
        binding.actvCampus.setAdapter(campusAdapter)
        binding.actvCampus.threshold = 0
    }

    /**
     * 执行保存操作：校验输入 → 保存头像 → 显示加载 → 调用 UserManager 更新 → 返回结果
     */
    private fun performSave() {
        val nickname = binding.etNickname.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val campus = binding.actvCampus.text.toString().trim()

        // 校验：昵称、学号、校区均为必填项
        if (nickname.isEmpty() || studentId.isEmpty() || campus.isEmpty()) {
            Toast.makeText(this, getString(R.string.input_required), Toast.LENGTH_SHORT).show()
            return
        }

        // 校验校区是否在合法范围内
        if (campus !in Constants.CAMPUSES) {
            Toast.makeText(this, "请选择有效的校区", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示加载状态，防止重复点击
        showLoading(true)

        // 模拟短暂延迟后执行更新，给用户更好的反馈感
        binding.root.postDelayed({
            try {
                // 如果用户选择了新头像，先保存头像路径
                if (avatarPath.isNotEmpty()) {
                    userManager.updateAvatar(avatarPath)
                }

                // 调用 UserManager 写入昵称、学号、校区到 SharedPreferences
                val success = userManager.updateUserInfo(nickname, studentId, campus)
                if (success) {
                    Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_SHORT).show()
                    // 更新成功后关闭当前页面，回到个人信息页后会重新加载数据
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.update_fail), Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.update_fail), Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }, 300)
    }

    /**
     * 控制加载状态：禁用/启用保存按钮并更新按钮文字
     * @param loading true 表示正在加载，false 表示加载完成
     */
    private fun showLoading(loading: Boolean) {
        binding.btnSave.isEnabled = !loading
        binding.btnSave.text = if (loading) getString(R.string.registering) else getString(R.string.save)
    }

    /**
     * 页面恢复时重新加载用户信息，确保显示最新数据
     * 但不会覆盖用户已选择的新头像
     */
    override fun onResume() {
        super.onResume()
        val userInfo = userManager.getUserInfo()
        binding.etUsername.setText(userInfo.username)
        binding.etNickname.setText(userInfo.nickname)
        binding.etStudentId.setText(userInfo.studentId)
        binding.actvCampus.setText(userInfo.campus)
        // 注意：不重置 avatarPath，保留用户刚刚选择的头像
        if (avatarPath.isEmpty()) {
            avatarPath = userInfo.avatarPath
            if (avatarPath.isNotEmpty() && File(avatarPath).exists()) {
                showAvatarPreview()
            }
        }
    }
}
