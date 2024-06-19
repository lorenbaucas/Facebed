package com.facebed.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.BookingAdapter
import com.facebed.adapters.ReviewsAdapter
import com.facebed.controllers.FirebaseController
import com.facebed.models.Booking
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EventsFragment : Fragment() {
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var bookings: MutableList<Booking>
    private lateinit var bookingsWithReviews: MutableList<Booking>
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.events_fragment, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        tabLayout = view.findViewById(R.id.tabLayout)

        bookings = mutableListOf()
        bookingsWithReviews = mutableListOf()

        bookingAdapter = BookingAdapter(bookings, showReviewButton = false)
        recyclerView.adapter = bookingAdapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        bookingAdapter = BookingAdapter(bookings, showReviewButton = false)
                        recyclerView.adapter = bookingAdapter
                        loadCurrentBookings()
                    }
                    1 -> {
                        bookingAdapter = BookingAdapter(bookings, showReviewButton = true)
                        recyclerView.adapter = bookingAdapter
                        loadBookingHistory()
                    }
                    2 -> {
                        reviewsAdapter = ReviewsAdapter(bookingsWithReviews, showButtons = true)
                        recyclerView.adapter = reviewsAdapter
                        loadAllBookingsWithReviews()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Load current bookings by default
        loadCurrentBookings()
    }

    private fun loadCurrentBookings() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            FirebaseFirestore.getInstance().collection("Bookings")
                .whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        bookings.clear()
                        val allBookings = documents.map { document ->
                            document.toObject(Booking::class.java)
                        }
                        FirebaseController.filterAndExcludeCancelledBookings(allBookings) { validBookings ->
                            bookings.clear()
                            bookings.addAll(validBookings)
                            bookingAdapter.notifyDataSetChanged()
                        }
                    }
                }
        }
    }

    private fun loadBookingHistory() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            FirebaseFirestore.getInstance().collection("Bookings")
                .whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener { documents ->
                    bookings.clear()
                    val allBookings = documents.map { document ->
                        document.toObject(Booking::class.java)
                    }
                    val completedOrCancelledBookings = FirebaseController.filterBookings(allBookings)
                    bookings.clear()
                    bookings.addAll(completedOrCancelledBookings)
                    bookingAdapter.notifyDataSetChanged()
                }
        }
    }

    private fun loadAllBookingsWithReviews() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            FirebaseFirestore.getInstance().collection("Bookings")
                .whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener { documents ->
                    bookingsWithReviews.clear()
                    val allBookings = documents.map { document ->
                        document.toObject(Booking::class.java)
                    }
                    allBookings.forEach { booking ->
                        FirebaseFirestore.getInstance().collection("Reviews")
                            .document(booking.bookingId)
                            .get()
                            .addOnSuccessListener { reviewDocument ->
                                if (reviewDocument.exists()) {
                                    bookingsWithReviews.add(booking)
                                    reviewsAdapter.notifyDataSetChanged()
                                }
                            }
                    }
                }
        }
    }
}