package com.deGans.coronaTracker.Models;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {
    @Query("SELECT * FROM locations")
    List<LocationDto> getAll();

//    @Query("SELECT * FROM locations WHERE locations IN (:userIds)")
//    List<User> loadAllByIds(int[] userIds);

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    User findByName(String first, String last);

//    @Insert
//    void insertAll(LocationDto... locationDtos);
    @Insert
    public void insert(LocationDto locDto);

    @Delete
    void delete(LocationDto locationDto);
}