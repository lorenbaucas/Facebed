package com.facebed.controllers

import android.net.Uri
import com.facebed.models.Booking
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import java.util.Calendar

class FirebaseController {
    companion object {
        fun deleteImages(storageRef: StorageReference, onComplete: () -> Unit) {
            storageRef.listAll()
                .addOnSuccessListener { listResult ->
                    val deleteTasks = listResult.items.map { item ->
                        item.delete()
                    }

                    Tasks.whenAll(deleteTasks)
                        .addOnSuccessListener { onComplete() }
                }
        }

        fun uploadImages(storageRef: StorageReference, images: MutableList<Uri>, onComplete: () -> Unit) {
            images.forEachIndexed { index, uri ->
                val imageRef = storageRef.child("image_$index.jpg")
                val uploadTask = imageRef.putFile(uri)

                uploadTask.addOnSuccessListener {
                    if (index == images.lastIndex) { onComplete() }
                }
            }
        }

        fun getImages(storageRef: StorageReference, images: MutableList<Uri>, onComplete: () -> Unit) {
            storageRef.listAll().addOnSuccessListener { listResult ->
                listResult.items.forEachIndexed { index, item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        images.add(uri)

                        if (index == listResult.items.size - 1) {
                            images.sortBy { it.toString() }
                            onComplete()
                        }
                    }
                }
            }
        }

        fun getHotel(userUid: String, hotelName: String, onComplete: (DocumentSnapshot?) -> Unit) {
            val hotelsCollectionRef = FirebaseFirestore.getInstance().collection("Hotels")
            hotelsCollectionRef
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("hotelName", hotelName)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        onComplete(documentSnapshot)
                    } else {
                        onComplete(null)
                    }
                }
        }

        fun getHotelServices(userUid: String, hotelId: String, onComplete: (DocumentSnapshot?) -> Unit) {
            val servicesCollectionRef = FirebaseFirestore.getInstance().collection("HotelServices")
            servicesCollectionRef
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("hotelId", hotelId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        onComplete(documentSnapshot)
                    } else {
                        onComplete(null)
                    }
                }
        }

        fun getRooms(hotelId: String, onComplete: (MutableList<DocumentSnapshot>) -> Unit) {
            val roomsCollectionRef = FirebaseFirestore.getInstance().collection("Rooms")
            roomsCollectionRef
                .whereEqualTo("hotelId", hotelId)
                .get()
                .addOnSuccessListener {
                    val documents = it.documents
                    onComplete(documents)
                }
        }

        fun getRoomServices(hotelId: String, number: String, onComplete: (DocumentSnapshot?) -> Unit) {
            val servicesCollectionRef = FirebaseFirestore.getInstance().collection("RoomServices")
            servicesCollectionRef
                .whereEqualTo("hotelId", hotelId)
                .whereEqualTo("number", number)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        onComplete(documentSnapshot)
                    } else {
                        onComplete(null)
                    }
                }
        }

        fun deleteFirebaseData(collectionRef: CollectionReference, userUid: String) {
            collectionRef.whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        collectionRef.document(document.id).delete()
                    }
                }
        }
        fun cancelBooking(booking: Booking, reason: String) {
            // Update the booking in Firebase to set pending to false and add a reason for cancellation
            val updates = hashMapOf<String, Any>(
                "reason" to reason,
                "pending" to false
            )

            FirebaseFirestore.getInstance().collection("Bookings")
                .document(booking.bookingId)
                .update(updates)
        }

        fun filterBookings(bookings: List<Booking>): List<Booking> {
            val today = Calendar.getInstance().timeInMillis
            return bookings.filter { booking ->
                val endDate = booking.datesList.last().timeInMillis
                endDate < today || booking.reason != null
            }
        }

        fun filterAndExcludeCancelledBookings(bookings: List<Booking>, onComplete: (List<Booking>) -> Unit) {
            FirebaseFirestore.getInstance().collection("Bookings")
                .whereIn("bookingId", bookings.map { it.bookingId })
                .get()
                .addOnSuccessListener { documents ->
                    val validBookings = documents.map { document ->
                        document.toObject(Booking::class.java)
                    }.filter { booking ->
                        booking.datesList.last().timeInMillis >= Calendar.getInstance().timeInMillis && booking.reason == null
                    }
                    onComplete(validBookings)
                }
                .addOnFailureListener {
                    onComplete(emptyList())
                }
        }

        fun calculateReviewsAverage(hotelId: String, callback: (String) -> Unit) {
            FirebaseFirestore.getInstance().collection("Reviews")
                .whereEqualTo("hotelId", hotelId)
                .get()
                .addOnSuccessListener { documents ->
                    var totalStars = 0
                    var numberOfReviews = 0

                    for (document in documents) {
                        val stars = document.getLong("stars")
                        if (stars != null) {
                            totalStars += stars.toInt()
                            numberOfReviews++
                        }
                    }

                    val average = if (numberOfReviews > 0) {
                        totalStars.toDouble() / numberOfReviews.toDouble()
                    } else {
                        0.0
                    }

                    val formattedAverage = "%.2f".format(average.coerceIn(0.0, 5.0))
                    callback(formattedAverage)
                }
        }
    }
}