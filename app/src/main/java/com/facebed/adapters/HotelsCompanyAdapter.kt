package com.facebed.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebed.R
import com.facebed.activities.AddHotelActivity
import com.facebed.activities.HotelRoomsActivity
import com.facebed.activities.ReviewsActivity
import com.facebed.models.Hotel
import de.hdodenhof.circleimageview.CircleImageView

class HotelsCompanyAdapter(private var hotels: MutableList<Hotel>) : RecyclerView.Adapter<HotelsCompanyAdapter.HotelViewHolder>() {

    class HotelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelName: TextView = itemView.findViewById(R.id.primary_text)
        val location: TextView = itemView.findViewById(R.id.secondary_text)
        val imageView: CircleImageView = itemView.findViewById(R.id.image)
        val editIcon: ImageButton = itemView.findViewById(R.id.edit_icon)
        val reviewIcon: ImageButton = itemView.findViewById(R.id.review_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_company_data, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        //Lista de hoteles en la empresa
        val hotel = hotels[position]
        holder.hotelName.text = hotel.name
        holder.location.text = hotel.location
        holder.reviewIcon.visibility = View.VISIBLE
        Glide.with(holder.itemView.context)
            .load(hotel.imageUri)
            .into(holder.imageView)

        //Para editar el hotel
        holder.editIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, AddHotelActivity::class.java)
            intent.putExtra("hotelName", hotel.name)
            holder.itemView.context.startActivity(intent)
        }

        //Para que nos muestre las habitaciones asociadas
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, HotelRoomsActivity::class.java)
                .putExtra("hotelName", hotel.name)
                .putExtra("hotelId", hotel.hotelId)
            holder.itemView.context.startActivity(intent)
        }

        //Para ver las reseñas de ese hotel
        holder.reviewIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, ReviewsActivity::class.java)
                .putExtra("hotelId", hotel.hotelId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = hotels.size
}