package com.example.swipeclean.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.swipeclean.model.Image
import com.example.tools.R
import com.example.tools.databinding.GridItemRecyclebinBinding

class RecyclerBinAdapter(
    val images: MutableList<Image>,
    val onItemRestoreClick: (image: Image, position: Int) -> Unit,
    val onItemClick: (photoImageView: ImageView, image: Image, position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerBinAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        return MyViewHolder(GridItemRecyclebinBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val image = images[position]

        Glide
            .with(holder.binding.root.context)
            .load(image.sourceUri)
            .placeholder(R.drawable.ic_vector_image)
            .into(holder.binding.ivCover)

        holder.binding.ivKeep.setOnClickListener {
            onItemRestoreClick.invoke(image, holder.bindingAdapterPosition)
        }
        holder.binding.root.setOnClickListener {
            onItemClick.invoke(holder.binding.ivCover, image, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun removeImage(image: Image) {
        images.remove(image)
    }

    fun getTotalSize(): Long {
        return images.sumOf { item -> item.size }
    }

    class MyViewHolder(val binding: GridItemRecyclebinBinding) : RecyclerView.ViewHolder(binding.root)
}