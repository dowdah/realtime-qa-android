#!/bin/bash
# API 自动化测试运行脚本

echo "=========================================="
echo "  实时问答系统 API 自动化测试"
echo "=========================================="
echo ""

# 检查 Python
if ! command -v python3 &> /dev/null; then
    echo "❌ 错误: 未找到 python3"
    exit 1
fi

# 检查测试依赖
echo "📦 检查测试依赖..."
if ! python3 -c "import requests" 2>/dev/null; then
    echo "⚠️  缺少依赖，正在安装..."
    pip3 install -r test_requirements.txt
fi

if ! python3 -c "import websockets" 2>/dev/null; then
    echo "⚠️  缺少依赖，正在安装..."
    pip3 install -r test_requirements.txt
fi

# 检查服务器是否运行
echo "🔍 检查后端服务器..."
if ! curl -s http://localhost:8000 > /dev/null 2>&1; then
    echo "❌ 错误: 后端服务器未运行"
    echo "请先启动服务器: python main.py"
    exit 1
fi

echo "✅ 后端服务器正在运行"
echo ""

# 运行测试
echo "🚀 开始运行测试..."
echo ""
python3 test_api.py

# 保存退出码
EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ 测试完成，所有测试通过！"
else
    echo "⚠️  测试完成，但部分测试失败"
fi

exit $EXIT_CODE

