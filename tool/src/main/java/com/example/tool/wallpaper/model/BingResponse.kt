package com.example.tool.wallpaper.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject


data class BingResponse(
    val images: JsonArray,
    val tooltips: JsonObject
)
