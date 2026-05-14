"""
校园失物招领 - AI 智能匹配后端服务
基于 Flask + DeepSeek API，提供自然语言失物匹配功能
启动方式：python ai_server.py
默认监听 0.0.0.0:5000
"""

from flask import Flask, request, jsonify
import requests
import json

app = Flask(__name__)

# ============ 配置区域 ============
# DeepSeek API Key（请替换为你自己的 Key）
# 获取地址：https://platform.deepseek.com/api_keys
DEEPSEEK_API_KEY = "sk-d326b30005544086b02f2596b002cc85"
DEEPSEEK_URL = "https://api.deepseek.com/v1/chat/completions"

# 数据库路径（SQLite）
# Android 端使用本地 SQLite，后端可复用或使用独立数据库
DB_PATH = "lost_found.db"  # 可选：连接到项目数据库
# ================================


def get_system_prompt():
    """
    构建系统提示词，定义 DeepSeek 的角色和匹配规则
    这是整个 AI 匹配的核心，决定了匹配质量
    """
    return """你是一个校园失物招领智能匹配助手。你的任务是分析用户的自然语言描述，从提供的物品信息列表中找出最可能匹配的失物或招领条目。

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


def build_request_payload(user_query, items_text):
    """
    构建发送给 DeepSeek API 的请求体
    使用 JSON Mode 强制模型返回结构化数据
    """
    return {
        "model": "deepseek-chat",
        "messages": [
            {"role": "system", "content": get_system_prompt()},
            {"role": "user", "content": f"""用户描述：{user_query}

当前可匹配的物品信息：
{items_text}

请严格按照 JSON 格式输出匹配结果，只返回 JSON 对象，不要包含任何解释性文字。"""}
        ],
        "temperature": 0.1,  # 低温度保证输出稳定
        "response_format": {"type": "json_object"},  # 强制 JSON 输出
        "max_tokens": 1500
    }


@app.route('/api/chat/match', methods=['POST'])
def intelligent_match():
    """
    AI 智能匹配接口
    接收用户自然语言查询，返回语义匹配结果
    """
    data = request.get_json()
    if not data:
        return jsonify({"error": "请求体不能为空"}), 400

    user_query = data.get('query', '').strip()
    if not user_query:
        return jsonify({"error": "描述不能为空"}), 400

    # 检查 API Key 是否已配置
    if DEEPSEEK_API_KEY == "sk-your-deepseek-api-key-here":
        return jsonify({
            "error": "API Key 未配置",
            "matches": [],
            "summary": "请在 ai_server.py 中配置 DeepSeek API Key 后重试。"
        }), 500

    # 从数据库查询物品信息（此处使用模拟数据，实际部署时接入真实数据库）
    items = get_items_from_db(user_query)

    if not items:
        return jsonify({
            "matches": [],
            "summary": "当前数据库中没有相关物品信息，建议先浏览公告板或稍后再试。"
        })

    # 将物品序列化为模型可读的文本格式
    items_text = serialize_items(items)

    headers = {
        "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
        "Content-Type": "application/json"
    }

    try:
        resp = requests.post(
            DEEPSEEK_URL,
            headers=headers,
            json=build_request_payload(user_query, items_text),
            timeout=15
        )
        resp.raise_for_status()
        result = resp.json()

        # 提取模型返回的 JSON 内容
        content = result["choices"][0]["message"]["content"]
        match_data = json.loads(content)

        # 确保返回格式正确
        return jsonify({
            "matches": match_data.get("matches", []),
            "summary": match_data.get("summary", "匹配完成")
        })

    except requests.exceptions.Timeout:
        return jsonify({
            "error": "AI 服务响应超时",
            "matches": [],
            "summary": "AI 服务响应超时，请稍后重试。"
        }), 504

    except requests.exceptions.HTTPError as e:
        status_code = e.response.status_code if e.response else 500
        if status_code == 401:
            return jsonify({"error": "API Key 无效", "matches": [], "summary": "服务配置错误。"}), 500
        elif status_code == 429:
            return jsonify({"error": "请求频率超限", "matches": [], "summary": "请求太频繁，请稍后再试。"}), 429
        return jsonify({"error": str(e), "matches": [], "summary": "AI 服务异常。"}), 500

    except Exception as e:
        return jsonify({
            "error": str(e),
            "matches": [],
            "summary": "匹配服务异常，请稍后重试。"
        }), 500


def serialize_items(items):
    """
    将物品列表序列化为模型可阅读的文本格式
    每条物品信息以一行展示，包含所有关键字段
    """
    lines = []
    for item in items:
        lines.append(
            f"ID:{item['id']} | "
            f"类型:{'招领' if item.get('type') == 'found' else '失物'} | "
            f"物品名称:{item.get('name', '')} | "
            f"类别:{item.get('category', '')} | "
            f"地点:{item.get('address_text') or item.get('location', '')} | "
            f"时间:{item.get('time', '')} | "
            f"描述:{item.get('description', '无')} | "
            f"联系方式:{item.get('contact', '')}"
        )
    return "\n".join(lines)


def get_items_from_db(query):
    """
    从数据库获取物品信息
    当前为模拟数据，实际部署时替换为 SQLite/MySQL 查询
    """
    # ====== 模拟数据（演示用） ======
    # 实际部署时取消下面注释，接入真实数据库：
    # import sqlite3
    # conn = sqlite3.connect(DB_PATH)
    # cursor = conn.cursor()
    # intent = "found" if any(w in query for w in ["丢了","丢失","找"]) else "lost"
    # cursor.execute("SELECT * FROM items WHERE type = ?", (intent,))
    # rows = cursor.fetchall()
    # conn.close()
    # return [dict(zip([col[0] for col in cursor.description], row)) for row in rows]

    return [
        {
            "id": 1, "type": "found", "name": "白色苹果手机",
            "category": "电子产品", "location": "图书馆二楼",
            "time": "2026-05-13 16:30",
            "contact": "QQ 11111",
            "description": "带有卡通手机壳",
            "address_text": "图书馆二楼自习区"
        },
        {
            "id": 2, "type": "found", "name": "黑色华为手机",
            "category": "电子产品", "location": "食堂一楼",
            "time": "2026-05-13 12:00",
            "contact": "微信 user1",
            "description": "无",
            "address_text": "第一食堂"
        },
        {
            "id": 3, "type": "found", "name": "黑色双肩包",
            "category": "钥匙钱包", "location": "操场看台",
            "time": "2026-05-12 10:00",
            "contact": "电话 13800138000",
            "description": "内有《数据结构》教材和蓝色文具盒",
            "address_text": "操场看台"
        },
        {
            "id": 4, "type": "found", "name": "学生证",
            "category": "证件卡片", "location": "教学楼A座",
            "time": "2026-05-13 08:00",
            "contact": "13900139000",
            "description": "计算机学院 李明",
            "address_text": "教学楼A座301"
        },
        {
            "id": 5, "type": "found", "name": "蓝牙耳机",
            "category": "电子产品", "location": "体育馆",
            "time": "2026-05-11 15:00",
            "contact": "13800138001",
            "description": "白色AirPods Pro，蓝色保护壳",
            "address_text": "体育馆更衣室"
        },
        {
            "id": 6, "type": "found", "name": "校园卡",
            "category": "证件卡片", "location": "自习室",
            "time": "2026-05-13 09:00",
            "contact": "13800138002",
            "description": "卡号后四位8832",
            "address_text": "第一自习室"
        },
        {
            "id": 7, "type": "found", "name": "运动水杯",
            "category": "生活用品", "location": "体育馆",
            "time": "2026-05-12 14:00",
            "contact": "13900139001",
            "description": "绿色李宁运动水杯",
            "address_text": "体育馆"
        },
        {
            "id": 8, "type": "lost", "name": "高等数学课本",
            "category": "书籍文具", "location": "图书馆",
            "time": "2026-05-10 16:00",
            "contact": "13900139002",
            "description": "同济版高数上册，书内有笔记",
            "address_text": "图书馆三楼"
        },
    ]


if __name__ == '__main__':
    print("=" * 50)
    print("  校园失物招领 AI 智能匹配服务已启动")
    print("  监听地址: http://0.0.0.0:5000")
    print("  接口: POST /api/chat/match")
    print("=" * 50)
    print()
    print("  提示：")
    print("  1. 请先将 DEEPSEEK_API_KEY 替换为你的真实 Key")
    print("  2. 模拟器通过 10.0.2.2:5000 访问本服务")
    print("  3. 真机通过电脑局域网 IP 访问，需修改 RetrofitClient.kt 中的 BASE_URL")
    print("=" * 50)
    app.run(host='0.0.0.0', port=5000, debug=True)
