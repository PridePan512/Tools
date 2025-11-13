package com.example.swipeclean.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.swipeclean.adapter.AlbumAdapter.MyViewHolder
import com.example.swipeclean.model.Album
import com.example.tools.R
import com.example.tools.databinding.ListItemAlbumBinding

class AlbumAdapter(
    val albums: ArrayList<Album>,
    val onItemClick: (albumId: Long, albumFormatDate: String, completed: Boolean) -> Unit
) : RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        return MyViewHolder(
            ListItemAlbumBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val album = albums[position]
        Glide
            .with(holder.binding.root.context)
            .load(album.getCoverUri())
            .placeholder(R.drawable.ic_vector_image)
            .into(holder.binding.ivCover)

        val totalCount = album.getTotalCount()
        val completedCunt = album.getCompletedCount()
        val operatedCount = album.getOperatedIndex()

        holder.binding.tvDate.text = album.formatData
        holder.binding.tvProgress.text = holder.binding.root.context.resources.getQuantityString(
            R.plurals.picture_progress,
            totalCount,
            completedCunt,
            totalCount
        )
        holder.binding.lpiClean.setProgress(100 * operatedCount / totalCount)
        holder.binding.lpiClean.setSecondaryProgress(100 * completedCunt / totalCount)

        if (album.isCompleted()) {
            holder.binding.ivCompleted.visibility = View.VISIBLE
            holder.binding.vCompleted.visibility = View.VISIBLE
            holder.binding.tvDate.setTextColor(
                ContextCompat.getColor(
                    holder.binding.root.context,
                    com.example.lib.R.color.text_sub
                )
            )
            holder.binding.tvDate.paintFlags =
                holder.binding.tvDate.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.root.setOnClickListener {
                onItemClick.invoke(album.getId(), album.formatData, true)
            }

        } else {
            holder.binding.ivCompleted.visibility = View.GONE
            holder.binding.vCompleted.visibility = View.GONE
            holder.binding.tvDate.setTextColor(
                ContextCompat.getColor(
                    holder.binding.root.context,
                    com.example.lib.R.color.text_main
                )
            )
            holder.binding.tvDate.paintFlags =
                holder.binding.tvDate.paintFlags and (Paint.STRIKE_THRU_TEXT_FLAG.inv())
            holder.binding.root.setOnClickListener {
                onItemClick.invoke(album.getId(), album.formatData, false)
            }
        }
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    override fun getItemId(position: Int): Long {
        return albums[position].getId()
    }

    class MyViewHolder(val binding: ListItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)
}