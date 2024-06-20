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
import com.facebed.controllers.Utils
import com.facebed.models.SimpleRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class HotelRoomsActivity : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var addRoomButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: HotelRoomsAdapter
    private lateinit var roomList: MutableList<SimpleRoom>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hotel_rooms_activity)

        title = findViewById(R.id.your_rooms_text)

        addRoomButton = findViewById(R.id.add_room_button)
        recyclerView = findViewById(R.id.rv_hotel_rooms)

        title.text = intent.getStringExtra("hotelName")

        //Nos llevara para crear una nueva habitacion
        addRoomButton.setOnClickListener {
            startActivity(
                Intent(this, AddRoomActivity::class.java)
                    .putExtra("hotelName", intent.getStringExtra("hotelName"))
                    .putExtra("hotelId", intent.getStringExtra("hotelId"))
            )
        }

        //Lista de habitaciones con sus datos
        roomList = mutableListOf()
        roomsAdapter = HotelRoomsAdapter(roomList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = roomsAdapter

        loadRoomData()
    }

    private fun loadRoomData() {
        val user = FirebaseAuth.getInstance().currentUser
        val userUid = user?.uid

        val firestore = FirebaseFirestore.getInstance()
        val roomsCollectionRef = firestore.collection("Rooms")

        val hotelId = intent.getStringExtra("hotelId")
        roomsCollectionRef.get().addOnSuccessListener { result ->
            for (document in result) {
                val roomName = document.getString("roomName").toString()
                val number = document.getString("number").toString()
                val roomUserUid = document.getString("userUid").toString()
                val roomId = document.id

                if (roomUserUid == userUid) {
                    val imageRef = FirebaseStorage.getInstance().reference
                        .child("HotelsData/$userUid/$hotelId/$roomId")

                    imageRef.listAll().addOnSuccessListener { listResult ->
                        if (listResult.items.isNotEmpty()) {
                            listResult.items[0].downloadUrl.addOnSuccessListener { uri ->
                                val room = SimpleRoom(roomName, number, uri)
                                roomList.add(room)
                                roomsAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }.addOnFailureListener { Utils.error(this) }
    }
}