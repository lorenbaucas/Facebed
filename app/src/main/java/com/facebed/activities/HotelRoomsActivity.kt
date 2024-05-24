package com.facebed.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.HotelRoomsAdapter
import com.facebed.adapters.Room
import com.facebed.controllers.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class HotelRoomsActivity : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var addRoomButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: HotelRoomsAdapter
    private lateinit var roomList: MutableList<Room>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hotel_rooms_activity)

        title = findViewById(R.id.your_rooms_text)

        addRoomButton = findViewById(R.id.add_room_button)
        recyclerView = findViewById(R.id.rv_hotel_rooms)

        title.text = intent.getStringExtra("hotelName")

        addRoomButton.setOnClickListener {
            startActivity(
                Intent(this, AddRoomActivity::class.java)
                    .putExtra("hotelName", title.text.trim().toString())
            )
        }

        roomList = mutableListOf()
        roomsAdapter = HotelRoomsAdapter(roomList)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = roomsAdapter

        loadRoomData()
    }

    private fun loadRoomData() {
        val user = FirebaseAuth.getInstance().currentUser
        val userUid = user?.uid

        val firestore = FirebaseFirestore.getInstance()
        val hotelsCollectionRef = firestore.collection("Rooms")

        val hotelName = intent.getStringExtra("hotelName")
        hotelsCollectionRef.get().addOnSuccessListener { result ->
            for (document in result) {
                val roomName = document.getString("roomName").toString()
                val number = document.getString("roomNumber").toString()
                val roomUserUid = document.getString("userUid").toString()

                if (roomUserUid == userUid) {
                    val imageRef = FirebaseStorage.getInstance().reference
                        .child("HotelsData/$userUid/$hotelName/$roomName/image_0.jpg")

                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val room = Room(roomName, number, uri)
                        roomList.add(room)
                        roomsAdapter.notifyDataSetChanged()
                    }
                }
            }
        }.addOnFailureListener { Utils.error(this) }
    }
}