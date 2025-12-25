package com.example.tool.wallpaper.network

import com.example.tool.wallpaper.model.BingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {
    @GET("HPImageArchive.aspx?format=js&mkt=zh-CN")
    suspend fun getWallPaper(@Query("idx") idx: Int, @Query("n") n: Int): BingResponse
}