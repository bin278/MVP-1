package com.campus.lostfound.firebase

import com.google.firebase.database.IgnoreExtraProperties

/**
 * 物品数据模型类
 * 用于存储失物招领信息
 *
 * @property id 数据库唯一标识符，由Firebase自动生成
 * @property type 物品类型："lost"表示丢失物品，"found"表示捡到物品
 * @property name 物品名称
 * @property description 物品详细描述
 * @property location 丢失或捡到物品的地点
 * @property contact 联系方式（手机号）
 * @property imageUrl 物品图片URL地址
 * @property publishTime 发布时间（时间戳格式）
 * @property publisherId 发布者用户ID
 * @property publisherName 发布者用户名
 */
@IgnoreExtraProperties
data class Item(
    var id: String? = null,
    var type: String? = null,
    var name: String? = null,
    var description: String? = null,
    var location: String? = null,
    var contact: String? = null,
    var imageUrl: String? = null,
    var publishTime: Long? = null,
    var publisherId: String? = null,
    var publisherName: String? = null
)
