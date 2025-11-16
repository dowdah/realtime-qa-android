#!/bin/bash

# ============================================
# AskNow Backend Server
# 使用Pipenv管理Python依赖
# ============================================

echo "🚀 Starting AskNow Backend Server..."
echo ""

# 检查是否安装了pipenv
if ! command -v pipenv &> /dev/null; then
    echo "❌ Pipenv not found!"
    echo "📦 Please install Pipenv first:"
    echo "   pip install pipenv"
    echo "   or: brew install pipenv"
    exit 1
fi

# 检查是否已安装依赖
if [ ! -f "Pipfile.lock" ]; then
    echo "📥 Installing dependencies..."
    pipenv install
    echo ""
fi

# 检查数据库文件
if [ ! -f "asknow.db" ]; then
    echo "ℹ️  Database not found - will be created on first run"
    echo ""
fi

# 设置Python路径
export PYTHONPATH="${PYTHONPATH}:$(pwd)"

# 显示服务器信息
echo "✅ Environment ready!"
echo "🌐 Server will be available at: http://0.0.0.0:8000"
echo "📱 For Android Emulator use: http://10.0.2.2:8000"
echo "📝 API docs at: http://0.0.0.0:8000/docs"
echo "📊 WebSocket endpoint: ws://0.0.0.0:8000/ws/{user_id}"
echo ""
echo "⚠️  Press CTRL+C to stop the server"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 运行FastAPI服务器（开发模式，带自动重载）
pipenv run uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 生产模式（不带自动重载，使用多个worker）
# pipenv run uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
