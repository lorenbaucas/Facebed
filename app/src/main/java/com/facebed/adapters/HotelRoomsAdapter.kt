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
import com.facebed.activities.AddRoomActivity
import com.facebed.models.SimpleRoom
import de.hdodenhof.circleimageview.CircleImageView

class HotelRoomsAdapter(private var rooms: MutableList<SimpleRoom>) : RecyclerView.Adapter<HotelRoomsAdapter.RoomViewHolder>() {

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelName: TextView = itemView.findViewById(R.id.primary_text)
        val location: TextView = itemView.findViewById(R.id.secondary_text)
        val imageView: CircleImageView = itemView.findViewById(R.id.image)
        val editIcon: ImageButton = itemView.findViewById(R.id.edit_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_company_data, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        //Lista de las habitaciones en el hotel de la empresa
        val room = rooms[position]
        holder.hotelName.text = room.name
        holder.location.text = room.number
        Glide.with(holder.itemView.context)
            .load(room.imageUri)
            .into(holder.imageView)

        holder.editIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, AddRoomActivity::class.java)
            intent.putExtra("roomName", room.name)
            intent.putExtra("number", room.number)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rooms.size
}