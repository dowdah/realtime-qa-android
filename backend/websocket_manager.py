from fastapi import WebSocket
from fastapi.websockets import WebSocketDisconnect, WebSocketState
from typing import Dict, List, Any
import json
import asyncio
import logging

# 配置日志
logger = logging.getLogger(__name__)


class ConnectionManager:
    """WebSocket连接管理器"""
    
    def __init__(self) -> None:
        """初始化连接管理器"""
        self.active_connections: Dict[int, WebSocket] = {}
        self.user_roles: Dict[int, str] = {}
    
    async def connect(self, websocket: WebSocket, user_id: int, role: str) -> None:
        """
        连接新的WebSocket客户端
        
        Args:
            websocket: WebSocket连接对象
            user_id: 用户ID
            role: 用户角色
            
        Returns:
            None
        """
        await websocket.accept()
        self.active_connections[user_id] = websocket
        self.user_roles[user_id] = role
        logger.info(f"User {user_id} ({role}) connected. Total connections: {len(self.active_connections)}")
    
    def disconnect(self, user_id: int) -> None:
        """
        断开WebSocket客户端连接
        
        Args:
            user_id: 用户ID
            
        Returns:
            None
        """
        if user_id in self.active_connections:
            del self.active_connections[user_id]
        if user_id in self.user_roles:
            del self.user_roles[user_id]
        logger.info(f"User {user_id} disconnected. Total connections: {len(self.active_connections)}")
    
    async def send_personal_message(self, message: Dict[str, Any], user_id: int) -> None:
        """
        发送个人消息
        
        Args:
            message: 消息字典
            user_id: 目标用户ID
            
        Returns:
            None
        """
        if user_id in self.active_connections:
            try:
                await self.active_connections[user_id].send_text(json.dumps(message))
                logger.debug(f"Sent message to user {user_id}: {message.get('type', 'unknown')}")
            except (WebSocketDisconnect, RuntimeError, ConnectionError) as e:
                logger.error(f"Error sending message to user {user_id}: {e}")
                self.disconnect(user_id)
    
    async def broadcast_to_tutors(self, message: Dict[str, Any]) -> None:
        """
        广播消息到所有连接的老师
        
        Args:
            message: 要广播的消息字典
            
        Returns:
            None
        """
        disconnected_users: List[int] = []
        tutor_count = 0
        
        for user_id, websocket in self.active_connections.items():
            if self.user_roles.get(user_id) == "tutor":
                tutor_count += 1
                try:
                    await websocket.send_text(json.dumps(message))
                except (WebSocketDisconnect, RuntimeError, ConnectionError) as e:
                    logger.error(f"Error broadcasting to tutor {user_id}: {e}")
                    disconnected_users.append(user_id)
        
        # Clean up disconnected users
        for user_id in disconnected_users:
            self.disconnect(user_id)
        
        logger.debug(f"Broadcast to {tutor_count} tutors: {message.get('type', 'unknown')}")
    
    async def broadcast_to_students(self, message: Dict[str, Any]) -> None:
        """
        广播消息到所有连接的学生
        
        Args:
            message: 要广播的消息字典
            
        Returns:
            None
        """
        disconnected_users: List[int] = []
        student_count = 0
        
        for user_id, websocket in self.active_connections.items():
            if self.user_roles.get(user_id) == "student":
                student_count += 1
                try:
                    await websocket.send_text(json.dumps(message))
                except (WebSocketDisconnect, RuntimeError, ConnectionError) as e:
                    logger.error(f"Error broadcasting to student {user_id}: {e}")
                    disconnected_users.append(user_id)
        
        # Clean up disconnected users
        for user_id in disconnected_users:
            self.disconnect(user_id)
        
        logger.debug(f"Broadcast to {student_count} students: {message.get('type', 'unknown')}")
    
    async def send_ack(self, user_id: int, message_id: str) -> None:
        """
        发送确认消息
        
        Args:
            user_id: 用户ID
            message_id: 消息ID
            
        Returns:
            None
        """
        ack_message = {
            "type": "ACK",
            "messageId": message_id,
            "timestamp": str(int(asyncio.get_event_loop().time() * 1000))
        }
        await self.send_personal_message(ack_message, user_id)


manager = ConnectionManager()

