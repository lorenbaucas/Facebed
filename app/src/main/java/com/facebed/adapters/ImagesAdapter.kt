package com.facebed.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.facebed.R

class ImagesAdapter(private val imageUris: MutableList<Uri>) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val deleteIcon: ImageButton = view.findViewById(R.id.delete_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = imageUris[position]
        val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(30))
        Glide.with(holder.imageView.context)
            .load(uri)
            .apply(requestOptions)
            .into(holder.imageView)

        holder.deleteIcon.setOnClickListener {
            imageUris.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = imageUris.size
}