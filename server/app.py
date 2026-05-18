from flask import Flask, request, jsonify
from flask_cors import CORS
import pymysql
import hashlib
from datetime import datetime
from config import DB_CONFIG, SERVER_CONFIG

app = Flask(__name__)
CORS(app)  # 允许跨域请求

def get_db_connection():
    """获取数据库连接"""
    config = DB_CONFIG.copy()
    cursor_class = config.pop('cursorclass')
    conn = pymysql.connect(**config, cursorclass=getattr(pymysql.cursors, cursor_class.split('.')[-1]))
    return conn

def md5_encrypt(text):
    """MD5加密"""
    return hashlib.md5(text.encode()).hexdigest()

# ============================================
# 用户相关API
# ============================================

@app.route('/api/register', methods=['POST'])
def register():
    """用户注册"""
    try:
        data = request.json
        username = data.get('username', '').strip()
        password = data.get('password', '').strip()
        nickname = data.get('nickname', '')
        student_id = data.get('studentId', '')
        campus = data.get('campus', '')
        avatar_path = data.get('avatarPath', '')

        if not username or not password:
            return jsonify({'success': False, 'message': '用户名和密码不能为空'})

        conn = get_db_connection()
        cursor = conn.cursor()

        # 检查用户名是否存在
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            conn.close()
            return jsonify({'success': False, 'message': '用户名已存在'})

        # 插入新用户
        password_md5 = md5_encrypt(password)
        sql = """INSERT INTO users (username, password, nickname, student_id, campus, avatar_path)
                 VALUES (%s, %s, %s, %s, %s, %s)"""
        cursor.execute(sql, (username, password_md5, nickname, student_id, campus, avatar_path))
        conn.commit()
        conn.close()

        return jsonify({'success': True, 'message': '注册成功'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/login', methods=['POST'])
def login():
    """用户登录"""
    try:
        data = request.json
        username = data.get('username', '').strip()
        password = data.get('password', '').strip()

        if not username or not password:
            return jsonify({'success': False, 'message': '用户名和密码不能为空'})

        conn = get_db_connection()
        cursor = conn.cursor()

        password_md5 = md5_encrypt(password)
        cursor.execute("SELECT * FROM users WHERE username = %s AND password = %s",
                      (username, password_md5))
        user = cursor.fetchone()
        conn.close()

        if user:
            # 返回用户信息（不包含密码）
            user_info = {
                'username': user['username'],
                'nickname': user['nickname'],
                'studentId': user['student_id'],
                'campus': user['campus'],
                'avatarPath': user['avatar_path']
            }
            return jsonify({'success': True, 'user': user_info})
        else:
            return jsonify({'success': False, 'message': '用户名或密码错误'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/user/info', methods=['GET'])
def get_user_info():
    """获取用户信息"""
    try:
        username = request.args.get('username', '')
        if not username:
            return jsonify({'success': False, 'message': '用户名不能为空'})

        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
        user = cursor.fetchone()
        conn.close()

        if user:
            user_info = {
                'username': user['username'],
                'nickname': user['nickname'],
                'studentId': user['student_id'],
                'campus': user['campus'],
                'avatarPath': user['avatar_path']
            }
            return jsonify({'success': True, 'user': user_info})
        else:
            return jsonify({'success': False, 'message': '用户不存在'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/user/update', methods=['POST'])
def update_user_info():
    """更新用户信息"""
    try:
        data = request.json
        username = data.get('username', '')
        nickname = data.get('nickname', '')
        student_id = data.get('studentId', '')
        campus = data.get('campus', '')

        conn = get_db_connection()
        cursor = conn.cursor()
        sql = """UPDATE users SET nickname = %s, student_id = %s, campus = %s
                 WHERE username = %s"""
        cursor.execute(sql, (nickname, student_id, campus, username))
        conn.commit()
        conn.close()

        return jsonify({'success': True, 'message': '更新成功'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/user/avatar', methods=['POST'])
def update_avatar():
    """更新用户头像"""
    try:
        data = request.json
        username = data.get('username', '')
        avatar_path = data.get('avatarPath', '')

        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("UPDATE users SET avatar_path = %s WHERE username = %s",
                      (avatar_path, username))
        conn.commit()
        conn.close()

        return jsonify({'success': True, 'message': '头像更新成功'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

# ============================================
# 物品相关API
# ============================================

@app.route('/api/items', methods=['GET'])
def get_items():
    """获取所有物品列表"""
    try:
        item_type = request.args.get('type', '')
        category = request.args.get('category', '')

        conn = get_db_connection()
        cursor = conn.cursor()

        sql = "SELECT * FROM items WHERE 1=1"
        params = []

        if item_type:
            sql += " AND type = %s"
            params.append(item_type)
        if category:
            sql += " AND category = %s"
            params.append(category)

        sql += " ORDER BY publish_time DESC"
        cursor.execute(sql, params)
        items = cursor.fetchall()
        conn.close()

        # 转换字段名（下划线转驼峰）
        result = []
        for item in items:
            result.append({
                'id': item['id'],
                'type': item['type'],
                'name': item['name'],
                'category': item['category'],
                'location': item['location'],
                'time': item['time'],
                'contact': item['contact'],
                'description': item['description'],
                'imagePath': item['image_path'],
                'publisher': item['publisher'],
                'publishTime': item['publish_time'],
                'latitude': item['latitude'],
                'longitude': item['longitude'],
                'addressText': item['address_text']
            })

        return jsonify({'success': True, 'items': result})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/items/<int:item_id>', methods=['GET'])
def get_item(item_id):
    """获取单个物品详情"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM items WHERE id = %s", (item_id,))
        item = cursor.fetchone()
        conn.close()

        if item:
            result = {
                'id': item['id'],
                'type': item['type'],
                'name': item['name'],
                'category': item['category'],
                'location': item['location'],
                'time': item['time'],
                'contact': item['contact'],
                'description': item['description'],
                'imagePath': item['image_path'],
                'publisher': item['publisher'],
                'publishTime': item['publish_time'],
                'latitude': item['latitude'],
                'longitude': item['longitude'],
                'addressText': item['address_text']
            }
            return jsonify({'success': True, 'item': result})
        else:
            return jsonify({'success': False, 'message': '物品不存在'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/items', methods=['POST'])
def add_item():
    """添加物品"""
    try:
        data = request.json
        conn = get_db_connection()
        cursor = conn.cursor()

        sql = """INSERT INTO items (type, name, category, location, time, contact,
                 description, image_path, publisher, publish_time, latitude, longitude, address_text)
                 VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"""

        cursor.execute(sql, (
            data.get('type', ''),
            data.get('name', ''),
            data.get('category', ''),
            data.get('location', ''),
            data.get('time', ''),
            data.get('contact', ''),
            data.get('description', ''),
            data.get('imagePath', ''),
            data.get('publisher', ''),
            data.get('publishTime', datetime.now().strftime('%Y-%m-%d %H:%M:%S')),
            data.get('latitude', 0.0),
            data.get('longitude', 0.0),
            data.get('addressText', '')
        ))
        conn.commit()
        item_id = cursor.lastrowid
        conn.close()

        return jsonify({'success': True, 'id': item_id, 'message': '发布成功'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/items/<int:item_id>', methods=['DELETE'])
def delete_item(item_id):
    """删除物品"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("DELETE FROM items WHERE id = %s", (item_id,))
        conn.commit()
        affected = cursor.rowcount
        conn.close()

        if affected > 0:
            return jsonify({'success': True, 'message': '删除成功'})
        else:
            return jsonify({'success': False, 'message': '物品不存在'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/items/user/<username>', methods=['GET'])
def get_user_items(username):
    """获取用户发布的物品"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM items WHERE publisher = %s ORDER BY publish_time DESC", (username,))
        items = cursor.fetchall()
        conn.close()

        result = []
        for item in items:
            result.append({
                'id': item['id'],
                'type': item['type'],
                'name': item['name'],
                'category': item['category'],
                'location': item['location'],
                'time': item['time'],
                'contact': item['contact'],
                'description': item['description'],
                'imagePath': item['image_path'],
                'publisher': item['publisher'],
                'publishTime': item['publish_time'],
                'latitude': item['latitude'],
                'longitude': item['longitude'],
                'addressText': item['address_text']
            })

        return jsonify({'success': True, 'items': result})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

# ============================================
# 收藏相关API
# ============================================

@app.route('/api/favorites', methods=['GET'])
def get_favorites():
    """获取用户收藏列表"""
    try:
        username = request.args.get('username', '')
        if not username:
            return jsonify({'success': False, 'message': '用户名不能为空'})

        conn = get_db_connection()
        cursor = conn.cursor()

        sql = """SELECT i.* FROM items i
                 INNER JOIN favorites f ON i.id = f.item_id
                 WHERE f.username = %s
                 ORDER BY f.create_time DESC"""
        cursor.execute(sql, (username,))
        items = cursor.fetchall()
        conn.close()

        result = []
        for item in items:
            result.append({
                'id': item['id'],
                'type': item['type'],
                'name': item['name'],
                'category': item['category'],
                'location': item['location'],
                'time': item['time'],
                'contact': item['contact'],
                'description': item['description'],
                'imagePath': item['image_path'],
                'publisher': item['publisher'],
                'publishTime': item['publish_time'],
                'latitude': item['latitude'],
                'longitude': item['longitude'],
                'addressText': item['address_text']
            })

        return jsonify({'success': True, 'items': result})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/favorites', methods=['POST'])
def add_favorite():
    """添加收藏"""
    try:
        data = request.json
        username = data.get('username', '')
        item_id = data.get('itemId', 0)

        conn = get_db_connection()
        cursor = conn.cursor()

        # 检查是否已收藏
        cursor.execute("SELECT id FROM favorites WHERE username = %s AND item_id = %s",
                      (username, item_id))
        if cursor.fetchone():
            conn.close()
            return jsonify({'success': False, 'message': '已收藏'})

        sql = "INSERT INTO favorites (username, item_id, create_time) VALUES (%s, %s, %s)"
        cursor.execute(sql, (username, item_id, datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
        conn.commit()
        conn.close()

        return jsonify({'success': True, 'message': '收藏成功'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/favorites', methods=['DELETE'])
def remove_favorite():
    """取消收藏"""
    try:
        username = request.args.get('username', '')
        item_id = request.args.get('itemId', 0)

        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("DELETE FROM favorites WHERE username = %s AND item_id = %s",
                      (username, item_id))
        conn.commit()
        conn.close()

        return jsonify({'success': True, 'message': '取消收藏成功'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

@app.route('/api/favorites/check', methods=['GET'])
def check_favorite():
    """检查是否已收藏"""
    try:
        username = request.args.get('username', '')
        item_id = request.args.get('itemId', 0)

        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM favorites WHERE username = %s AND item_id = %s",
                      (username, item_id))
        exists = cursor.fetchone() is not None
        conn.close()

        return jsonify({'success': True, 'isFavorite': exists})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)})

# ============================================
# 启动服务
# ============================================

if __name__ == '__main__':
    print("=" * 50)
    print("校园失物招领系统 - 后端API服务")
    print("=" * 50)
    print(f"服务地址: http://localhost:{SERVER_CONFIG['port']}")
    print(f"API文档: http://localhost:{SERVER_CONFIG['port']}/api")
    print("=" * 50)

    app.run(**SERVER_CONFIG)
