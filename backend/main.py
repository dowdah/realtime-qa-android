import json
import logging
import time
from contextlib import asynccontextmanager
from pathlib import Path
from typing import List, Optional, Dict, Any

from fastapi import FastAPI, Depends, HTTPException, WebSocket, WebSocketDisconnect, UploadFile, File, Header, Query
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func
from sqlalchemy.orm import selectinload
from sqlalchemy.exc import SQLAlchemyError

import config
from database import init_db, get_db
from models import User, Question, Message
from auth import hash_password, verify_password, create_access_token, decode_access_token
from websocket_manager import manager

# 配置日志系统
log_handlers = []
if config.LOG_TO_FILE:
    log_handlers.append(logging.FileHandler(config.LOG_FILE))
if config.LOG_TO_CONSOLE:
    log_handlers.append(logging.StreamHandler())

logging.basicConfig(
    level=getattr(logging, config.LOG_LEVEL),
    format=config.LOG_FORMAT,
    handlers=log_handlers
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    config.init_config()
    await init_db()
    logger.info("Database initialized successfully")
    logger.info(f"Configuration: {config.get_config_info()}")
    yield
    # Shutdown (如果需要清理资源可以在这里添加)
    logger.info("Application shutting down")


app = FastAPI(title=config.APP_NAME, version=config.APP_VERSION, lifespan=lifespan)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=config.CORS_ORIGINS,
    allow_credentials=config.CORS_ALLOW_CREDENTIALS,
    allow_methods=config.CORS_ALLOW_METHODS,
    allow_headers=config.CORS_ALLOW_HEADERS,
)


# Health check endpoint
@app.get("/")
async def health_check() -> Dict[str, Any]:
    """
    健康检查端点
    
    Returns:
        Dict[str, Any]: 包含状态信息的字典
    """
    return {
        "status": "ok",
        "message": f"{config.APP_NAME} is running",
        "version": config.APP_VERSION,
        "timestamp": int(time.time() * 1000)
    }


# Pydantic models


class RegisterRequest(BaseModel):
    username: str
    password: str
    role: str


class LoginRequest(BaseModel):
    username: str
    password: str


class QuestionRequest(BaseModel):
    content: str
    imagePaths: Optional[List[str]] = None


class MessageRequest(BaseModel):
    questionId: int
    content: str
    messageType: str = "text"


class QuestionActionRequest(BaseModel):
    questionId: int


class MarkReadRequest(BaseModel):
    questionId: int


# 依赖注入：从Header获取并验证token
async def get_token_from_header(authorization: str = Header(...)) -> str:
    """
    从请求头中提取token
    
    Args:
        authorization: Authorization头部内容
        
    Returns:
        str: JWT令牌
        
    Raises:
        HTTPException: 如果头部格式无效
    """
    if not authorization or not authorization.startswith(config.AUTH_HEADER_PREFIX):
        logger.warning("Invalid authorization header format")
        raise HTTPException(status_code=401, detail="Invalid authorization header")
    return authorization.replace(config.AUTH_HEADER_PREFIX, "")


# 依赖注入：获取当前用户
async def get_current_user(
    token: str = Depends(get_token_from_header),
    db: AsyncSession = Depends(get_db)
) -> User:
    """
    验证token并返回当前用户
    
    Args:
        token: JWT令牌
        db: 数据库会话
        
    Returns:
        User: 当前用户对象
        
    Raises:
        HTTPException: 如果令牌无效或用户不存在
    """
    payload = decode_access_token(token)
    
    if not payload:
        logger.warning("Invalid or expired token")
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    
    user_id = payload.get("user_id")
    if not user_id:
        logger.warning("Token missing user_id")
        raise HTTPException(status_code=401, detail="Invalid token payload")
    
    result = await db.execute(
        select(User).where(
            and_(User.id == user_id, User.is_deleted == config.USER_NOT_DELETED)
        )
    )
    user = result.scalar_one_or_none()
    
    if not user:
        logger.warning(f"User {user_id} not found or deleted")
        raise HTTPException(status_code=401, detail="User not found")
    
    return user


# 文件验证辅助函数
def validate_image_file(file: UploadFile) -> None:
    """
    验证上传的图片文件
    
    Args:
        file: 上传的文件对象
        
    Raises:
        HTTPException: 如果文件类型或扩展名无效
    """
    # 检查文件类型 - 接受任何image/*类型或精确匹配的类型
    if file.content_type:
        # 检查是否是image类型（允许image/*或image/jpeg等）
        if not (file.content_type.startswith("image/") or 
                file.content_type in config.ALLOWED_IMAGE_TYPES):
            raise HTTPException(
                status_code=config.HTTP_400_BAD_REQUEST,
                detail=f"Invalid file type: {file.content_type}. Only image files are allowed."
            )
    
    # 检查文件扩展名（主要的安全验证）
    file_ext = Path(file.filename).suffix.lower()
    if file_ext not in config.ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=config.HTTP_400_BAD_REQUEST,
            detail=f"Invalid file extension. Allowed: {', '.join(config.ALLOWED_EXTENSIONS)}"
        )
    
    # 检查文件名安全性
    if ".." in file.filename or "/" in file.filename or "\\" in file.filename:
        raise HTTPException(status_code=config.HTTP_400_BAD_REQUEST, detail="Invalid filename")


# Auth endpoints
@app.post("/api/register")
async def register(request: RegisterRequest, db: AsyncSession = Depends(get_db)) -> Dict[str, Any]:
    """
    用户注册
    
    Args:
        request: 注册请求数据
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 注册结果
        
    Raises:
        HTTPException: 如果注册失败
    """
    try:
        # Check if username exists
        result = await db.execute(
            select(User).where(
                and_(User.username == request.username, User.is_deleted == config.USER_NOT_DELETED)
            )
        )
        existing_user = result.scalar_one_or_none()
        
        if existing_user:
            logger.info(f"Registration failed: username '{request.username}' already exists")
            return {
                "success": False,
                "message": config.MSG_USERNAME_EXISTS
            }
        
        # Create new user
        hashed_pw = hash_password(request.password)
        new_user = User(
            username=request.username,
            password_hash=hashed_pw,
            role=request.role,
            created_at=int(time.time() * 1000),
            is_deleted=config.USER_NOT_DELETED
        )
        
        db.add(new_user)
        await db.commit()
        await db.refresh(new_user)
        
        # Create token
        token = create_access_token({"user_id": new_user.id, "role": new_user.role})
        
        logger.info(f"User registered successfully: {new_user.username} (ID: {new_user.id})")
        return {
            "success": True,
            "message": config.MSG_REGISTRATION_SUCCESSFUL,
            "token": token,
            "user": new_user.to_dict()
        }
    except SQLAlchemyError as e:
        logger.error(f"Database error during registration: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Registration failed")
    except Exception as e:
        logger.error(f"Unexpected error during registration: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Registration failed")


@app.post("/api/login")
async def login(request: LoginRequest, db: AsyncSession = Depends(get_db)) -> Dict[str, Any]:
    """
    用户登录
    
    Args:
        request: 登录请求数据
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 登录结果
        
    Raises:
        HTTPException: 如果登录失败
    """
    try:
        # Find user
        result = await db.execute(
            select(User).where(
                and_(User.username == request.username, User.is_deleted == config.USER_NOT_DELETED)
            )
        )
        user = result.scalar_one_or_none()
        
        if not user or not verify_password(request.password, user.password_hash):
            logger.warning(f"Login failed for username: {request.username}")
            return {
                "success": False,
                "message": config.MSG_INVALID_CREDENTIALS
            }
        
        # Create token
        token = create_access_token({"user_id": user.id, "role": user.role})
        
        logger.info(f"User logged in: {user.username} (ID: {user.id})")
        return {
            "success": True,
            "message": config.MSG_LOGIN_SUCCESSFUL,
            "token": token,
            "user": user.to_dict()
        }
    except SQLAlchemyError as e:
        logger.error(f"Database error during login: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Login failed")
    except Exception as e:
        logger.error(f"Unexpected error during login: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Login failed")


# Question endpoints
@app.get("/api/questions")
async def get_questions(
    current_user: User = Depends(get_current_user),
    status: str = None,
    page: int = Query(1, ge=1, description="页码，从1开始"),
    page_size: int = Query(config.DEFAULT_QUESTIONS_PAGE_SIZE, ge=1, le=config.MAX_QUESTIONS_PAGE_SIZE, 
                           description=f"每页数量，1-{config.MAX_QUESTIONS_PAGE_SIZE}"),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    获取问题列表（支持分页）
    
    Args:
        current_user: 当前用户
        status: 问题状态过滤
        page: 页码
        page_size: 每页数量
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 问题列表和分页信息
        
    Raises:
        HTTPException: 如果查询失败
    """
    try:
        # 构建基础查询（排除已删除的问题）
        query = select(Question).where(Question.is_deleted == config.USER_NOT_DELETED)
        
        if status:
            query = query.where(Question.status == status)
        else:
            if current_user.role == config.ROLE_STUDENT:
                # 学生只获取自己创建的问题
                query = query.where(Question.user_id == current_user.id)
            elif current_user.role == config.ROLE_TUTOR:
                # 老师获取自己接取的问题（in_progress和closed状态）
                query = query.where(Question.tutor_id == current_user.id)
        
        # 获取总数（用于分页）
        count_query = select(func.count()).select_from(query.subquery())
        total_result = await db.execute(count_query)
        total = total_result.scalar()
        
        # 应用分页和排序
        query = query.order_by(Question.created_at.desc())
        query = query.offset((page - 1) * page_size).limit(page_size)
        
        result = await db.execute(query)
        questions = result.scalars().all()
        
        logger.info(f"User {current_user.id} fetched {len(questions)} questions (page {page})")
        return {
            "success": True,
            "questions": [q.to_dict() for q in questions],
            "pagination": {
                "page": page,
                "pageSize": page_size,
                "total": total,
                "totalPages": (total + page_size - 1) // page_size
            }
        }
    except SQLAlchemyError as e:
        logger.error(f"Database error fetching questions: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Failed to fetch questions")
    except Exception as e:
        logger.error(f"Unexpected error fetching questions: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Failed to fetch questions")


@app.post("/api/questions")
async def create_question(
    request: QuestionRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    创建新问题
    
    Args:
        request: 问题请求数据
        current_user: 当前用户
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 创建结果
        
    Raises:
        HTTPException: 如果创建失败
    """
    try:
        current_time = int(time.time() * 1000)
        
        # 将图片路径列表转换为 JSON 字符串存储
        image_paths_json = None
        if request.imagePaths:
            image_paths_json = json.dumps(request.imagePaths)
        
        new_question = Question(
            user_id=current_user.id,
            content=request.content,
            image_paths=image_paths_json,
            status=config.STATUS_PENDING,
            created_at=current_time,
            updated_at=current_time,
            is_deleted=config.USER_NOT_DELETED
        )
        
        db.add(new_question)
        await db.commit()
        await db.refresh(new_question)
        
        # 广播新问题给所有老师
        await manager.broadcast_to_tutors(new_question.to_ws_message(config.WS_TYPE_NEW_QUESTION))
        
        logger.info(f"Question created: ID {new_question.id} by user {current_user.id}")
        return {
            "success": True,
            "message": config.MSG_QUESTION_CREATED,
            "question": new_question.to_dict()
        }
    except SQLAlchemyError as e:
        logger.error(f"Database error creating question: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to create question")
    except Exception as e:
        logger.error(f"Unexpected error creating question: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to create question")


# Message endpoints
@app.post("/api/messages")
async def send_message(
    request: MessageRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    发送消息
    
    Args:
        request: 消息请求数据
        current_user: 当前用户
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 发送结果
        
    Raises:
        HTTPException: 如果发送失败
    """
    try:
        # 检查问题是否存在且未删除
        result = await db.execute(
            select(Question).where(
                and_(Question.id == request.questionId, Question.is_deleted == config.USER_NOT_DELETED)
            )
        )
        question = result.scalar_one_or_none()
        if not question:
            raise HTTPException(status_code=404, detail=config.MSG_QUESTION_NOT_FOUND)
        
        # 创建消息
        new_message = Message(
            question_id=request.questionId,
            sender_id=current_user.id,
            content=request.content,
            message_type=request.messageType,
            created_at=int(time.time() * 1000),
            is_read=False,
            is_deleted=config.USER_NOT_DELETED
        )
        
        db.add(new_message)
        
        # 更新问题的updated_at时间
        question.updated_at = int(time.time() * 1000)
        
        await db.commit()
        await db.refresh(new_message)
        
        # 通过 WebSocket 发送消息到对方（学生发给老师，老师发给学生）
        if current_user.role == config.ROLE_STUDENT and question.tutor_id:
            await manager.send_personal_message(new_message.to_ws_message(), question.tutor_id)
        elif current_user.role == config.ROLE_TUTOR and question.user_id:
            await manager.send_personal_message(new_message.to_ws_message(), question.user_id)
        
        logger.info(f"Message sent: ID {new_message.id} for question {question.id}")
        return {
            "success": True,
            "message": config.MSG_MESSAGE_SENT,
            "data": new_message.to_dict()
        }
    except HTTPException:
        raise
    except SQLAlchemyError as e:
        logger.error(f"Database error sending message: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to send message")
    except Exception as e:
        logger.error(f"Unexpected error sending message: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to send message")


@app.get("/api/messages")
async def get_messages(
    questionId: int,
    page: int = Query(1, ge=1, description="页码，从1开始"),
    page_size: int = Query(config.DEFAULT_MESSAGES_PAGE_SIZE, ge=1, le=config.MAX_MESSAGES_PAGE_SIZE,
                           description=f"每页数量，1-{config.MAX_MESSAGES_PAGE_SIZE}"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    获取消息列表（支持分页）
    
    Args:
        questionId: 问题ID
        page: 页码
        page_size: 每页数量
        current_user: 当前用户
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 消息列表和分页信息
        
    Raises:
        HTTPException: 如果查询失败
    """
    try:
        # 验证问题是否存在且未删除
        question_result = await db.execute(
            select(Question).where(
                and_(Question.id == questionId, Question.is_deleted == config.USER_NOT_DELETED)
            )
        )
        question = question_result.scalar_one_or_none()
        if not question:
            raise HTTPException(status_code=404, detail=config.MSG_QUESTION_NOT_FOUND)
        
        # 验证用户权限（只有问题的创建者或接受者可以查看消息）
        if current_user.id != question.user_id and current_user.id != question.tutor_id:
            raise HTTPException(status_code=403, detail="No permission to view these messages")
        
        # 构建查询（排除已删除的消息）
        query = select(Message).where(
            and_(Message.question_id == questionId, Message.is_deleted == config.USER_NOT_DELETED)
        )
        
        # 获取总数
        count_query = select(func.count()).select_from(query.subquery())
        total_result = await db.execute(count_query)
        total = total_result.scalar()
        
        # 应用分页和排序
        query = query.order_by(Message.created_at.asc())
        query = query.offset((page - 1) * page_size).limit(page_size)
        
        # 使用 selectinload 预加载发送者信息（解决 N+1 查询问题）
        query = query.options(selectinload(Message.sender))
        
        result = await db.execute(query)
        messages = result.scalars().all()
        
        logger.info(f"User {current_user.id} fetched {len(messages)} messages for question {questionId}")
        return {
            "success": True,
            "messages": [m.to_dict() for m in messages],
            "pagination": {
                "page": page,
                "pageSize": page_size,
                "total": total,
                "totalPages": (total + page_size - 1) // page_size
            }
        }
    except HTTPException:
        raise
    except SQLAlchemyError as e:
        logger.error(f"Database error fetching messages: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Failed to fetch messages")
    except Exception as e:
        logger.error(f"Unexpected error fetching messages: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Failed to fetch messages")


@app.post("/api/messages/mark-read")
async def mark_messages_as_read(
    request: MarkReadRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    标记消息为已读
    
    Args:
        request: 标记已读请求
        current_user: 当前用户
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 标记结果
        
    Raises:
        HTTPException: 如果操作失败
    """
    try:
        # 验证问题是否存在且未删除
        question_result = await db.execute(
            select(Question).where(
                and_(Question.id == request.questionId, Question.is_deleted == config.USER_NOT_DELETED)
            )
        )
        question = question_result.scalar_one_or_none()
        if not question:
            raise HTTPException(status_code=404, detail=config.MSG_QUESTION_NOT_FOUND)
        
        # 验证用户权限（只有问题的创建者或接受者可以标记消息为已读）
        if current_user.id != question.user_id and current_user.id != question.tutor_id:
            raise HTTPException(status_code=403, detail="No permission to mark messages as read")
        
        # 标记所有不是当前用户发送的消息为已读
        result = await db.execute(
            select(Message).where(
                and_(
                    Message.question_id == request.questionId,
                    Message.sender_id != current_user.id,
                    Message.is_read == False,
                    Message.is_deleted == config.USER_NOT_DELETED
                )
            )
        )
        messages_to_mark = result.scalars().all()
        
        # 更新消息的已读状态
        for message in messages_to_mark:
            message.is_read = True
        
        await db.commit()
        
        logger.info(f"User {current_user.id} marked {len(messages_to_mark)} messages as read for question {request.questionId}")
        return {
            "success": True,
            "message": "Messages marked as read",
            "count": len(messages_to_mark)
        }
    except HTTPException:
        raise
    except SQLAlchemyError as e:
        logger.error(f"Database error marking messages as read: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to mark messages as read")
    except Exception as e:
        logger.error(f"Unexpected error marking messages as read: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to mark messages as read")


# Question status management
@app.post("/api/questions/accept")
async def accept_question(
    request: QuestionActionRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    老师接受问题
    
    Args:
        request: 问题操作请求
        current_user: 当前用户
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 操作结果
        
    Raises:
        HTTPException: 如果操作失败
    """
    try:
        if current_user.role != config.ROLE_TUTOR:
            raise HTTPException(status_code=403, detail=config.MSG_ONLY_TUTORS_CAN_ACCEPT)
        
        result = await db.execute(
            select(Question).where(
                and_(Question.id == request.questionId, Question.is_deleted == config.USER_NOT_DELETED)
            )
        )
        question = result.scalar_one_or_none()
        
        if not question:
            raise HTTPException(status_code=404, detail=config.MSG_QUESTION_NOT_FOUND)
        
        if question.status != config.STATUS_PENDING:
            raise HTTPException(status_code=400, detail=config.MSG_QUESTION_NOT_PENDING)
        
        question.status = config.STATUS_IN_PROGRESS
        question.tutor_id = current_user.id
        question.updated_at = int(time.time() * 1000)
        
        await db.commit()
        await db.refresh(question)
        
        # 广播问题状态更新到相关用户（学生和老师）
        ws_message = question.to_ws_message(config.WS_TYPE_QUESTION_UPDATED)
        
        # 发送给学生
        await manager.send_personal_message(ws_message, question.user_id)
        # 发送给所有老师（移除已接受的问题）
        await manager.broadcast_to_tutors(ws_message)
        
        logger.info(f"Question {question.id} accepted by tutor {current_user.id}")
        return {
            "success": True,
            "message": config.MSG_QUESTION_ACCEPTED,
            "question": question.to_dict(full=False)
        }
    except HTTPException:
        raise
    except SQLAlchemyError as e:
        logger.error(f"Database error accepting question: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to accept question")
    except Exception as e:
        logger.error(f"Unexpected error accepting question: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to accept question")


@app.post("/api/questions/close")
async def close_question(
    request: QuestionActionRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    关闭问题
    
    Args:
        request: 问题操作请求
        current_user: 当前用户
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 操作结果
        
    Raises:
        HTTPException: 如果操作失败
    """
    try:
        result = await db.execute(
            select(Question).where(
                and_(Question.id == request.questionId, Question.is_deleted == config.USER_NOT_DELETED)
            )
        )
        question = result.scalar_one_or_none()
        
        if not question:
            raise HTTPException(status_code=404, detail=config.MSG_QUESTION_NOT_FOUND)
        
        # 只有老师或者学生本人可以关闭问题
        if current_user.role == config.ROLE_TUTOR and question.tutor_id != current_user.id:
            raise HTTPException(status_code=403, detail="You are not assigned to this question")
        
        if current_user.role == config.ROLE_STUDENT and question.user_id != current_user.id:
            raise HTTPException(status_code=403, detail="You can only close your own questions")
        
        question.status = config.STATUS_CLOSED
        question.updated_at = int(time.time() * 1000)
        
        await db.commit()
        await db.refresh(question)
        
        # 广播问题状态更新到相关用户
        ws_message = question.to_ws_message(config.WS_TYPE_QUESTION_UPDATED)
        
        # 发送给学生
        await manager.send_personal_message(ws_message, question.user_id)
        # 如果有老师，也发送给老师
        if question.tutor_id:
            await manager.send_personal_message(ws_message, question.tutor_id)
        
        logger.info(f"Question {question.id} closed by user {current_user.id}")
        return {
            "success": True,
            "message": config.MSG_QUESTION_CLOSED,
            "question": question.to_dict(full=False)
        }
    except HTTPException:
        raise
    except SQLAlchemyError as e:
        logger.error(f"Database error closing question: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to close question")
    except Exception as e:
        logger.error(f"Unexpected error closing question: {str(e)}", exc_info=True)
        await db.rollback()
        raise HTTPException(status_code=500, detail="Failed to close question")


# Upload endpoint
@app.post("/api/upload")
async def upload_image(
    image: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
) -> Dict[str, Any]:
    """
    上传图片文件
    
    Args:
        image: 上传的图片文件
        current_user: 当前用户
        db: 数据库会话
        
    Returns:
        Dict[str, Any]: 上传结果
        
    Raises:
        HTTPException: 如果上传失败
    """
    try:
        # 验证文件
        validate_image_file(image)
        
        # 读取文件内容并检查大小
        file_content = await image.read()
        file_size = len(file_content)
        
        if file_size > config.MAX_FILE_SIZE:
            raise HTTPException(
                status_code=config.HTTP_400_BAD_REQUEST,
                detail=f"File too large. Max size: {config.MAX_FILE_SIZE / 1024 / 1024}MB"
            )
        
        if file_size == 0:
            raise HTTPException(status_code=400, detail="File is empty")
        
        # Create user upload directory
        user_upload_dir = Path(config.UPLOAD_DIR) / str(current_user.id)
        user_upload_dir.mkdir(parents=True, exist_ok=True)
        
        # Generate safe filename
        timestamp = int(time.time() * 1000)
        file_ext = Path(image.filename).suffix.lower()
        safe_filename = f"{timestamp}{file_ext}"
        file_path = user_upload_dir / safe_filename
        
        # Save file
        with open(str(file_path), "wb") as buffer:
            buffer.write(file_content)
        
        # Return relative path
        relative_path = f"/uploads/{current_user.id}/{safe_filename}"
        
        logger.info(f"Image uploaded: {relative_path} by user {current_user.id} ({file_size} bytes)")
        return {
            "success": True,
            "message": config.MSG_IMAGE_UPLOADED,
            "imagePath": relative_path
        }
    except HTTPException:
        raise
    except (IOError, OSError) as e:
        logger.error(f"File system error uploading image: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Failed to upload image")
    except Exception as e:
        logger.error(f"Unexpected error uploading image: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Failed to upload image")


# Serve uploaded images
@app.get("/uploads/{user_id}/{filename}")
async def get_image(user_id: int, filename: str) -> FileResponse:
    """
    获取上传的图片
    
    Args:
        user_id: 用户ID
        filename: 文件名
        
    Returns:
        FileResponse: 图片文件响应
        
    Raises:
        HTTPException: 如果文件不存在或无效
    """
    try:
        # 验证文件名安全性
        if ".." in filename or "/" in filename or "\\" in filename:
            raise HTTPException(status_code=config.HTTP_400_BAD_REQUEST, detail="Invalid filename")
        
        file_path = Path(config.UPLOAD_DIR) / str(user_id) / filename
        
        if not file_path.exists():
            raise HTTPException(status_code=config.HTTP_404_NOT_FOUND, detail="Image not found")
        
        # 验证文件类型
        file_ext = Path(filename).suffix.lower()
        if file_ext not in config.ALLOWED_EXTENSIONS:
            raise HTTPException(status_code=config.HTTP_400_BAD_REQUEST, detail="Invalid file type")
        
        return FileResponse(str(file_path))
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Unexpected error serving image: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Failed to serve image")


# WebSocket endpoint
@app.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: int) -> None:
    """
    WebSocket连接端点
    
    Args:
        websocket: WebSocket连接对象
        user_id: 用户ID
        
    Returns:
        None
    """
    # Get user role from query params or database
    db_gen = get_db()
    db = await anext(db_gen)
    
    # 先查询用户，但必须先accept才能正确关闭连接
    try:
        result = await db.execute(
            select(User).where(and_(User.id == user_id, User.is_deleted == config.USER_NOT_DELETED))
        )
        user = result.scalar_one_or_none()
        
        if not user:
            logger.warning(f"WebSocket connection rejected: user {user_id} not found")
            # 必须先accept连接才能发送close消息
            await websocket.accept()
            await websocket.close(code=1008, reason="User not found")
            return
        
        await manager.connect(websocket, user_id, user.role)
        logger.info(f"WebSocket connected: user {user_id} ({user.role})")
        
        # If tutor connects, send all pending questions
        if user.role == config.ROLE_TUTOR:
            result = await db.execute(
                select(Question).where(
                    and_(Question.status == config.STATUS_PENDING, Question.is_deleted == config.USER_NOT_DELETED)
                ).order_by(Question.created_at.asc())
            )
            pending_questions = result.scalars().all()
            
            for question in pending_questions:
                await manager.send_personal_message(question.to_ws_message(config.WS_TYPE_NEW_QUESTION), user_id)
            
            if pending_questions:
                logger.info(f"Sent {len(pending_questions)} pending questions to tutor {user_id}")
        
        try:
            while True:
                data = await websocket.receive_text()
                message = json.loads(data)
                
                message_type = message.get("type")
                message_id = message.get("messageId")
                
                # Send ACK
                if message_id:
                    await manager.send_ack(user_id, message_id)
                
                # 注意：客户端不再通过 WebSocket 发送业务消息
                # 所有消息现在都通过 HTTP API 处理，后端自动推送
                # 保留此处以便接收心跳或其他控制消息
                logger.debug(f"Received WebSocket message type={message_type} from user {user_id}")
                
                # 如果将来需要处理特殊的 WebSocket 消息，可以在这里添加
                # 例如：心跳检测、在线状态更新等
                
                # 以下转发逻辑已废弃，因为客户端不再发送这些消息：
                # - NEW_QUESTION: 由 /api/questions POST 处理并广播
                # - CHAT_MESSAGE: 由 /api/messages POST 处理并推送
                # - QUESTION_ACCEPTED: 由 /api/questions/accept POST 处理并推送
                # - QUESTION_CLOSED: 由 /api/questions/close POST 处理并推送
        
        except WebSocketDisconnect:
            manager.disconnect(user_id)
            logger.info(f"WebSocket disconnected: user {user_id}")
        except (json.JSONDecodeError, KeyError) as e:
            logger.error(f"WebSocket message parse error for user {user_id}: {str(e)}", exc_info=True)
            manager.disconnect(user_id)
        except Exception as e:
            logger.error(f"WebSocket error for user {user_id}: {str(e)}", exc_info=True)
            manager.disconnect(user_id)
    
    finally:
        await db.close()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=config.HOST, port=config.PORT, reload=config.RELOAD)

