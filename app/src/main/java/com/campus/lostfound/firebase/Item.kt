package com.campus.lostfound.firebase

import com.google.firebase.database.IgnoreExtraProperties

/**
 * 物品数据模型类
 * 用于存储失物招领信息
 *
 * @property id 数据库唯一标识符，由Firebase自动生成
 * @property type 物品类型："lost"表示丢失物品，"found"表示捡到物品
 * @property name 物品名称
 * @property category 物品分类
 * @property description 物品详细描述
 * @property location 丢失或捡到物品的地点
 * @property campus 校区
 * @property time 丢失或捡到时间
 * @property phone 联系方式（手机号）
 * @property images 图片路径列表
 * @property publishTime 发布时间（时间戳格式）
 * @property publisherId 发布者用户ID
 * @property publisherName 发布者用户名
 * @property latitude 纬度坐标
 * @property longitude 经度坐标
 * @property addressText 地址文本
 */
@IgnoreExtraProperties
data class Item(
    var id: String? = null,
    var type: String? = null,
    var name: String? = null,
    var category: String? = null,
    var description: String? = null,
    var location: String? = null,
    var campus: String? = null,
    var time: String? = null,
    var phone: String? = null,
    var images: List<String>? = null,
    var publishTime: Long? = null,
    var publisherId: String? = null,
    var publisherName: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var addressText: String? = null
)
