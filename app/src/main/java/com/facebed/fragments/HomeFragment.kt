package com.facebed.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.Hotel
import com.facebed.adapters.HotelAdapter
import com.facebed.controllers.FirebaseController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class HomeFragment : Fragment() {
    private lateinit var hotelsAdapter: HotelAdapter
    private val hotels: MutableList<Hotel> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.home_fragment, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.rv_hotels_company)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        hotelsAdapter = HotelAdapter(hotels)
        recyclerView.adapter = hotelsAdapter

        val firestore = FirebaseFirestore.getInstance()

        val hotelsCollectionRef = firestore.collection("Hotels")

        hotelsCollectionRef.get().addOnSuccessListener { querySnapshot ->
            for (documentSnapshot in querySnapshot.documents) {
                val hotelName = documentSnapshot.getString("hotelName")
                val location = documentSnapshot.getString("location")
                val userUid = documentSnapshot.getString("userUid").toString()

                FirebaseController.getHotelId(userUid, hotelName!!) { documentSnapshot ->
                    val imageRef = FirebaseStorage.getInstance().reference
                        .child("HotelsData/$userUid/${documentSnapshot?.id}/MainPhotos/image_0.jpg")

                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val hotel = Hotel(hotelName, location!!, uri)
                        hotels.add(hotel)
                        hotels.sortWith(compareBy { it.name })
                        hotelsAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }
}