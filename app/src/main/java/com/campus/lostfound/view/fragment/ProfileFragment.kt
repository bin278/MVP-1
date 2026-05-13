package com.campus.lostfound.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.campus.lostfound.R
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.activity.LoginActivity
import com.campus.lostfound.view.activity.MyPublishActivity
import com.campus.lostfound.view.activity.MyFavoriteActivity
import com.google.android.material.button.MaterialButton

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

        val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
        val tvStudentInfo = view.findViewById<TextView>(R.id.tvStudentInfo)
        val tvMyPublish = view.findViewById<View>(R.id.tvMyPublish)
        val tvMyFavorite = view.findViewById<View>(R.id.tvMyFavorite)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)

        val userInfo = userManager.getUserInfo()
        tvUsername.text = userInfo.nickname.ifEmpty { userInfo.username }

        val infoParts = mutableListOf<String>()
        if (userInfo.studentId.isNotEmpty()) {
            infoParts.add("学号: ${userInfo.studentId}")
        }
        if (userInfo.campus.isNotEmpty()) {
            infoParts.add("校区: ${userInfo.campus}")
        }
        tvStudentInfo.text = infoParts.joinToString(" | ")

        tvMyPublish.setOnClickListener {
            startActivity(Intent(requireContext(), MyPublishActivity::class.java))
        }

        tvMyFavorite.setOnClickListener {
            startActivity(Intent(requireContext(), MyFavoriteActivity::class.java))
        }

        btnLogout.setOnClickListener {
            userManager.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}