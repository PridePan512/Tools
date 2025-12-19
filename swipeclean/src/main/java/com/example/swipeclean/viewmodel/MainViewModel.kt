package com.example.swipeclean.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.dialog.SortDialogFragment
import com.example.swipeclean.model.Album
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _albumsLiveData = MutableLiveData<ArrayList<Album>>()
    val albumsLiveData = _albumsLiveData
    private val _reCleanAlbumsLiveData = MutableLiveData<Int>()
    val reCleanAlbumsLiveData = _reCleanAlbumsLiveData

    /**
     * 同步数据库
     */
    fun syncDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            AlbumController.syncDatabase()
        }
    }

    fun loadAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            val albums = AlbumController.loadAlbums()
            _albumsLiveData.postValue(albums)
        }
    }

    fun reCleanAlbums(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            album.images.let { photos ->
                AlbumController.cleanCompletedImage(
                    photos
                )
                photos.forEach { it.cancelOperated() }
            }
            _reCleanAlbumsLiveData.postValue(0)
        }
    }

    fun isAlbumOperated(albumId: Long): Boolean {
        val album = AlbumController.getAlbums()
            .find { it.getId() == albumId }

        return album?.images.isNullOrEmpty() || album.isOperated()
    }

    fun sortAlbums(albums: ArrayList<Album>,sortType: Int): ArrayList<Album> {
        when (sortType) {
            SortDialogFragment.DATE_DOWN -> {
                albums.sortByDescending(Album::getDateTime)
            }

            SortDialogFragment.DATE_UP -> {
                albums.sortBy(Album::getDateTime)
            }

            SortDialogFragment.SIZE_DOWN -> {
                albums.sortByDescending(Album::getTotalCount)
            }

            SortDialogFragment.SIZE_UP -> {
                albums.sortBy(Album::getTotalCount)
            }

            SortDialogFragment.UNFINISHED_DOWN -> {
                albums.sortBy(Album::isOperated)
            }

            SortDialogFragment.UNFINISHED_UP -> {
                albums.sortByDescending(Album::isOperated)
            }
        }
        return albums
    }
}