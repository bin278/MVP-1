package com.campus.lostfound.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.activity.EditProfileActivity
import com.campus.lostfound.view.activity.LoginActivity
import com.campus.lostfound.view.activity.MyPublishActivity
import com.campus.lostfound.view.activity.MyFavoriteActivity
import com.campus.lostfound.view.activity.ContactServiceActivity
import java.io.File

/**
 * "我的"页面 Fragment
 * 顶部：黄绿渐变背景 + 圆形头像 + 用户名 / 学号 / 校区
 * 中间：功能列表（我的发布、我的认领、联系客服、关于平台、编辑资料）
 * 底部：红色"退出登录"文字
 */
class ProfileFragment : Fragment() {

    private lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userManager = UserManager(requireContext())
        val ctx = requireContext()

        val ivAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)
        val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
        val tvStudentId = view.findViewById<TextView>(R.id.tvStudentId)
        val tvCampusLabel = view.findViewById<TextView>(R.id.tvCampusLabel)

        val tvMyPublish = view.findViewById<View>(R.id.tvMyPublish)
        val tvMyClaim = view.findViewById<View>(R.id.tvMyClaim)
        val tvContactService = view.findViewById<View>(R.id.tvContactService)
        val tvAbout = view.findViewById<View>(R.id.tvAbout)
        val tvEditProfile = view.findViewById<View>(R.id.tvEditProfile)
        val btnLogout = view.findViewById<View>(R.id.btnLogout)

        loadUserInfo(ivAvatar, tvUsername, tvStudentId, tvCampusLabel)

        tvMyPublish.setOnClickListener {
            startActivity(Intent(ctx, MyPublishActivity::class.java))
        }

        tvMyClaim.setOnClickListener {
            startActivity(Intent(ctx, MyFavoriteActivity::class.java))
        }

        tvContactService.setOnClickListener {
            startActivity(Intent(ctx, ContactServiceActivity::class.java))
        }

        tvAbout.setOnClickListener {
            Toast.makeText(ctx, "校园失物招领 v1.0\n广西科技大学", Toast.LENGTH_LONG).show()
        }

        tvEditProfile.setOnClickListener {
            startActivity(Intent(ctx, EditProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            userManager.logout()
            startActivity(Intent(ctx, LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun loadUserInfo(
        ivAvatar: ImageView,
        tvUsername: TextView,
        tvStudentId: TextView,
        tvCampusLabel: TextView
    ) {
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
        view?.let { v ->
            loadUserInfo(
                v.findViewById(R.id.ivProfileAvatar),
                v.findViewById(R.id.tvUsername),
                v.findViewById(R.id.tvStudentId),
                v.findViewById(R.id.tvCampusLabel)
            )
        }
    }
}
