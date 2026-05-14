package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.sharedpref.UserManager
import java.io.File

/**
 * 个人信息展示页面（从底部导航"我的"进入）
 * 顶部：黄绿渐变 + 头像 + 用户名/学号/校区
 * 中间：功能列表
 * 底部：红色退出登录
 */
class ProfileActivity : BaseActivity() {

    private lateinit var userManager: UserManager
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvStudentId: TextView
    private lateinit var tvCampusLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userManager = UserManager(this)

        ivAvatar = findViewById(R.id.ivProfileAvatar)
        tvUsername = findViewById(R.id.tvUsername)
        tvStudentId = findViewById(R.id.tvStudentId)
        tvCampusLabel = findViewById(R.id.tvCampusLabel)

        val tvMyPublish = findViewById<View>(R.id.tvMyPublish)
        val tvMyClaim = findViewById<View>(R.id.tvMyClaim)
        val tvContactService = findViewById<View>(R.id.tvContactService)
        val tvAbout = findViewById<View>(R.id.tvAbout)
        val tvEditProfile = findViewById<View>(R.id.tvEditProfile)
        val btnLogout = findViewById<View>(R.id.btnLogout)

        loadUserInfo()

        tvMyPublish.setOnClickListener {
            startActivity(Intent(this, MyPublishActivity::class.java))
        }

        tvMyClaim.setOnClickListener {
            startActivity(Intent(this, MyFavoriteActivity::class.java))
        }

        tvContactService.setOnClickListener {
            startActivity(Intent(this, ContactServiceActivity::class.java))
        }

        tvAbout.setOnClickListener {
            Toast.makeText(this, "校园失物招领 v1.0\n广西科技大学", Toast.LENGTH_LONG).show()
        }

        tvEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            userManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadUserInfo() {
        val userInfo = userManager.getUserInfo()
        tvUsername.text = userInfo.nickname.ifEmpty { userInfo.username }
        tvStudentId.text = if (userInfo.studentId.isNotEmpty()) "学号: ${userInfo.studentId}" else "学号: 未设置"
        tvCampusLabel.text = if (userInfo.campus.isNotEmpty()) "校区: ${userInfo.campus}" else "校区: 未设置"

        if (userInfo.avatarPath.isNotEmpty()) {
            val avatarFile = File(userInfo.avatarPath)
            if (avatarFile.exists()) {
                Glide.with(this)
                    .load(avatarFile)
                    .circleCrop()
                    .placeholder(R.drawable.bg_avatar_default)
                    .into(ivAvatar)
                return
            }
        }
        ivAvatar.setImageResource(R.drawable.bg_avatar_default)
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }
}
