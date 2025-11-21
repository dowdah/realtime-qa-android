from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, AsyncEngine
from sqlalchemy.orm import sessionmaker
from typing import AsyncGenerator
from models import Base
import config

engine: AsyncEngine = create_async_engine(config.DATABASE_URL, echo=config.DATABASE_ECHO)

AsyncSessionLocal = sessionmaker(
    engine, class_=AsyncSession, expire_on_commit=False
)


async def init_db() -> None:
    """
    初始化数据库，创建所有表
    
    Returns:
        None
    """
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """
    获取数据库会话（依赖注入）
    
    Yields:
        AsyncSession: 数据库会话对象
    """
    async with AsyncSessionLocal() as session:
        yield session

