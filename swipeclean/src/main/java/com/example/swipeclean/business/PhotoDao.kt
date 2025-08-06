package com.example.swipeclean.business

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.swipeclean.model.Photo
import com.example.swipeclean.other.Constants.PHOTO_DELETE
import com.example.swipeclean.other.Constants.PHOTO_KEEP

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhoto(photo: Photo)

    @Query("SELECT sourceId FROM photo WHERE tag = $PHOTO_DELETE")
    fun getDeletePhotoIds(): List<Long>

    @Query("SELECT sourceId FROM photo WHERE tag = $PHOTO_KEEP")
    fun getKeepPhotoIds(): List<Long>

    @Query("DELETE FROM photo WHERE sourceId = :sourceId")
    fun delete(sourceId: Long)

    @Query("DELETE FROM photo WHERE sourceId IN (:sourceIds)")
    fun delete(sourceIds: List<Long>)

    @Query("UPDATE photo SET tag = $PHOTO_KEEP WHERE sourceId = :sourceId")
    fun convertDeleteToKeep(sourceId: Long)

    @Query("UPDATE photo SET tag = $PHOTO_KEEP WHERE sourceId in (:sourceIds)")
    fun convertDeleteToKeep(sourceIds: List<Long>)
}