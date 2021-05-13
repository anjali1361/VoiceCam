package com.example.cameraapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.blackbox.imageutils.models.ImageItem
import com.bumptech.glide.Glide
import com.example.cameraapp.R
import java.io.File

class ImageAdapter(context:Context, item: MutableList<ImageItem>) : RecyclerView.Adapter<ImageAdapter.ViewHolder>(){

    val context:Context
    val item:MutableList<ImageItem>

    init {
        this.context = context
        this.item = item
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.list_item_image, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//
//        val pin = getItem(position)
//
//        holder.apply {
//            bind(pin)
//            itemView.tag = pin
//        }

        val imageItem = item[position]
        holder.apply {
            bind(imageItem)
            itemView.tag=imageItem
        }

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(image: ImageItem) {
            val context = itemView.context
            val imageView:AppCompatImageView = itemView.findViewById(R.id.imageView)

            Glide.with(context)
                .load(image.imagePath)
                .into(imageView)
        }
    }

    override fun getItemCount(): Int {
       return item.size
    }
}