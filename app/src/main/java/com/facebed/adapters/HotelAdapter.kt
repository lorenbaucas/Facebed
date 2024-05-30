package com.facebed.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebed.R
import com.facebed.activities.HotelViewActivity
import com.facebed.controllers.FirebaseController
import com.facebed.controllers.Utils
import com.facebed.models.Hotel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class HotelAdapter(private var hotels: MutableList<Hotel>) : RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {

    inner class HotelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelName: TextView = itemView.findViewById(R.id.hotel_name_text)
        val location: TextView = itemView.findViewById(R.id.location_text)
        val hotelImage: CircleImageView = itemView.findViewById(R.id.hotel_image)
        val starsNumber: TextView = itemView.findViewById(R.id.stars_number_text)
        val rvHotelImages: RecyclerView = itemView.findViewById(R.id.rv_hotel_images)
        val rvHotelServices: RecyclerView = itemView.findViewById(R.id.rv_hotel_services)

        init {
            rvHotelImages.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            rvHotelServices.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hotel, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        val hotel = hotels[position]

        val imageUris: MutableList<Uri> = mutableListOf()
        val servicesList: MutableList<String> = mutableListOf()
        imageUris.clear()
        servicesList.clear()

        val hotelsCollectionRef = FirebaseFirestore.getInstance().collection("Hotels")
        hotelsCollectionRef
            .whereEqualTo("hotelName", hotel.name)
            .whereEqualTo("location", hotel.location)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]
                    val hotelId = documentSnapshot.id

                    val userUid = documentSnapshot.getString("userUid")
                    val hotelName = documentSnapshot.getString("hotelName")
                    val location = documentSnapshot.getString("location")
                    val stars = documentSnapshot.getLong("stars")

                    holder.starsNumber.text = stars.toString()
                    holder.hotelName.text = hotelName
                    holder.location.text = location
                    Glide.with(holder.itemView.context)
                        .load(hotel.imageUri)
                        .into(holder.hotelImage)

                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("HotelsData/$userUid/$hotelId/MainPhotos")

                    storageRef.listAll().addOnSuccessListener { listResult ->
                        listResult.items.forEachIndexed { index, item ->
                            item.downloadUrl.addOnSuccessListener { uri ->
                                imageUris.add(uri)

                                if (index == listResult.items.size - 1) {
                                    imageUris.sortBy { it.toString() }
                                    val imageAdapter = ImagesAdapter(imageUris) {
                                        handleClick(holder.itemView.context, hotelId)
                                    }
                                    holder.rvHotelImages.adapter = imageAdapter
                                }
                            }
                        }
                    }

                    val serviceKeys = Utils.getHotelServiceKeys()

                    FirebaseController.getHotelServices(userUid!!, hotelId) { documentSnapshot ->
                        documentSnapshot?.data?.forEach { (key, value) ->
                            if (key != "userUid" && key != "hotelId"
                                && value is Boolean && value) {
                                val serviceKey = serviceKeys[key]
                                if (serviceKey != null) {
                                    val serviceName = holder.itemView.context.getString(serviceKey)
                                    servicesList.add(serviceName)
                                }
                            }
                        }

                        val servicesAdapter = ServicesAdapter(servicesList) {
                            handleClick(holder.itemView.context, hotelId)
                        }
                        holder.rvHotelServices.adapter = servicesAdapter

                        holder.itemView.setOnClickListener {
                            handleClick(holder.itemView.context, hotelId)
                        }
                    }
                }
            }
    }

    override fun getItemCount(): Int = hotels.size

    private fun handleClick(context: Context, id: String) {
        val intent = Intent(context, HotelViewActivity::class.java)
        intent.putExtra("hotelId", id)
        context.startActivity(intent)
    }
}