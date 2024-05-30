package com.facebed.models

import android.net.Uri

data class Hotel(
    val name: String,
    val hotelId: String,
    val location: String,
    val imageUri: Uri
)

data class SimpleRoom(
    val name: String,
    val number: String,
    val imageUri: Uri
)

data class Room(
    val name: String,
    val hotelId: String,
    val number: String,
    val maxPeople: Int,
    val price: Int,
)