package com.dowdah.asknow.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.PendingMessageDao;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.dao.UserDao;
import com.dowdah.asknow.data.local.entity.MessageEntity;
import com.dowdah.asknow.data.local.entity.PendingMessageEntity;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.local.entity.UserEntity;

@Database(
    entities = {
        UserEntity.class,
        QuestionEntity.class,
        PendingMessageEntity.class,
        MessageEntity.class
    },
    version = 6,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract QuestionDao questionDao();
    public abstract PendingMessageDao pendingMessageDao();
    public abstract MessageDao messageDao();
}

