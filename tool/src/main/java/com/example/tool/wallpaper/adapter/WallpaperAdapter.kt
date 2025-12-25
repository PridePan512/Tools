package com.example.tool.wallpaper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tool.R
import com.google.gson.JsonObject

class WallpaperAdapter(
    private val data: MutableList<JsonObject>
) : RecyclerView.Adapter<WallpaperAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.ivWallpaper)
        val tvCopyright: TextView = view.findViewById(R.id.tvCopyright)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallpaper, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val obj = data[position]

        // 图片
        val url = obj.get("url").asString
        val fullUrl = "https://www.bing.com$url"

        Glide.with(holder.iv)
            .load(fullUrl)
            .into(holder.iv)

        // 左下角版权
        holder.tvCopyright.text =
            (obj.get("copyright")?.asString ?: "")
                .replaceFirst(Regex("[,，]"), "\n")

    }

    override fun getItemCount() = data.size
}

