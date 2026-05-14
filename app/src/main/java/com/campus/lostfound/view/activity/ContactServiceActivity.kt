package com.campus.lostfound.view.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.campus.lostfound.R

/**
 * 联系客服页面
 * 展示客服电话、邮箱、地址，支持一键拨号、发邮件
 * 同时包含常见问题 Q&A
 */
class ContactServiceActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_service)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<View>(R.id.btnCallPhone).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:07722688888")
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSendEmail).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:lostfound@gxust.edu.cn")
                putExtra(Intent.EXTRA_SUBJECT, "校园失物招领 - 用户反馈")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // 没有邮件客户端时复制邮箱
                copyToClipboard("lostfound@gxust.edu.cn", "邮箱已复制到剪贴板")
            }
        }

        findViewById<View>(R.id.btnAddress).setOnClickListener {
            copyToClipboard("广西科技大学文昌校区行政楼101室", "地址已复制到剪贴板")
        }
    }

    private fun copyToClipboard(text: String, toastMsg: String) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("label", text))
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }
}
