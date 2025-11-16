#!/usr/bin/env python3
"""
数据库重置脚本
用于删除并重建 AskNow 应用的数据库

使用方法:
    python reset_db.py
"""

import os
import shutil
from pathlib import Path


def reset_database():
    """重置数据库和相关文件"""
    
    # 获取 backend 目录路径
    backend_dir = Path(__file__).parent
    
    print("=" * 60)
    print("AskNow 数据库重置工具")
    print("=" * 60)
    print()
    
    # 删除数据库文件
    db_file = backend_dir / "asknow.db"
    if db_file.exists():
        db_file.unlink()
        print("✓ 已删除数据库文件: asknow.db")
    else:
        print("- 数据库文件不存在（已经是干净状态）")
    
    # 删除 uploads 目录
    uploads_dir = backend_dir / "uploads"
    if uploads_dir.exists():
        # 统计文件数量
        file_count = sum(1 for _ in uploads_dir.rglob('*') if _.is_file())
        shutil.rmtree(uploads_dir)
        print(f"✓ 已删除 uploads 目录（包含 {file_count} 个文件）")
    else:
        print("- uploads 目录不存在")
    
    # 重新创建 uploads 目录
    uploads_dir.mkdir(exist_ok=True)
    print("✓ 已重新创建空的 uploads 目录")
    
    # 删除日志文件（可选）
    log_file = backend_dir / "server.log"
    if log_file.exists():
        response = input("\n是否删除服务器日志文件 server.log? (y/N): ")
        if response.lower() in ['y', 'yes']:
            log_file.unlink()
            print("✓ 已删除日志文件: server.log")
        else:
            print("- 保留日志文件")
    
    print()
    print("=" * 60)
    print("✓ 数据库重置完成！")
    print()
    print("下次启动服务器时将自动创建新的数据库表。")
    print("启动命令: ./run.sh 或 python main.py")
    print("=" * 60)


if __name__ == "__main__":
    try:
        # 确认操作
        print("\n⚠️  警告：此操作将删除所有数据库数据和上传的文件！")
        response = input("确定要继续吗? (yes/N): ")
        
        if response.lower() == 'yes':
            reset_database()
        else:
            print("\n已取消操作。")
    except KeyboardInterrupt:
        print("\n\n已取消操作。")
    except Exception as e:
        print(f"\n❌ 错误: {e}")

