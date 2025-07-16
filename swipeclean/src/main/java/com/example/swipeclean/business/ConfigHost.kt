package com.example.swipeclean.business

import android.content.Context
import androidx.core.content.edit

object ConfigHost {

    private const val CONFIG_FILE_NAME = "swipe_clean"
    private const val KEY_CLEANED_SIZE = "cleaned_size"
    private const val KEY_ALBUM_SORT = "album_sort"

    fun getCleanedSize(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(KEY_CLEANED_SIZE, 0)
    }

    fun setCleanedSize(size: Long, context: Context) {
        context.getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_CLEANED_SIZE, getCleanedSize(context) + size)
        }
    }

    fun getSortType(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_ALBUM_SORT, 0)
    }

    fun setSortType(sortType: Int, context: Context) {
        context.getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_ALBUM_SORT, sortType)
            commit()
        }
    }

}