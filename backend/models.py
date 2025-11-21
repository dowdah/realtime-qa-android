from sqlalchemy import Column, Integer, String, Text, BigInteger, ForeignKey, Boolean, Index
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from typing import Dict, List, Any, Optional
import json
import time

Base = declarative_base()


class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, autoincrement=True, index=True)
    username = Column(String(100), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    role = Column(String(20), nullable=False, index=True)  # 'student' or 'tutor'
    created_at = Column(BigInteger, nullable=False)
    is_deleted = Column(Boolean, default=False, nullable=False, index=True)  # 软删除标记
    deleted_at = Column(BigInteger, nullable=True)  # 删除时间
    
    questions = relationship("Question", back_populates="user", foreign_keys="Question.user_id")
    tutoring_questions = relationship("Question", foreign_keys="Question.tutor_id")
    
    def to_dict(self, include_sensitive: bool = False) -> Dict[str, Any]:
        """
        转换为字典，默认不包含敏感信息
        
        Args:
            include_sensitive: 是否包含敏感信息（如密码哈希）
            
        Returns:
            Dict[str, Any]: 用户信息字典
        """
        data = {
            "id": self.id,
            "username": self.username,
            "role": self.role
        }
        if include_sensitive:
            data["password_hash"] = self.password_hash
        return data


class Question(Base):
    __tablename__ = "questions"
    
    id = Column(Integer, primary_key=True, autoincrement=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)  # 添加索引
    tutor_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)  # 添加索引
    content = Column(Text, nullable=False)
    image_paths = Column(Text, nullable=True)  # 存储 JSON 数组格式的多图片路径
    status = Column(String(20), default="pending", nullable=False, index=True)  # 添加索引
    created_at = Column(BigInteger, nullable=False, index=True)  # 添加索引用于排序
    updated_at = Column(BigInteger, nullable=False, index=True)  # 添加索引用于排序
    is_deleted = Column(Boolean, default=False, nullable=False, index=True)  # 软删除标记
    deleted_at = Column(BigInteger, nullable=True)  # 删除时间
    
    user = relationship("User", back_populates="questions", foreign_keys=[user_id])
    tutor = relationship("User", foreign_keys=[tutor_id], overlaps="tutoring_questions")
    messages = relationship("Message", back_populates="question", cascade="all, delete-orphan")
    
    # 复合索引：提高常见查询的性能
    __table_args__ = (
        Index('idx_status_created', 'status', 'created_at'),
        Index('idx_user_status', 'user_id', 'status'),
        Index('idx_tutor_status', 'tutor_id', 'status'),
    )
    
    def _parse_image_paths(self) -> List[str]:
        """
        解析JSON格式的图片路径
        
        Returns:
            List[str]: 图片路径列表，如果解析失败则返回空列表
        """
        if not self.image_paths:
            return []
        try:
            return json.loads(self.image_paths)
        except (json.JSONDecodeError, TypeError):
            return []
    
    def to_dict(self, full: bool = True) -> Dict[str, Any]:
        """
        转换为字典
        
        Args:
            full: True返回所有字段，False返回部分字段（用于简化响应）
            
        Returns:
            Dict[str, Any]: 问题信息字典
        """
        data = {
            "id": self.id,
            "userId": self.user_id,
            "tutorId": self.tutor_id,
            "status": self.status,
            "updatedAt": self.updated_at
        }
        if full:
            data.update({
                "content": self.content,
                "imagePaths": self._parse_image_paths(),
                "createdAt": self.created_at
            })
        return data
    
    def to_ws_message(self, message_type: str = "QUESTION_UPDATED") -> Dict[str, Any]:
        """
        创建 WebSocket 消息
        
        Args:
            message_type: 消息类型（NEW_QUESTION, QUESTION_UPDATED 等）
            
        Returns:
            Dict[str, Any]: WebSocket消息字典
        """
        return {
            "type": message_type,
            "data": {
                "questionId": self.id,
                "userId": self.user_id,
                "tutorId": self.tutor_id,
                "content": self.content,
                "imagePaths": self._parse_image_paths(),
                "status": self.status,
                "createdAt": self.created_at,
                "updatedAt": self.updated_at
            },
            "timestamp": str(int(time.time() * 1000))
        }


class Message(Base):
    __tablename__ = "messages"
    
    id = Column(Integer, primary_key=True, autoincrement=True, index=True)
    question_id = Column(Integer, ForeignKey("questions.id"), nullable=False, index=True)  # 添加索引
    sender_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)  # 添加索引
    content = Column(Text, nullable=False)
    message_type = Column(String(20), default="text", nullable=False)  # 'text', 'image'
    created_at = Column(BigInteger, nullable=False, index=True)  # 添加索引用于排序
    is_read = Column(Boolean, default=False, nullable=False)  # 消息是否已读
    is_deleted = Column(Boolean, default=False, nullable=False, index=True)  # 软删除标记
    deleted_at = Column(BigInteger, nullable=True)  # 删除时间
    
    question = relationship("Question", back_populates="messages")
    sender = relationship("User")
    
    # 复合索引：提高常见查询的性能
    __table_args__ = (
        Index('idx_question_created', 'question_id', 'created_at'),
    )
    
    def to_dict(self) -> Dict[str, Any]:
        """
        转换为字典
        
        Returns:
            Dict[str, Any]: 消息信息字典
        """
        return {
            "id": self.id,
            "questionId": self.question_id,
            "senderId": self.sender_id,
            "content": self.content,
            "messageType": self.message_type,
            "createdAt": self.created_at,
            "isRead": self.is_read
        }
    
    def to_ws_message(self) -> Dict[str, Any]:
        """
        创建 WebSocket 消息
        
        Returns:
            Dict[str, Any]: WebSocket消息字典
        """
        return {
            "type": "CHAT_MESSAGE",
            "data": self.to_dict(),
            "timestamp": str(int(time.time() * 1000))
        }

