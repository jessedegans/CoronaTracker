package com.deGans.coronaTracker.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.deGans.coronaTracker.Models.LocationDao;
import com.deGans.coronaTracker.Models.LocationDto;

@Database(entities = {LocationDto.class}, version = 2,exportSchema=false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
}