package com.campus.lostfound.view.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.sharedpref.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class RegisterActivity : BaseActivity() {

    private lateinit var userManager: UserManager
    private lateinit var ivAvatar: ImageView
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
        setContentView(R.layout.activity_register)

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etNickname = findViewById<TextInputEditText>(R.id.etNickname)
        val etStudentId = findViewById<TextInputEditText>(R.id.etStudentId)
        val actvCampus = findViewById<AutoCompleteTextView>(R.id.actvCampus)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)
        val cardAvatar = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAvatar)
        val tvChangeAvatar = findViewById<TextView>(R.id.tvChangeAvatar)
        ivAvatar = findViewById(R.id.ivAvatar)

        val campusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.CAMPUSES)
        actvCampus.setAdapter(campusAdapter)

        val avatarClickListener = {
            showAvatarPickerDialog()
        }
        cardAvatar.setOnClickListener { avatarClickListener() }
        tvChangeAvatar.setOnClickListener { avatarClickListener() }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val studentId = etStudentId.text.toString().trim()
            val campus = actvCampus.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || nickname.isEmpty() || studentId.isEmpty() || campus.isEmpty()) {
                Toast.makeText(this, getString(R.string.input_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userManager.register(username, password, nickname, studentId, campus, avatarPath)) {
                Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.register_fail), Toast.LENGTH_SHORT).show()
            }
        }

        tvGoLogin.setOnClickListener {
            finish()
        }
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
            .into(ivAvatar)
    }
}