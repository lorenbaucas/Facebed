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
import com.facebed.adapters.HotelAdapter
import com.facebed.controllers.FirebaseController
import com.facebed.models.Hotel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class HomeFragment : Fragment() {
    private lateinit var hotelsAdapter: HotelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView

    private val hotels: MutableList<Hotel> = mutableListOf()
    private val displayedHotels: MutableList<Hotel> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_hotels_company)
        searchView = view.findViewById(R.id.search_view)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        hotelsAdapter = HotelAdapter(displayedHotels)
        recyclerView.adapter = hotelsAdapter

        //Lista de hoteles con todos sus datos
        val hotelsCollectionRef = FirebaseFirestore.getInstance().collection("Hotels")
        hotelsCollectionRef.get().addOnSuccessListener { querySnapshot ->
            for (documentSnapshot in querySnapshot.documents) {
                val hotelName = documentSnapshot.getString("hotelName")
                val location = documentSnapshot.getString("location")
                val userUid = documentSnapshot.getString("userUid").toString()
                val hotelId = documentSnapshot.id

                FirebaseController.getHotel(userUid, hotelName!!) {
                    val imageRef = FirebaseStorage.getInstance().reference
                        .child("HotelsData/$userUid/${hotelId}/MainPhotos/image_0.jpg")

                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val hotel = Hotel(hotelName, hotelId, location!!, uri)
                        hotels.add(hotel)
                        displayedHotels.add(hotel)
                        displayedHotels.sortWith(compareBy { it.name })
                        hotelsAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        //Para filtrar los hoteles por su nombre
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
            hotels
        } else {
            hotels.filter { it.name.contains(query, ignoreCase = true) }
        }
        displayedHotels.clear()
        displayedHotels.addAll(filteredList)
        hotelsAdapter.notifyDataSetChanged()
    }
}