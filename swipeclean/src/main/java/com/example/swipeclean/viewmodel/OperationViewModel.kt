package com.example.swipeclean.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipeclean.activity.OperationActivity.Companion.PHOTO_OPERATION_CANCEL
import com.example.swipeclean.activity.OperationActivity.Companion.PHOTO_OPERATION_DELETE
import com.example.swipeclean.activity.OperationActivity.Companion.PHOTO_OPERATION_KEEP
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OperationViewModel : ViewModel() {

    private var mAlbum: Album? = null

    fun initAlbum(id: Long) {
        mAlbum = AlbumController.getAlbums().find { item ->
            item.getId() == id
        }
    }

    fun getAlbum(): Album? {
        return mAlbum
    }

    fun doOnCompleted(operationType: Int){
        var image: Image? = null
        mAlbum?.apply {
            image = if (operationType == PHOTO_OPERATION_CANCEL) images[getOperatedIndex() - 1] else images[getOperatedIndex()]
        }

        image?.let {
            when (operationType) {
                PHOTO_OPERATION_CANCEL -> {
                    image.cancelOperated()
                    viewModelScope.launch(Dispatchers.IO) {
                        AlbumController.cleanCompletedImage(image)
                    }
                }

                PHOTO_OPERATION_KEEP -> {
                    image.doKeep()
                    viewModelScope.launch(Dispatchers.IO) {
                        AlbumController.addImage(image)
                    }
                }

                PHOTO_OPERATION_DELETE -> {
                    image.doDelete()
                    viewModelScope.launch(Dispatchers.IO) {
                        AlbumController.addImage(image)
                    }
                }
            }
        }
    }
}