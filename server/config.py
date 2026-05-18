# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'admin123',  # 修改为你的MySQL密码
    'database': 'campus_lost_found',
    'charset': 'utf8mb4',
    'cursorclass': 'pymysql.cursors.DictCursor'
}

# 服务器配置
SERVER_CONFIG = {
    'host': '0.0.0.0',  # 允许外部访问
    'port': 5000,
    'debug': True
}
