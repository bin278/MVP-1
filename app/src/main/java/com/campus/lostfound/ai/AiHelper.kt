package com.campus.lostfound.ai

import com.campus.lostfound.constant.Constants

class AiHelper {

    private val keywordMap = mapOf(
        "手机" to "电子产品", "电脑" to "电子产品", "平板" to "电子产品", "耳机" to "电子产品",
        "充电宝" to "电子产品", "充电器" to "电子产品", "数据线" to "电子产品", "U盘" to "电子产品",
        "键盘" to "电子产品", "鼠标" to "电子产品", "手表" to "电子产品", "相机" to "电子产品",
        "身份证" to "证件卡片", "学生证" to "证件卡片", "校园卡" to "证件卡片", "银行卡" to "证件卡片",
        "饭卡" to "证件卡片", "驾驶证" to "证件卡片", "护照" to "证件卡片",
        "钥匙" to "钥匙钱包", "钱包" to "钥匙钱包", "卡包" to "钥匙钱包", "背包" to "钥匙钱包",
        "书包" to "钥匙钱包", "手提包" to "钥匙钱包",
        "教材" to "书籍文具", "课本" to "书籍文具", "笔记本" to "书籍文具", "笔" to "书籍文具",
        "书" to "书籍文具", "文具" to "书籍文具", "橡皮" to "书籍文具", "尺子" to "书籍文具",
        "外套" to "衣物配饰", "衣服" to "衣物配饰", "围巾" to "衣物配饰", "帽子" to "衣物配饰",
        "手套" to "衣物配饰", "眼镜" to "衣物配饰", "雨伞" to "衣物配饰",
        "水杯" to "生活用品", "保温杯" to "生活用品", "饭盒" to "生活用品", "毛巾" to "生活用品",
        "耳机壳" to "生活用品", "手机壳" to "生活用品",
        "篮球" to "运动器材", "足球" to "运动器材", "羽毛球" to "运动器材", "乒乓球" to "运动器材",
        "跳绳" to "运动器材", "瑜伽垫" to "运动器材"
    )

    fun classify(name: String): String {
        for ((keyword, category) in keywordMap) {
            if (name.contains(keyword)) {
                return category
            }
        }
        return Constants.CATEGORIES.last()
    }
}
