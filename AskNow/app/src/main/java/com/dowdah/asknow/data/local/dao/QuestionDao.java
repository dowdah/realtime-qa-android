package com.dowdah.asknow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.dowdah.asknow.data.local.entity.QuestionEntity;

import java.util.List;

@Dao
public interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(QuestionEntity question);
    
    @Update
    void update(QuestionEntity question);
    
    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    QuestionEntity getQuestionById(long id);
    
    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    LiveData<QuestionEntity> getQuestionByIdLive(long id);
    
    @Query("SELECT * FROM questions WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<QuestionEntity>> getQuestionsByUserId(long userId);
    
    @Query("SELECT * FROM questions WHERE userId = :userId ORDER BY createdAt DESC")
    List<QuestionEntity> getQuestionsByUserIdSync(long userId);
    
    @Query("SELECT * FROM questions WHERE status = :status ORDER BY createdAt DESC")
    LiveData<List<QuestionEntity>> getQuestionsByStatus(String status);
    
    @Query("SELECT * FROM questions WHERE tutorId = :tutorId AND status = :status ORDER BY updatedAt DESC")
    LiveData<List<QuestionEntity>> getQuestionsByTutorAndStatus(long tutorId, String status);
    
    @Query("SELECT * FROM questions WHERE tutorId = :tutorId ORDER BY updatedAt DESC")
    List<QuestionEntity> getQuestionsByTutorId(long tutorId);
    
    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    LiveData<List<QuestionEntity>> getAllQuestions();
    
    @Query("UPDATE questions SET status = :status WHERE id = :id")
    void updateQuestionStatus(long id, String status);
    
    @Query("UPDATE questions SET updatedAt = :updatedAt WHERE id = :id")
    void updateUpdatedAt(long id, long updatedAt);
    
    @Query("DELETE FROM questions WHERE id = :id")
    void deleteQuestion(long id);
    
    @Query("DELETE FROM questions")
    void deleteAllQuestions();
}

