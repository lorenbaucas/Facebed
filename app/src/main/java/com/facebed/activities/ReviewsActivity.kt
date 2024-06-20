package com.facebed.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.ReviewsAdapter
import com.facebed.controllers.FirebaseController
import com.facebed.models.Booking
import com.google.firebase.firestore.FirebaseFirestore

class ReviewsActivity : AppCompatActivity() {

    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var rvReviews: RecyclerView
    private lateinit var averageTextView: TextView
    private var bookingsWithReviews: MutableList<Booking> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reviews_activity)

        rvReviews = findViewById(R.id.rv_reviews_company)
        rvReviews.layoutManager = LinearLayoutManager(this)
        reviewsAdapter = ReviewsAdapter(bookingsWithReviews, showButtons = false)
        rvReviews.adapter = reviewsAdapter

        averageTextView = findViewById(R.id.stars_number_text)

        val hotelId = intent.getStringExtra("hotelId")

        if (hotelId != null) {
            loadBookingsWithReviews(hotelId) {
                FirebaseController.calculateReviewsAverage(hotelId) { formattedAverage ->
                    averageTextView.text = formattedAverage
                }
            }
        }
    }

    //Cargamos las reservas que tengan reseñas
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