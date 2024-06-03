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
    val hotelName: String,
    val number: String,
    val maxPeople: String,
    val price: String,
)

data class Booking(
    val bookingId: String = "",
    val userUid: String = "",
    val hotelId: String = "",
    val hotelName: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val name: String = "",
    val pending: Boolean = false,
    val finalPrice: Double = 0.0,
    val datesList: List<Date> = listOf()
) {
    data class Date(
        val timeInMillis: Long = 0
    )
}