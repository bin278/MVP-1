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

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userManager: UserManager
    private var avatarPath: String = ""

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

    private var cameraAvatarUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userManager = UserManager(this)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val campusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.CAMPUSES)
        binding.actvCampus.setAdapter(campusAdapter)

        val avatarClickListener = { showAvatarPickerDialog() }
        binding.cardAvatar.setOnClickListener { avatarClickListener() }
        binding.tvChangeAvatar.setOnClickListener { avatarClickListener() }

        binding.btnRegister.setOnClickListener { performRegister() }

        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun performRegister() {
        val username = binding.etUsername.text.toString().trim()
        val nickname = binding.etNickname.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val campus = binding.actvCampus.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || nickname.isEmpty() || studentId.isEmpty() || campus.isEmpty()) {
            showError(getString(R.string.input_required))
            return
        }

        if (password.length < 6) {
            showError(getString(R.string.password_too_short))
            return
        }

        if (password != confirmPassword) {
            showError(getString(R.string.password_mismatch))
            return
        }

        showLoading(true)

        binding.root.postDelayed({
            try {
                if (userManager.register(username, password, nickname, studentId, campus, avatarPath)) {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    showError(getString(R.string.register_fail))
                    showLoading(false)
                }
            } catch (e: Exception) {
                showError(getString(R.string.register_fail))
                showLoading(false)
            }
        }, 300)
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAvatarPickerDialog() {
        val options = arrayOf(getString(R.string.select_from_camera), getString(R.string.select_from_gallery))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_avatar))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCameraForAvatar()
                    1 -> openGalleryForAvatar()
                }
            }
            .show()
    }

    private fun openCameraForAvatar() {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), Constants.IMAGE_DIR)
            if (!dir.exists()) dir.mkdirs()
            val imageFile = File(dir, "AVATAR_${System.currentTimeMillis()}.jpg")
            cameraAvatarUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraAvatarUri)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGalleryForAvatar() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

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

    private fun showAvatarPreview() {
        Glide.with(this)
            .load(File(avatarPath))
            .circleCrop()
            .placeholder(android.R.drawable.ic_menu_camera)
            .into(binding.ivAvatar)
    }
}
