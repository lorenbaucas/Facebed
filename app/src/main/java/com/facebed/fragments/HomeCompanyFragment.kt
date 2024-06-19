package com.facebed.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.CustomerAdapter
import com.facebed.controllers.FirebaseController
import com.facebed.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class HomeCompanyFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var customerAdapter: CustomerAdapter
    private lateinit var searchView: SearchView

    private val bookingList: MutableList<Booking> = mutableListOf()
    private val displayedBookings: MutableList<Booking> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.home_company_fragment, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_bookings)
        searchView = view.findViewById(R.id.search_view)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        customerAdapter = CustomerAdapter(displayedBookings)
        recyclerView.adapter = customerAdapter

        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val firestore = FirebaseFirestore.getInstance()
        val hotelsCollectionRef = firestore.collection("Hotels")

        hotelsCollectionRef.get().addOnSuccessListener { result ->
            val hotelIds = mutableListOf<String>()

            for (document in result) {
                val hotelUserUid = document.getString("userUid").toString()
                if (hotelUserUid == userUid) {
                    val hotelId = document.id
                    hotelIds.add(hotelId)
                }
            }

            if (hotelIds.isNotEmpty()) {
                firestore.collection("Bookings").whereIn("hotelId", hotelIds)
                    .get().addOnSuccessListener { result ->
                        val allBookings = result.map { document ->
                            document.toObject(Booking::class.java)
                        }

                        FirebaseController.filterAndExcludeCancelledBookings(allBookings) { validBookings ->
                            val currentDate = Calendar.getInstance().timeInMillis
                            val futureBookings = validBookings.filter { booking ->
                                booking.datesList.last().timeInMillis >= currentDate
                            }
                            displayedBookings.clear()
                            bookingList.clear()
                            displayedBookings.addAll(futureBookings)
                            bookingList.addAll(displayedBookings)
                            customerAdapter.notifyDataSetChanged()
                        }
                    }
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterHotels(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterHotels(newText)
                return false
            }
        })
    }

    private fun filterHotels(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            bookingList
        } else {
            displayedBookings.filter { booking ->
                booking.name.contains(query, ignoreCase = true) ||
                        booking.id.contains(query, ignoreCase = true) ||
                        booking.bookingId.contains(query, ignoreCase = true)
            }
        }
        displayedBookings.clear()
        displayedBookings.addAll(filteredList)
        customerAdapter.notifyDataSetChanged()
    }
}