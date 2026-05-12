package com.campus.lostfound.model

/**
 * 失物招领物品数据类
 */
data class Item(
    var id: Long = 0,                   // 数据库ID，主键
    var type: String = "",              // 类型："lost" 丢失物品，"found" 招领物品
    var name: String = "",              // 物品名称
    var category: String = "",          // 物品分类
    var location: String = "",          // 地点名称
    var time: String = "",              // 丢失或发现时间
    var contact: String = "",           // 联系方式
    var description: String = "",       // 详细描述
    var imagePath: String = "",         // 图片文件路径
    var publisher: String = "",         // 发布者用户名
    var publishTime: String = "",       // 发布时间
    var latitude: Double = 0.0,         // 纬度
    var longitude: Double = 0.0,        // 经度
    var addressText: String = ""        // 详细地址文字
)
