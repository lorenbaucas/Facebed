package com.facebed.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.facebed.R
import com.facebed.activities.HotelViewActivity
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
            rvHotelImages.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            rvHotelServices.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
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

        val firestore = FirebaseFirestore.getInstance()

        val hotelsCollectionRef = firestore.collection("Hotels")
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
                    val description = documentSnapshot.getString("description")
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
                                    val imageAdapter = HotelImagesAdapter(imageUris) {
                                        handleClick(holder.itemView.context, hotel.name)
                                    }
                                    holder.rvHotelImages.adapter = imageAdapter
                                }
                            }
                        }
                    }

                    val serviceKeys = mapOf(
                        "swimming_pool" to R.string.swimming_pool,
                        "restaurant" to R.string.restaurant,
                        "spa" to R.string.spa,
                        "adults_only" to R.string.adults_only,
                        "gym" to R.string.gym,
                        "water_park" to R.string.water_park,
                        "bowling" to R.string.bowling,
                        "padel_courts" to R.string.padel_courts,
                        "seafront" to R.string.seafront,
                        "rural" to R.string.rural
                    )

                    val servicesCollectionRef = firestore.collection("HotelServices")
                    servicesCollectionRef
                        .whereEqualTo("userUid", userUid)
                        .whereEqualTo("hotelName", hotelName)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val documentSnapshot = querySnapshot.documents[0]

                                documentSnapshot.data?.forEach { (key, value) ->
                                    if (key != "userUid" && key != "hotelName"
                                        && value is Boolean && value) {
                                        val serviceKey = serviceKeys[key]
                                        if (serviceKey != null) {
                                            val serviceName = holder.itemView.context.getString(serviceKey)
                                            servicesList.add(serviceName)
                                        }
                                    }
                                }

                                val servicesAdapter = HotelServicesAdapter(servicesList) {
                                    handleClick(holder.itemView.context, hotel.name)
                                }
                                holder.rvHotelServices.adapter = servicesAdapter
                            }
                        }
                }
            }

        holder.itemView.setOnClickListener {
            handleClick(holder.itemView.context, hotel.name)
        }
    }

    override fun getItemCount(): Int = hotels.size

    inner class HotelImagesAdapter(private val imageUris: MutableList<Uri>, val clickListener: () -> Unit) : RecyclerView.Adapter<HotelImagesAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.hotel_image_view)

            init {
                itemView.setOnClickListener { clickListener() }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hotel_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val uri = imageUris[position]
            val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(30))
            Glide.with(holder.itemView.context)
                .load(uri)
                .apply(requestOptions)
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = imageUris.size
    }

    inner class HotelServicesAdapter(private val servicesData: MutableList<String>, val clickListener: () -> Unit) : RecyclerView.Adapter<HotelServicesAdapter.ServiceViewHolder>() {

        inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val serviceName: TextView = itemView.findViewById(R.id.service_name_text)

            init {
                itemView.setOnClickListener { clickListener() }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hotel_service, parent, false)
            return ServiceViewHolder(view)
        }

        override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
            val service = servicesData[position]
            holder.serviceName.text = service
        }

        override fun getItemCount(): Int = servicesData.size
    }

    private fun handleClick(context: Context, hotelName: String) {
        val intent = Intent(context, HotelViewActivity::class.java)
        intent.putExtra("hotelName", hotelName)
        context.startActivity(intent)
    }
}