package com.campus.lostfound.model

data class Item(
    var id: Long = 0,
    var type: String = "",
    var name: String = "",
    var category: String = "",
    var location: String = "",
    var time: String = "",
    var contact: String = "",
    var description: String = "",
    var imagePath: String = "",
    var publisher: String = "",
    var publishTime: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var addressText: String = ""
)
