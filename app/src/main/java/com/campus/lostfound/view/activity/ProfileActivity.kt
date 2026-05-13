package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.campus.lostfound.R
import com.campus.lostfound.sharedpref.UserManager
import com.google.android.material.button.MaterialButton

class ProfileActivity : BaseActivity() {

    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userManager = UserManager(this)

        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val tvStudentInfo = findViewById<TextView>(R.id.tvStudentInfo)
        val tvMyPublish = findViewById<View>(R.id.tvMyPublish)
        val tvMyFavorite = findViewById<View>(R.id.tvMyFavorite)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        val userInfo = userManager.getUserInfo()
        tvUsername.text = userInfo.nickname.ifEmpty { userInfo.username }

        val infoParts = mutableListOf<String>()
        if (userInfo.studentId.isNotEmpty()) infoParts.add("学号: ${userInfo.studentId}")
        if (userInfo.campus.isNotEmpty()) infoParts.add("校区: ${userInfo.campus}")
        tvStudentInfo.text = infoParts.joinToString(" | ")

        tvMyPublish.setOnClickListener {
            startActivity(Intent(this, MyPublishActivity::class.java))
        }
        tvMyFavorite.setOnClickListener {
            startActivity(Intent(this, MyFavoriteActivity::class.java))
        }
        btnLogout.setOnClickListener {
            userManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}