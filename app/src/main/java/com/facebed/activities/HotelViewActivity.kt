package com.facebed.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebed.R
import com.facebed.adapters.ImagesAdapter
import com.facebed.adapters.ReviewsAdapter
import com.facebed.adapters.RoomAdapter
import com.facebed.adapters.ServicesAdapter
import com.facebed.controllers.FirebaseController
import com.facebed.controllers.Utils
import com.facebed.models.Booking
import com.facebed.models.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class HotelViewActivity : AppCompatActivity() {
    private lateinit var hotelImage: CircleImageView

    private lateinit var hotelName: TextView
    private lateinit var hotelLocation: TextView
    private lateinit var hotelRating: TextView
    private lateinit var hotelDescription: TextView

    private lateinit var rvHotelServices: RecyclerView
    private lateinit var rvHotelImages: RecyclerView
    private lateinit var rvRooms: RecyclerView

    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var rvReviews: RecyclerView
    private lateinit var averageTextView: TextView
    private var bookingsWithReviews: MutableList<Booking> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hotel_view_activity)

        hotelImage = findViewById(R.id.image)
        hotelName = findViewById(R.id.hotel_name_text)
        hotelLocation = findViewById(R.id.location_text)
        hotelRating = findViewById(R.id.stars_number_text)
        hotelDescription = findViewById(R.id.description_text)

        rvHotelServices = findViewById(R.id.rv_services)
        rvHotelImages = findViewById(R.id.rv_images)
        rvRooms = findViewById(R.id.rv_rooms)

        averageTextView = findViewById(R.id.stars_text)

        rvHotelImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvHotelServices.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvRooms.layoutManager = LinearLayoutManager(this)

        rvReviews = findViewById(R.id.rv_reviews_company)
        rvReviews.layoutManager = LinearLayoutManager(this)
        reviewsAdapter = ReviewsAdapter(bookingsWithReviews, showButtons = false)
        rvReviews.adapter = reviewsAdapter

        val imageUris: MutableList<Uri> = mutableListOf()
        val servicesList: MutableList<String> = mutableListOf()
        val roomsList: MutableList<Room> = mutableListOf()

        //Calculamos la media de las reseñas de ese hotel
        val hotelId = intent.getStringExtra("hotelId")
        if (hotelId != null) {
            loadBookingsWithReviews(hotelId) {
                FirebaseController.calculateReviewsAverage(hotelId) { formattedAverage ->
                    averageTextView.text = formattedAverage
                }
            }
        }

        //Cargamos los datos del hotel
        FirebaseFirestore.getInstance().collection("Hotels").document(hotelId!!)
            .get().addOnSuccessListener { documentSnapshot ->
                val userUid = documentSnapshot.getString("userUid")
                val name = documentSnapshot.getString("hotelName")
                val location = documentSnapshot.getString("location")
                val description = documentSnapshot.getString("description")
                val stars = documentSnapshot.getLong("stars")

                hotelName.text = name
                hotelLocation.text = location
                hotelRating.text = stars.toString()
                hotelDescription.text = description

                val storageRef = FirebaseStorage.getInstance().reference
                    .child("HotelsData/$userUid/$hotelId/MainPhotos")

                storageRef.listAll().addOnSuccessListener { listResult ->
                    if (listResult.items.isNotEmpty()) {
                        listResult.items[0].downloadUrl.addOnSuccessListener { uri ->
                            Glide.with(this)
                                .load(uri)
                                .into(hotelImage)
                        }
                    }
                }

                //Fotos del hotel
                storageRef.listAll().addOnSuccessListener { listResult ->
                    listResult.items.forEachIndexed { index, item ->
                        item.downloadUrl.addOnSuccessListener { uri ->
                            imageUris.add(uri)

                            if (index == listResult.items.size - 1) {
                                imageUris.sortBy { it.toString() }
                                val imageAdapter = ImagesAdapter(imageUris) { uri ->
                                    val intent = Intent(this, FullScreenImageActivity::class.java).apply {
                                        putExtra("uri", uri.toString())
                                    }
                                    startActivity(intent)
                                }
                                rvHotelImages.adapter = imageAdapter
                            }
                        }
                    }
                }
                val serviceKeys = Utils.getHotelServiceKeys()

                //Servicios del hotel
                FirebaseController.getHotelServices(userUid!!, hotelId) { documentSnapshot ->
                    documentSnapshot?.data?.forEach { (key, value) ->
                        if (key != "userUid" && key != "hotelId"
                            && value is Boolean && value) {
                            val serviceKey = serviceKeys[key]
                            if (serviceKey != null) {
                                val serviceName = this.getString(serviceKey)
                                servicesList.add(serviceName)
                            }
                        }
                    }

                    val servicesAdapter = ServicesAdapter(servicesList) {}
                    rvHotelServices.adapter = servicesAdapter
                }

                //Lista de habitaciones del hotel
                FirebaseController.getRooms(hotelId) { documents ->
                    for (documentSnapshot in documents) {
                        val roomName = documentSnapshot.getString("roomName")
                        val number = documentSnapshot.getString("number")
                        val maxPeople = documentSnapshot.getString("maxPeople")
                        val price = documentSnapshot.getString("price")

                        val room = Room(roomName!!, hotelId, name!!, number!!, maxPeople!!, price!!)
                        roomsList.add(room)
                        roomsList.sortWith(compareBy { it.price })
                    }

                    val roomsAdapter = RoomAdapter(roomsList)
                    rvRooms.adapter = roomsAdapter
                }
            }
    }

    //Solo se cargaran las reservas de ese hotel que hayan sido finalizadas y tengan una reseña
    private fun loadBookingsWithReviews(hotelId: String, onComplete: () -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("Bookings")
            .whereEqualTo("hotelId", hotelId)
            .get()
            .addOnSuccessListener { documents ->
                bookingsWithReviews.clear()
                val allBookings = documents.map { document ->
                    document.toObject(Booking::class.java)
                }
                var loadedCount = 0

                allBookings.forEach { booking ->
                    firestore.collection("Reviews")
                        .document(booking.bookingId)
                        .get()
                        .addOnSuccessListener { reviewDocument ->
                            if (reviewDocument.exists()) {
                                bookingsWithReviews.add(booking)
                                reviewsAdapter.notifyDataSetChanged()
                            }
                            loadedCount++

                            if (loadedCount == allBookings.size) {
                                onComplete()
                            }
                        }
                }
            }
    }
}