package com.facebed.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.activities.BookActivity
import com.facebed.activities.FullScreenImageActivity
import com.facebed.controllers.FirebaseController
import com.facebed.controllers.Utils
import com.facebed.models.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RoomAdapter(private var rooms: MutableList<Room>) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roomName: TextView = itemView.findViewById(R.id.room_name_text)
        val roomData: TextView = itemView.findViewById(R.id.data_room_text)
        val rvRoomImages: RecyclerView = itemView.findViewById(R.id.rv_room_images)
        val rvRoomServices: RecyclerView = itemView.findViewById(R.id.rv_room_services)
        val bookButton: Button = itemView.findViewById(R.id.book_button)

        init {
            rvRoomImages.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            rvRoomServices.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]

        holder.roomName.text = room.name
        holder.roomData.text = "${room.price}€ / ${room.maxPeople}"

        val imageUris: MutableList<Uri> = mutableListOf()
        val servicesList: MutableList<String> = mutableListOf()
        imageUris.clear()
        servicesList.clear()

        val roomsCollectionRef = FirebaseFirestore.getInstance().collection("Rooms")
        roomsCollectionRef
            .whereEqualTo("hotelId", room.hotelId)
            .whereEqualTo("number", room.number)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]

                    val hotelId = room.hotelId
                    val userUid = documentSnapshot.getString("userUid")
                    val number = documentSnapshot.getString("number")

                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("HotelsData/$userUid/$hotelId/${documentSnapshot.id}")

                    storageRef.listAll().addOnSuccessListener { listResult ->
                        listResult.items.forEachIndexed { index, item ->
                            item.downloadUrl.addOnSuccessListener { uri ->
                                imageUris.add(uri)

                                if (index == listResult.items.size - 1) {
                                    imageUris.sortBy { it.toString() }
                                    val imageAdapter = ImagesAdapter(imageUris) { uri ->
                                        val context = holder.itemView.context
                                        val intent = Intent(context, FullScreenImageActivity::class.java).apply {
                                            putExtra("uri", uri.toString())
                                        }
                                        context.startActivity(intent)
                                    }
                                    holder.rvRoomImages.adapter = imageAdapter
                                }
                            }
                        }
                    }

                    val serviceKeys = Utils.getRoomServiceKeys()

                    FirebaseController.getRoomServices(hotelId, number!!) { documentSnapshot ->
                        documentSnapshot?.data?.forEach { (key, value) ->
                            if (key != "hotelId" && key != "number" && key != "userUid"
                                && value is Boolean && value) {
                                val serviceKey = serviceKeys[key]
                                if (serviceKey != null) {
                                    val serviceName = holder.itemView.context.getString(serviceKey)
                                    servicesList.add(serviceName)
                                }
                            }
                        }

                        val servicesAdapter = ServicesAdapter(servicesList) {}
                        holder.rvRoomServices.adapter = servicesAdapter
                    }
                }
            }

        holder.bookButton.setOnClickListener {
            val intent = Intent(holder.itemView.context, BookActivity::class.java)
            intent.putExtra("hotelId", room.hotelId)
            intent.putExtra("hotelName", room.hotelName)
            intent.putExtra("roomName", room.name)
            intent.putExtra("roomId", room.number)
            intent.putExtra("price", room.price)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rooms.size
}