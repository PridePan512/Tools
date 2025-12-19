package com.example.swipeclean.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipeclean.adapter.RecyclerBinAdapter
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.business.ConfigHost
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Image
import com.example.swipeclean.other.Constants.MIN_SHOW_LOADING_TIME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecyclerBinViewModel : ViewModel() {

    private val _cleanCompletedLiveData = MutableLiveData<Boolean>()
    val cleanCompletedLiveData = _cleanCompletedLiveData
    private val _restoreImageLiveData = MutableLiveData<Boolean>()
    val restoreImageLiveData = _restoreImageLiveData
    private var mAlbum: Album? = null

    fun initAlbum(id: Long) {
        mAlbum = AlbumController.getAlbums().find { item ->
            item.getId() == id
        }
    }

    fun getAlbum(): Album? {
        return mAlbum
    }

    fun cleanCompletedImages(mAdapter: RecyclerBinAdapter, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigHost.setCleanedSize(
                mAdapter.getTotalSize(),
                context
            )

            AlbumController.cleanCompletedImage(mAdapter.images)
            mAlbum?.images?.removeAll(mAdapter.images)

            delay(MIN_SHOW_LOADING_TIME)

            _cleanCompletedLiveData.postValue(true)
        }
    }

    fun cleanCompletedImagesUseOld(mAdapter: RecyclerBinAdapter, context: Context, contentResolver: android.content.ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigHost.setCleanedSize(
                mAdapter.getTotalSize(),
                context
            )

            mAdapter.images.let { deletePhotos ->
                AlbumController.cleanCompletedImage(deletePhotos)
                mAlbum?.images?.removeAll(deletePhotos)

                deletePhotos
                    .mapNotNull { it.sourceUri }
                    .forEach { contentResolver.delete(it, null, null) }
            }
            delay(MIN_SHOW_LOADING_TIME)
        }
    }

    fun converseDeleteToKeepImage(image: Image) {
        viewModelScope.launch(Dispatchers.IO) {
            AlbumController.converseDeleteToKeepImage(image)
        }
    }

    fun converseDeleteToKeepImage(images: List<Image>) {
        viewModelScope.launch(Dispatchers.IO) {
            AlbumController.converseDeleteToKeepImage(images)
            _restoreImageLiveData.postValue(true)
        }
    }
}