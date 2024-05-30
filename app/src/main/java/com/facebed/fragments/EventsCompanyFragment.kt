package com.facebed.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.HotelsCompanyAdapter
import com.facebed.activities.AddHotelActivity
import com.facebed.models.Hotel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EventsCompanyFragment : Fragment() {
    private lateinit var addHotelButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var hotelsAdapter: HotelsCompanyAdapter
    private lateinit var hotelList: MutableList<Hotel>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.events_company_fragment, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addHotelButton = view.findViewById(R.id.add_hotel_button)
        recyclerView = view.findViewById(R.id.rv_hotels_company)

        addHotelButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddHotelActivity::class.java))
        }

        hotelList = mutableListOf()
        hotelsAdapter = HotelsCompanyAdapter(hotelList)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = hotelsAdapter

        loadHotelData()
    }

    private fun loadHotelData() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        val firestore = FirebaseFirestore.getInstance()
        val hotelsCollectionRef = firestore.collection("Hotels")

        hotelsCollectionRef.get().addOnSuccessListener { result ->
            for (document in result) {
                val hotelUserUid = document.getString("userUid").toString()

                if (hotelUserUid == userUid) {
                    val hotelName = document.getString("hotelName").toString()
                    val location = document.getString("location").toString()
                    val hotelId = document.id

                    val imageRef = FirebaseStorage.getInstance().reference
                        .child("HotelsData/$userUid/$hotelId/MainPhotos/image_0.jpg")

                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val hotel = Hotel(hotelName, hotelId, location, uri)
                        hotelList.add(hotel)
                        hotelsAdapter.notifyDataSetChanged()
                    }.addOnFailureListener {}
                }
            }
        }
    }
}