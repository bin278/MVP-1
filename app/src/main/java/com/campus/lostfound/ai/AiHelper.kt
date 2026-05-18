package com.campus.lostfound.ai

import com.campus.lostfound.constant.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI 助手类
 * 支持：
 * 1. 本地关键词分类
 * 2. 直接调用 DeepSeek API 进行智能匹配
 */
class AiHelper {

    // DeepSeek API 配置
    private val DEEPSEEK_API_KEY = "sk-d326b30005544086b02f2596b002cc85"
    private val DEEPSEEK_URL = "https://api.deepseek.com/v1/chat/completions"

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

    /**
     * 本地关键词分类
     * @param name 物品名称
     * @return 分类名称
     */
    fun classify(name: String): String {
        for ((keyword, category) in keywordMap) {
            if (name.contains(keyword)) {
                return category
            }
        }
        return Constants.CATEGORIES.last()
    }

    /**
     * 直接调用 DeepSeek API 进行智能匹配
     * @param userQuery 用户查询描述
     * @param itemsText 物品列表文本
     * @param callback 回调函数，返回匹配结果和错误信息
     */
    fun callDeepSeekApi(userQuery: String, itemsText: String, callback: (String?, String?) -> Unit) {
        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                val systemPrompt = """你是一个校园失物招领智能匹配助手。你的任务是分析用户的自然语言描述，从提供的物品信息列表中找出最可能匹配的失物或招领条目。

匹配规则（按重要性排序）：
1. 物品名称与类别（权重 40%）：理解同义词和泛称，如"双肩包"与"背包"视为同类。
2. 外观特征（权重 25%）：颜色、品牌、材质、图案、特殊标记。
3. 地点匹配（权重 20%）：拾取/丢失地点与用户描述地点一致或相近。
4. 时间匹配（权重 10%）：时间接近程度（当天、昨天、本周等）。
5. 内含物或附加描述（权重 5%）：如内含书籍、证件、水杯等。

匹配度分级：
- 90-100 分：高度匹配，特征几乎完全一致。
- 70-89 分：较强匹配，多数核心特征吻合。
- 50-69 分：可能匹配，部分特征相似。
- 低于50分不推荐。

输出格式：必须返回一个严格的 JSON 对象，不能包含其他文字或 markdown 标记。
{
  "matches": [
    {
      "id": 物品ID（整数）,
      "score": 匹配度分数（整数 0-100）,
      "reason": "简短的匹配理由",
      "suggestion": "给用户的建议"
    }
  ],
  "summary": "一段简洁的总结语"
}

如果没有任何匹配项，matches 为空数组 []，summary 给出鼓励性建议。"""

                val userMessage = """用户描述：$userQuery

当前可匹配的物品信息：
$itemsText

请严格按照 JSON 格式输出匹配结果，只返回 JSON 对象，不要包含任何解释性文字。"""

                val requestBody = JSONObject().apply {
                    put("model", "deepseek-chat")
                    put("messages", listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf("role" to "user", "content" to userMessage)
                    ))
                    put("temperature", 0.1)
                    put("response_format", mapOf("type" to "json_object"))
                    put("max_tokens", 1500)
                }.toString()

                val request = Request.Builder()
                    .url(DEEPSEEK_URL)
                    .addHeader("Authorization", "Bearer $DEEPSEEK_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "HTTP error ${response.code}"
                        callback(null, "API 请求失败: $errorBody")
                        return@Thread
                    }

                    val responseBody = response.body?.string() ?: ""
                    try {
                        val json = JSONObject(responseBody)
                        val content = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        callback(content, null)
                    } catch (e: Exception) {
                        callback(null, "解析响应失败: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                callback(null, "网络错误: ${e.message}")
            }
        }.start()
    }
}