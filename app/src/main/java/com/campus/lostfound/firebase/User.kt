package com.campus.lostfound.firebase

import com.google.firebase.database.IgnoreExtraProperties

/**
 * 用户数据模型类
 * 用于存储用户账户信息
 *
 * @property id 用户唯一标识符，由Firebase自动生成
 * @property username 用户名（登录用）
 * @property password 密码（实际项目中应加密存储）
 * @property nickname 用户昵称
 * @property phone 手机号码
 * @property email 邮箱地址
 * @property avatarUrl 头像图片URL
 * @property createTime 注册时间（时间戳格式）
 */
@IgnoreExtraProperties
data class User(
    var id: String? = null,
    var username: String? = null,
    var password: String? = null,
    var nickname: String? = null,
    var phone: String? = null,
    var email: String? = null,
    var avatarUrl: String? = null,
    var createTime: Long? = null
)
