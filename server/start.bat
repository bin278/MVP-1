@echo off
chcp 65001 >nul
echo ============================================
echo 校园失物招领系统 - 后端服务启动脚本
echo ============================================
echo.

echo [1/3] 检查Python环境...
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Python，请先安装Python 3.8+
    echo 下载地址: https://www.python.org/downloads/
    pause
    exit /b 1
)
echo [OK] Python环境正常

echo.
echo [2/3] 安装依赖包...
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
if errorlevel 1 (
    echo [警告] 依赖安装可能有问题，尝试继续启动...
) else (
    echo [OK] 依赖安装完成
)

echo.
echo [3/3] 启动后端服务...
echo ============================================
echo 服务启动后，请保持此窗口打开
echo 按 Ctrl+C 可停止服务
echo ============================================
echo.

python app.py

pause
