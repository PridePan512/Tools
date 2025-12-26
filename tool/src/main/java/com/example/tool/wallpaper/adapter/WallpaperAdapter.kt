package com.example.tool.wallpaper.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tool.databinding.ItemWallpaperBinding
import com.google.gson.JsonObject

class WallpaperAdapter(
    private val data: MutableList<JsonObject>
) : RecyclerView.Adapter<WallpaperAdapter.VH>() {

    private val baseUrl = "https://www.bing.com"

    class VH(val binding: ItemWallpaperBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemWallpaperBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val obj = data[position]

        Glide.with(holder.binding.root.context)
            .load(baseUrl + obj.get("url").asString)
            .into(holder.binding.ivWallpaper)

        obj.get("copyright")?.asString?.let {
            val title = it.substringBefore(",")
                .takeIf { part -> part != it }
                ?: it.substringBefore("，")
            holder.binding.tvTitle.text = title.trim()

            val copyRight = it.substringAfter(",")
                .takeIf { part -> part != it }
                ?: it.substringAfter("，")
            holder.binding.tvCopyright.text = copyRight.trim()
        }

    }

    override fun getItemCount() = data.size
}

