package com.example.tool.wallpaper.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lib.mvvm.BaseActivity
import com.example.tool.databinding.ActivityWallpaperBinding
import com.example.tool.wallpaper.adapter.WallpaperAdapter
import com.example.tool.wallpaper.model.BingResponse
import com.example.tool.wallpaper.network.Api
import com.example.tool.wallpaper.network.NetworkManager
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class WallpaperActivity : BaseActivity<ActivityWallpaperBinding>() {
    private val wallpaperList = mutableListOf<JsonObject>()
    private lateinit var adapter: WallpaperAdapter
    private var currentOffset = 0
    private val pageSize = 10
    private var isLoading = false


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = WallpaperAdapter(wallpaperList)

        binding.rvWallpaper.adapter = adapter
        binding.rvWallpaper.layoutManager = LinearLayoutManager(this)

        loadNextPage()
        binding.rvWallpaper.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                if (lastVisible >= wallpaperList.size - 3) {
                    loadNextPage()
                }
            }
        })
    }

    private fun loadNextPage() {
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch {
            getWallPaperFlow(currentOffset, pageSize)
                .collect { result ->
                    val images = result.images
                    if (images.size() == 0) return@collect

                    images.forEach {
                        wallpaperList.add(it.asJsonObject)
                    }

                    adapter.notifyItemRangeInserted(
                        wallpaperList.size - images.size(),
                        images.size()
                    )

                    currentOffset += pageSize
                    isLoading = false
                }
        }
    }

    fun getWallPaperFlow(dayBefore: Int, size: Int): Flow<BingResponse> = flow {
        emit(NetworkManager.create(Api::class.java).getWallPaper(dayBefore, size))
    }.flowOn(Dispatchers.IO)
}