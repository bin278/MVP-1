package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.sharedpref.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : BaseActivity() {

    private lateinit var userManager: UserManager

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

        val campusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.CAMPUSES)
        actvCampus.setAdapter(campusAdapter)

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

            if (userManager.register(username, password, nickname, studentId, campus)) {
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
}