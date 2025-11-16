package com.dowdah.asknow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.dowdah.asknow.data.local.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Query("SELECT * FROM messages WHERE questionId = :questionId ORDER BY createdAt ASC")
    LiveData<List<MessageEntity>> getMessagesByQuestionId(long questionId);

    @Query("SELECT * FROM messages WHERE questionId = :questionId ORDER BY createdAt ASC")
    List<MessageEntity> getMessagesByQuestionIdSync(long questionId);

    @Query("SELECT * FROM messages WHERE id = :messageId")
    MessageEntity getMessageById(long messageId);

    @Query("DELETE FROM messages WHERE questionId = :questionId")
    void deleteMessagesByQuestionId(long questionId);
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    void deleteMessage(long messageId);

    @Query("DELETE FROM messages")
    void deleteAll();
    
    // 已读功能相关方法
    @Query("SELECT COUNT(*) FROM messages WHERE questionId = :questionId AND isRead = 0 AND senderId != :currentUserId")
    int getUnreadMessageCount(long questionId, long currentUserId);
    
    @Query("SELECT COUNT(*) FROM messages WHERE questionId = :questionId AND isRead = 0 AND senderId != :currentUserId")
    LiveData<Integer> getUnreadMessageCountLive(long questionId, long currentUserId);
    
    @Query("UPDATE messages SET isRead = 1 WHERE questionId = :questionId AND senderId != :currentUserId")
    void markMessagesAsRead(long questionId, long currentUserId);
    
    @Query("UPDATE messages SET isRead = :isRead WHERE id = :messageId")
    void updateMessageReadStatus(long messageId, boolean isRead);
    
    // 乐观更新相关方法
    @Query("UPDATE messages SET sendStatus = :status WHERE id = :messageId")
    void updateSendStatus(long messageId, String status);
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    void deleteById(long messageId);
}

