#!/usr/bin/env python3
"""
实时问答系统 API 自动化测试脚本

测试覆盖：
1. 用户注册和登录
2. 问题创建和查询
3. 问题接受和关闭
4. 消息发送和接收
5. WebSocket 连接和实时通信
"""

import asyncio
import json
import time
from datetime import datetime
from typing import Optional, Dict, Any, Tuple

import requests
import websockets

# 配置
BASE_URL = "http://localhost:8000"
WS_URL = "ws://localhost:8000"

# 颜色输出
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    BOLD = '\033[1m'
    END = '\033[0m'

def print_header(text: str) -> None:
    """打印测试标题"""
    print(f"\n{Colors.BOLD}{Colors.BLUE}{'=' * 60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.BLUE}{text:^60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.BLUE}{'=' * 60}{Colors.END}\n")

def print_success(text: str) -> None:
    """打印成功信息"""
    print(f"{Colors.GREEN}✅ {text}{Colors.END}")

def print_error(text: str) -> None:
    """打印错误信息"""
    print(f"{Colors.RED}❌ {text}{Colors.END}")

def print_info(text: str) -> None:
    """打印信息"""
    print(f"{Colors.YELLOW}ℹ️  {text}{Colors.END}")

def print_result(test_name: str, passed: bool, details: str = "") -> None:
    """打印测试结果"""
    status = "通过" if passed else "失败"
    color = Colors.GREEN if passed else Colors.RED
    symbol = "✅" if passed else "❌"
    print(f"{color}{symbol} [{status}] {test_name}{Colors.END}")
    if details:
        print(f"   详情: {details}")

class APITester:
    """API 测试类"""
    
    def __init__(self) -> None:
        self.base_url: str = BASE_URL
        self.ws_url: str = WS_URL
        self.student_token: Optional[str] = None
        self.tutor_token: Optional[str] = None
        self.student_id: Optional[int] = None
        self.tutor_id: Optional[int] = None
        self.test_question_id: Optional[int] = None
        self.test_results: list = []
        
    def add_result(self, test_name: str, passed: bool, details: str = "") -> None:
        """记录测试结果"""
        self.test_results.append({
            "test": test_name,
            "passed": passed,
            "details": details
        })
        print_result(test_name, passed, details)
    
    def test_register_user(self, username: str, password: str, role: str) -> Optional[Dict]:
        """测试用户注册"""
        print_info(f"测试用户注册: {username} ({role})")
        
        try:
            response = requests.post(
                f"{self.base_url}/api/register",
                json={
                    "username": username,
                    "password": password,
                    "role": role
                },
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    self.add_result(f"注册用户 {username}", True, f"用户ID: {data['user']['id']}")
                    return data
                else:
                    self.add_result(f"注册用户 {username}", False, data.get("message", "未知错误"))
            else:
                self.add_result(f"注册用户 {username}", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result(f"注册用户 {username}", False, str(e))
        
        return None
    
    def test_login_user(self, username: str, password: str) -> Tuple[Optional[str], Optional[int]]:
        """测试用户登录"""
        print_info(f"测试用户登录: {username}")
        
        try:
            response = requests.post(
                f"{self.base_url}/api/login",
                json={
                    "username": username,
                    "password": password
                },
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    token = data["token"]
                    user_id = data["user"]["id"]
                    self.add_result(f"登录用户 {username}", True, f"Token: {token[:20]}...")
                    return token, user_id
                else:
                    self.add_result(f"登录用户 {username}", False, data.get("message"))
            else:
                self.add_result(f"登录用户 {username}", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result(f"登录用户 {username}", False, str(e))
        
        return None, None
    
    def test_create_question(self, token: str, content: str) -> Optional[int]:
        """测试创建问题"""
        print_info(f"测试创建问题: {content[:30]}...")
        
        try:
            response = requests.post(
                f"{self.base_url}/api/questions",
                json={
                    "content": content,
                    "imagePaths": None
                },
                headers={"Authorization": f"Bearer {token}"},
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    question_id = data["question"]["id"]
                    self.add_result("创建问题", True, f"问题ID: {question_id}")
                    return question_id
                else:
                    self.add_result("创建问题", False, data.get("message"))
            else:
                self.add_result("创建问题", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result("创建问题", False, str(e))
        
        return None
    
    def test_get_questions(self, token: str, page: int = 1, page_size: int = 20) -> bool:
        """测试获取问题列表（支持分页）"""
        print_info(f"测试获取问题列表 (page={page}, page_size={page_size})")
        
        try:
            response = requests.get(
                f"{self.base_url}/api/questions",
                params={"page": page, "page_size": page_size},
                headers={"Authorization": f"Bearer {token}"},
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    questions = data.get("questions", [])
                    pagination = data.get("pagination", {})
                    details = f"当前页 {len(questions)} 个问题，总共 {pagination.get('total', 0)} 个"
                    self.add_result("获取问题列表", True, details)
                    return True
                else:
                    self.add_result("获取问题列表", False, data.get("message"))
            else:
                self.add_result("获取问题列表", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result("获取问题列表", False, str(e))
        
        return False
    
    def test_accept_question(self, token: str, question_id: int) -> bool:
        """测试接受问题"""
        print_info(f"测试接受问题: {question_id}")
        
        try:
            response = requests.post(
                f"{self.base_url}/api/questions/accept",
                json={"questionId": question_id},
                headers={"Authorization": f"Bearer {token}"},
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    self.add_result("接受问题", True, f"问题 {question_id} 已接受")
                    return True
                else:
                    self.add_result("接受问题", False, data.get("message"))
            else:
                self.add_result("接受问题", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result("接受问题", False, str(e))
        
        return False
    
    def test_send_message(self, token: str, question_id: int, content: str) -> bool:
        """测试发送消息"""
        print_info(f"测试发送消息: {content[:30]}...")
        
        try:
            response = requests.post(
                f"{self.base_url}/api/messages",
                json={
                    "questionId": question_id,
                    "content": content,
                    "messageType": "text"
                },
                headers={"Authorization": f"Bearer {token}"},
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    message_id = data["data"]["id"]
                    self.add_result("发送消息", True, f"消息ID: {message_id}")
                    return True
                else:
                    self.add_result("发送消息", False, data.get("message"))
            else:
                self.add_result("发送消息", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result("发送消息", False, str(e))
        
        return False
    
    def test_get_messages(self, token: str, question_id: int, page: int = 1, page_size: int = 50) -> bool:
        """测试获取消息列表（支持分页）"""
        print_info(f"测试获取消息列表: 问题 {question_id} (page={page}, page_size={page_size})")
        
        try:
            response = requests.get(
                f"{self.base_url}/api/messages",
                params={"questionId": question_id, "page": page, "page_size": page_size},
                headers={"Authorization": f"Bearer {token}"},
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    messages = data.get("messages", [])
                    pagination = data.get("pagination", {})
                    details = f"当前页 {len(messages)} 条消息，总共 {pagination.get('total', 0)} 条"
                    self.add_result("获取消息列表", True, details)
                    return True
                else:
                    self.add_result("获取消息列表", False, data.get("message"))
            else:
                self.add_result("获取消息列表", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result("获取消息列表", False, str(e))
        
        return False
    
    def test_close_question(self, token: str, question_id: int) -> bool:
        """测试关闭问题"""
        print_info(f"测试关闭问题: {question_id}")
        
        try:
            response = requests.post(
                f"{self.base_url}/api/questions/close",
                json={"questionId": question_id},
                headers={"Authorization": f"Bearer {token}"},
                timeout=5
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    self.add_result("关闭问题", True, f"问题 {question_id} 已关闭")
                    return True
                else:
                    self.add_result("关闭问题", False, data.get("message"))
            else:
                self.add_result("关闭问题", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            self.add_result("关闭问题", False, str(e))
        
        return False
    
    async def test_websocket_connection(self, user_id: int, role: str) -> bool:
        """测试 WebSocket 连接"""
        print_info(f"测试 WebSocket 连接: 用户 {user_id} ({role})")
        
        try:
            ws_uri = f"{self.ws_url}/ws/{user_id}"
            async with websockets.connect(ws_uri, ping_interval=None) as websocket:
                # 等待连接建立
                await asyncio.sleep(1)
                
                # 发送测试消息
                test_message = {
                    "type": "PING",
                    "timestamp": str(int(time.time() * 1000))
                }
                await websocket.send(json.dumps(test_message))
                
                # 等待响应（设置超时）
                try:
                    response = await asyncio.wait_for(websocket.recv(), timeout=3.0)
                    self.add_result(f"WebSocket 连接 ({role})", True, "连接成功并能收发消息")
                    return True
                except asyncio.TimeoutError:
                    # 没有响应也算连接成功（服务器可能不响应 PING）
                    self.add_result(f"WebSocket 连接 ({role})", True, "连接成功")
                    return True
                    
        except Exception as e:
            self.add_result(f"WebSocket 连接 ({role})", False, str(e))
        
        return False
    
    async def test_websocket_message_broadcast(self) -> bool:
        """测试 WebSocket 消息广播"""
        print_info("测试 WebSocket 消息广播")
        
        try:
            student_ws_uri = f"{self.ws_url}/ws/{self.student_id}"
            tutor_ws_uri = f"{self.ws_url}/ws/{self.tutor_id}"
            
            # 同时连接学生和老师
            async with websockets.connect(student_ws_uri, ping_interval=None) as student_ws, \
                       websockets.connect(tutor_ws_uri, ping_interval=None) as tutor_ws:
                
                await asyncio.sleep(1)
                
                # 学生发送新问题（通过 API）
                question_id = self.test_create_question(
                    self.student_token, 
                    "WebSocket 测试问题"
                )
                
                if question_id:
                    # 等待老师端收到 WebSocket 消息
                    try:
                        message = await asyncio.wait_for(tutor_ws.recv(), timeout=5.0)
                        data = json.loads(message)
                        
                        if data.get("type") == "NEW_QUESTION":
                            self.add_result("WebSocket 消息广播", True, "老师收到新问题通知")
                            return True
                        else:
                            self.add_result("WebSocket 消息广播", False, f"收到错误的消息类型: {data.get('type')}")
                    except asyncio.TimeoutError:
                        self.add_result("WebSocket 消息广播", False, "老师未收到消息（超时）")
                else:
                    self.add_result("WebSocket 消息广播", False, "创建问题失败")
                    
        except Exception as e:
            self.add_result("WebSocket 消息广播", False, str(e))
        
        return False
    
    def print_summary(self) -> bool:
        """打印测试摘要"""
        print_header("测试结果摘要")
        
        total = len(self.test_results)
        passed = sum(1 for r in self.test_results if r["passed"])
        failed = total - passed
        success_rate = (passed / total * 100) if total > 0 else 0
        
        print(f"总测试数: {total}")
        print(f"{Colors.GREEN}通过: {passed}{Colors.END}")
        print(f"{Colors.RED}失败: {failed}{Colors.END}")
        print(f"成功率: {success_rate:.1f}%\n")
        
        if failed > 0:
            print(f"{Colors.RED}{Colors.BOLD}失败的测试：{Colors.END}")
            for result in self.test_results:
                if not result["passed"]:
                    print(f"  ❌ {result['test']}")
                    if result["details"]:
                        print(f"     {result['details']}")
            print()
        
        # 总体结果
        if failed == 0:
            print(f"{Colors.GREEN}{Colors.BOLD}🎉 所有测试通过！{Colors.END}\n")
            return True
        else:
            print(f"{Colors.RED}{Colors.BOLD}⚠️  部分测试失败，请检查{Colors.END}\n")
            return False

async def main() -> int:
    """主测试流程"""
    tester = APITester()
    
    print_header("实时问答系统 API 自动化测试")
    print(f"测试目标: {BASE_URL}")
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    
    # 检查服务器连接
    print_info("检查服务器连接...")
    try:
        response = requests.get(f"{BASE_URL}/", timeout=5)
        if response.status_code == 200:
            print_success("服务器连接正常")
        else:
            print_error(f"服务器返回异常状态码: {response.status_code}")
            return
    except Exception as e:
        print_error(f"无法连接到服务器: {e}")
        print_info("请确保后端服务器正在运行: python main.py")
        return
    
    # ========== 第一部分：用户管理测试 ==========
    print_header("第一部分：用户管理测试")
    
    # 生成唯一的用户名（避免重复注册失败）
    timestamp = int(time.time())
    student_username = f"test_student_{timestamp}"
    tutor_username = f"test_tutor_{timestamp}"
    password = "test123456"
    
    # 注册学生
    tester.test_register_user(student_username, password, "student")
    
    # 注册老师
    tester.test_register_user(tutor_username, password, "tutor")
    
    # 登录学生
    tester.student_token, tester.student_id = tester.test_login_user(student_username, password)
    
    # 登录老师
    tester.tutor_token, tester.tutor_id = tester.test_login_user(tutor_username, password)
    
    if not tester.student_token or not tester.tutor_token:
        print_error("用户登录失败，终止测试")
        tester.print_summary()
        return
    
    # ========== 第二部分：问题管理测试 ==========
    print_header("第二部分：问题管理测试")
    
    # 创建问题
    tester.test_question_id = tester.test_create_question(
        tester.student_token,
        "测试问题：如何修复同步机制的bug？"
    )
    
    # 获取问题列表（学生）- 默认分页
    tester.test_get_questions(tester.student_token)
    
    # 获取问题列表（学生）- 自定义分页
    tester.test_get_questions(tester.student_token, page=1, page_size=10)
    
    # 获取问题列表（老师）
    tester.test_get_questions(tester.tutor_token)
    
    if not tester.test_question_id:
        print_error("创建问题失败，跳过后续测试")
    else:
        # 老师接受问题
        tester.test_accept_question(tester.tutor_token, tester.test_question_id)
        
        # 等待一下，让状态更新
        time.sleep(1)
        
        # ========== 第三部分：消息管理测试 ==========
        print_header("第三部分：消息管理测试")
        
        # 学生发送消息
        tester.test_send_message(
            tester.student_token,
            tester.test_question_id,
            "你好老师，我遇到了同步问题"
        )
        
        # 老师发送消息
        tester.test_send_message(
            tester.tutor_token,
            tester.test_question_id,
            "你好，请详细描述一下问题"
        )
        
        # 获取消息列表（关闭前）- 默认分页
        tester.test_get_messages(tester.student_token, tester.test_question_id)
        
        # 获取消息列表（关闭前）- 自定义分页
        tester.test_get_messages(tester.student_token, tester.test_question_id, page=1, page_size=5)
        
        # 关闭问题
        tester.test_close_question(tester.student_token, tester.test_question_id)
        
        # 等待一下，让状态更新
        time.sleep(1)
        
        # 再次获取消息列表（关闭后）- 验证消息没有丢失
        tester.test_get_messages(tester.student_token, tester.test_question_id)
    
    # ========== 第四部分：WebSocket 测试 ==========
    print_header("第四部分：WebSocket 测试")
    
    # 测试学生 WebSocket 连接
    await tester.test_websocket_connection(tester.student_id, "student")
    
    # 测试老师 WebSocket 连接
    await tester.test_websocket_connection(tester.tutor_id, "tutor")
    
    # 测试消息广播
    await tester.test_websocket_message_broadcast()
    
    # ========== 测试总结 ==========
    all_passed = tester.print_summary()
    
    # 返回退出码
    return 0 if all_passed else 1

if __name__ == "__main__":
    try:
        exit_code = asyncio.run(main())
        exit(exit_code)
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}测试被用户中断{Colors.END}")
        exit(1)
    except Exception as e:
        print(f"\n{Colors.RED}测试发生异常: {e}{Colors.END}")
        import traceback
        traceback.print_exc()
        exit(1)

