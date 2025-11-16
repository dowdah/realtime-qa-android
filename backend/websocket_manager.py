from fastapi import WebSocket
from typing import Dict, List
import json
import asyncio
import logging

# 配置日志
logger = logging.getLogger(__name__)


class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[int, WebSocket] = {}
        self.user_roles: Dict[int, str] = {}
    
    async def connect(self, websocket: WebSocket, user_id: int, role: str):
        """连接新的WebSocket客户端"""
        await websocket.accept()
        self.active_connections[user_id] = websocket
        self.user_roles[user_id] = role
        logger.info(f"User {user_id} ({role}) connected. Total connections: {len(self.active_connections)}")
    
    def disconnect(self, user_id: int):
        """断开WebSocket客户端连接"""
        if user_id in self.active_connections:
            del self.active_connections[user_id]
        if user_id in self.user_roles:
            del self.user_roles[user_id]
        logger.info(f"User {user_id} disconnected. Total connections: {len(self.active_connections)}")
    
    async def send_personal_message(self, message: dict, user_id: int):
        """发送个人消息"""
        if user_id in self.active_connections:
            try:
                await self.active_connections[user_id].send_text(json.dumps(message))
                logger.debug(f"Sent message to user {user_id}: {message.get('type', 'unknown')}")
            except Exception as e:
                logger.error(f"Error sending message to user {user_id}: {e}")
                self.disconnect(user_id)
    
    async def broadcast_to_tutors(self, message: dict):
        """广播消息到所有连接的老师"""
        disconnected_users = []
        tutor_count = 0
        
        for user_id, websocket in self.active_connections.items():
            if self.user_roles.get(user_id) == "tutor":
                tutor_count += 1
                try:
                    await websocket.send_text(json.dumps(message))
                except Exception as e:
                    logger.error(f"Error broadcasting to tutor {user_id}: {e}")
                    disconnected_users.append(user_id)
        
        # Clean up disconnected users
        for user_id in disconnected_users:
            self.disconnect(user_id)
        
        logger.debug(f"Broadcast to {tutor_count} tutors: {message.get('type', 'unknown')}")
    
    async def broadcast_to_students(self, message: dict):
        """广播消息到所有连接的学生"""
        disconnected_users = []
        student_count = 0
        
        for user_id, websocket in self.active_connections.items():
            if self.user_roles.get(user_id) == "student":
                student_count += 1
                try:
                    await websocket.send_text(json.dumps(message))
                except Exception as e:
                    logger.error(f"Error broadcasting to student {user_id}: {e}")
                    disconnected_users.append(user_id)
        
        # Clean up disconnected users
        for user_id in disconnected_users:
            self.disconnect(user_id)
        
        logger.debug(f"Broadcast to {student_count} students: {message.get('type', 'unknown')}")
    
    async def send_ack(self, user_id: int, message_id: str):
        """发送确认消息"""
        ack_message = {
            "type": "ACK",
            "messageId": message_id,
            "timestamp": str(int(asyncio.get_event_loop().time() * 1000))
        }
        await self.send_personal_message(ack_message, user_id)


manager = ConnectionManager()

