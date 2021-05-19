package com.example.cameraapp.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cameraapp.R
import com.example.cameraapp.fragment.CameraFragmentDirections
import com.example.cameraapp.fragment.GalleryFragmentDirections
import com.example.cameraapp.model.row
import java.io.File

class StaggeredRecyclerAdapter(context: Context, listItem: MutableList<File>): RecyclerView.Adapter<StaggeredRecyclerAdapter.ImageViewHolder>() {

    var context:Context
    var listItem:MutableList<File>

    init {
        this.context = context
        this.listItem = listItem
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StaggeredRecyclerAdapter.ImageViewHolder {
     val view = LayoutInflater.from(context).inflate(R.layout.list_item_image,parent,false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: StaggeredRecyclerAdapter.ImageViewHolder, position: Int) {

        Glide.with(this.context)
            .load(listItem.get(position))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.img)

        holder.itemView.setOnClickListener{
            Navigation.findNavController(context as Activity, R.id.fragment_container).navigate(
               GalleryFragmentDirections.actionGalleryToPhoto(listItem.get(position).toString()))
        }
    }

    override fun getItemCount(): Int {
        return listItem.size

    }

    class ImageViewHolder(itemView:View): RecyclerView.ViewHolder(itemView) {
        val img: ImageView

        init {
            img = itemView.findViewById(R.id.row_img)
        }
    }
}