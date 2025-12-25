package com.example.tool.wallpaper.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.lib.mvvm.BaseActivity
import com.example.tool.databinding.ActivityWallpaperBinding
import com.example.tool.wallpaper.model.BingResponse
import com.example.tool.wallpaper.network.Api
import com.example.tool.wallpaper.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class WallpaperActivity : BaseActivity<ActivityWallpaperBinding>() {
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            getWallPaperFlow(0, 2)
                .collect { result ->
                    Log.d("test", result.tooltips.toString())
                }
        }
    }

    fun getWallPaperFlow(dayBefore: Int, size: Int): Flow<BingResponse> = flow {
        emit(NetworkManager.create(Api::class.java).getWallPaper(dayBefore, size))
    }.flowOn(Dispatchers.IO)
}