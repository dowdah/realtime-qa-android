"""
后端配置文件
统一管理所有常量和配置项
"""
import os
from pathlib import Path

# ============================================================================
# 应用配置
# ============================================================================
APP_NAME = "AskNow API"
APP_VERSION = "1.0.0"
DEBUG = os.getenv("DEBUG", "False").lower() == "true"

# ============================================================================
# 服务器配置
# ============================================================================
HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", "8000"))
RELOAD = os.getenv("RELOAD", "True").lower() == "true"

# ============================================================================
# 数据库配置
# ============================================================================
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite+aiosqlite:///./asknow.db")
DATABASE_ECHO = os.getenv("DATABASE_ECHO", "False").lower() == "true"

# ============================================================================
# 认证配置
# ============================================================================
# JWT密钥（生产环境必须修改！）
SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-in-production")
ALGORITHM = "HS256"
# Token过期时间（秒）
ACCESS_TOKEN_EXPIRE_SECONDS = int(os.getenv("ACCESS_TOKEN_EXPIRE_SECONDS", str(86400 * 7)))  # 7天

# ============================================================================
# CORS配置
# ============================================================================
# 允许的来源（生产环境建议限制）
CORS_ORIGINS = os.getenv("CORS_ORIGINS", "*").split(",")
CORS_ALLOW_CREDENTIALS = True
CORS_ALLOW_METHODS = ["*"]
CORS_ALLOW_HEADERS = ["*"]

# ============================================================================
# 文件上传配置
# ============================================================================
# 上传目录
UPLOAD_DIR = os.getenv("UPLOAD_DIR", "uploads")
# 最大文件大小（字节）
MAX_FILE_SIZE = int(os.getenv("MAX_FILE_SIZE", str(10 * 1024 * 1024)))  # 10MB
# 允许的图片MIME类型
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/jpg", "image/png", "image/webp"}
# 允许的文件扩展名
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}

# ============================================================================
# 日志配置
# ============================================================================
# 日志级别
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
# 日志文件路径
LOG_FILE = os.getenv("LOG_FILE", "app.log")
# 日志格式
LOG_FORMAT = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
# 日志处理器
LOG_TO_FILE = os.getenv("LOG_TO_FILE", "True").lower() == "true"
LOG_TO_CONSOLE = os.getenv("LOG_TO_CONSOLE", "True").lower() == "true"

# ============================================================================
# 分页配置
# ============================================================================
# 问题列表默认分页大小
DEFAULT_QUESTIONS_PAGE_SIZE = 20
MAX_QUESTIONS_PAGE_SIZE = 100

# 消息列表默认分页大小
DEFAULT_MESSAGES_PAGE_SIZE = 50
MAX_MESSAGES_PAGE_SIZE = 200

# ============================================================================
# 用户角色
# ============================================================================
ROLE_STUDENT = "student"
ROLE_TUTOR = "tutor"
VALID_ROLES = {ROLE_STUDENT, ROLE_TUTOR}

# ============================================================================
# 问题状态
# ============================================================================
STATUS_PENDING = "pending"
STATUS_IN_PROGRESS = "in_progress"
STATUS_CLOSED = "closed"
VALID_STATUSES = {STATUS_PENDING, STATUS_IN_PROGRESS, STATUS_CLOSED}

# ============================================================================
# 消息类型
# ============================================================================
MESSAGE_TYPE_TEXT = "text"
MESSAGE_TYPE_IMAGE = "image"
VALID_MESSAGE_TYPES = {MESSAGE_TYPE_TEXT, MESSAGE_TYPE_IMAGE}

# ============================================================================
# WebSocket消息类型
# ============================================================================
WS_TYPE_NEW_QUESTION = "NEW_QUESTION"
WS_TYPE_QUESTION_UPDATED = "QUESTION_UPDATED"
WS_TYPE_QUESTION_ACCEPTED = "QUESTION_ACCEPTED"
WS_TYPE_QUESTION_CLOSED = "QUESTION_CLOSED"
WS_TYPE_CHAT_MESSAGE = "CHAT_MESSAGE"
WS_TYPE_ACK = "ACK"

# ============================================================================
# HTTP状态码
# ============================================================================
HTTP_200_OK = 200
HTTP_201_CREATED = 201
HTTP_400_BAD_REQUEST = 400
HTTP_401_UNAUTHORIZED = 401
HTTP_403_FORBIDDEN = 403
HTTP_404_NOT_FOUND = 404
HTTP_500_INTERNAL_SERVER_ERROR = 500

# ============================================================================
# 响应消息
# ============================================================================
MSG_SUCCESS = "Success"
MSG_REGISTRATION_SUCCESSFUL = "Registration successful"
MSG_LOGIN_SUCCESSFUL = "Login successful"
MSG_INVALID_CREDENTIALS = "Invalid username or password"
MSG_USERNAME_EXISTS = "Username already exists"
MSG_QUESTION_CREATED = "Question created successfully"
MSG_MESSAGE_SENT = "Message sent successfully"
MSG_QUESTION_ACCEPTED = "Question accepted"
MSG_QUESTION_CLOSED = "Question closed"
MSG_IMAGE_UPLOADED = "Image uploaded successfully"
MSG_USER_NOT_FOUND = "User not found"
MSG_QUESTION_NOT_FOUND = "Question not found"
MSG_INVALID_TOKEN = "Invalid or expired token"
MSG_INVALID_FILE_TYPE = "Invalid file type"
MSG_FILE_TOO_LARGE = "File too large"
MSG_ONLY_TUTORS_CAN_ACCEPT = "Only tutors can accept questions"
MSG_QUESTION_NOT_PENDING = "Question is not pending"

# ============================================================================
# 初始化配置
# ============================================================================
def init_config():
    """初始化配置，创建必要的目录"""
    # 创建上传目录
    Path(UPLOAD_DIR).mkdir(parents=True, exist_ok=True)
    

def get_config_info() -> dict:
    """获取配置信息（用于调试）"""
    return {
        "app_name": APP_NAME,
        "app_version": APP_VERSION,
        "debug": DEBUG,
        "host": HOST,
        "port": PORT,
        "database_url": DATABASE_URL.replace("sqlite+aiosqlite", "sqlite"),
        "upload_dir": UPLOAD_DIR,
        "max_file_size_mb": MAX_FILE_SIZE / 1024 / 1024,
        "log_level": LOG_LEVEL,
        "log_file": LOG_FILE,
    }

