package com.dowdah.asknow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.dowdah.asknow.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);
    
    @Update
    void update(UserEntity user);
    
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity getUserByUsername(String username);
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getUserById(long id);
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    LiveData<UserEntity> getUserByIdLive(long id);
    
    @Query("SELECT * FROM users")
    List<UserEntity> getAllUsers();
    
    @Query("DELETE FROM users WHERE id = :id")
    void deleteUser(long id);
    
    @Query("DELETE FROM users")
    void deleteAllUsers();
}

