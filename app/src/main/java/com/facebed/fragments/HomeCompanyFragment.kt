package com.facebed.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.BookingAdapter
import com.facebed.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeCompanyFragment : Fragment() {
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var bookings: MutableList<Booking>
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.home_company_fragment, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        bookings = mutableListOf()
        bookingAdapter = BookingAdapter(bookings)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = bookingAdapter

        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid != null) {
            FirebaseFirestore.getInstance().collection("Hotels")
                .whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener { hotelDocuments ->
                    val hotelIds = hotelDocuments.map { it.id }

                    if (hotelIds.isNotEmpty()) {
                        FirebaseFirestore.getInstance().collection("Bookings")
                            .whereIn("hotelId", hotelIds)
                            .get()
                            .addOnSuccessListener { bookingDocuments ->
                                for (document in bookingDocuments) {
                                    val booking = document.toObject(Booking::class.java)
                                    bookings.add(booking)
                                }
                                bookingAdapter.notifyDataSetChanged()
                            }
                    }
                }
        }
    }
}