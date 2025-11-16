package com.dowdah.asknow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.dowdah.asknow.data.local.entity.PendingMessageEntity;

import java.util.List;

@Dao
public interface PendingMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PendingMessageEntity message);
    
    @Update
    void update(PendingMessageEntity message);
    
    @Query("SELECT * FROM pending_messages WHERE id = :id LIMIT 1")
    PendingMessageEntity getMessageById(long id);
    
    @Query("SELECT * FROM pending_messages WHERE messageId = :messageId LIMIT 1")
    PendingMessageEntity getMessageByMessageId(String messageId);
    
    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    List<PendingMessageEntity> getAllPendingMessages();
    
    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    LiveData<List<PendingMessageEntity>> getAllPendingMessagesLive();
    
    @Query("UPDATE pending_messages SET retryCount = :retryCount WHERE id = :id")
    void updateRetryCount(long id, int retryCount);
    
    @Query("DELETE FROM pending_messages WHERE id = :id")
    void deleteMessage(long id);
    
    @Query("DELETE FROM pending_messages WHERE messageId = :messageId")
    void deleteMessageByMessageId(String messageId);
    
    @Query("DELETE FROM pending_messages")
    void deleteAllMessages();
}

