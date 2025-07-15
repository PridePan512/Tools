package com.example.swipeclean.business

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.swipeclean.model.Photo

@Database(entities = [Photo::class], version = 1, exportSchema = false)
abstract class PhotoDataBase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
}