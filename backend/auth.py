import jwt
import time
from passlib.context import CryptContext
from typing import Optional, Dict, Any
import config

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    """
    对密码进行哈希加密
    
    Args:
        password: 明文密码
        
    Returns:
        str: 哈希后的密码
    """
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    验证密码是否正确
    
    Args:
        plain_password: 明文密码
        hashed_password: 哈希后的密码
        
    Returns:
        bool: 密码是否匹配
    """
    return pwd_context.verify(plain_password, hashed_password)


def create_access_token(data: Dict[str, Any]) -> str:
    """
    创建JWT访问令牌
    
    Args:
        data: 要编码到令牌中的数据字典
        
    Returns:
        str: JWT令牌字符串
    """
    to_encode = data.copy()
    expire = int(time.time()) + config.ACCESS_TOKEN_EXPIRE_SECONDS
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, config.SECRET_KEY, algorithm=config.ALGORITHM)
    return encoded_jwt


def decode_access_token(token: str) -> Optional[Dict[str, Any]]:
    """
    解码JWT访问令牌
    
    Args:
        token: JWT令牌字符串
        
    Returns:
        Optional[Dict[str, Any]]: 解码后的数据字典，解码失败返回None
    """
    try:
        payload = jwt.decode(token, config.SECRET_KEY, algorithms=[config.ALGORITHM])
        return payload
    except jwt.PyJWTError:
        return None

