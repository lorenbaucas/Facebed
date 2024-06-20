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
        //Para borrar todas las imagenes
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

        //Para subir las imagenes
        fun uploadImages(storageRef: StorageReference, images: MutableList<Uri>, onComplete: () -> Unit) {
            images.forEachIndexed { index, uri ->
                val imageRef = storageRef.child("image_$index.jpg")
                val uploadTask = imageRef.putFile(uri)

                uploadTask.addOnSuccessListener {
                    if (index == images.lastIndex) { onComplete() }
                }
            }
        }

        //Para obtener las imagenes
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

        //Para obtener un hotel en especifico
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

        //Para obtener los servicios de un hotel
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

        //Para obtener las habitaciones de un hotel
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

        //Para obtener una habitacion especifica de un hotel
        fun getRoom(hotelId: String, number: String, onComplete: (DocumentSnapshot?) -> Unit) {
            val roomRef = FirebaseFirestore.getInstance().collection("Rooms")
            roomRef
                .whereEqualTo("hotelId", hotelId)
                .whereEqualTo("number", number)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        onComplete(querySnapshot.documents[0])
                    } else {
                        onComplete(null)
                    }
                }
                .addOnFailureListener { exception ->
                    onComplete(null)
                }
        }

        //Para obtener los servicios de una habitacion
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

        //Para eliminar un documento entero en especifico
        fun deleteFirebaseData(collectionRef: CollectionReference, userUid: String) {
            collectionRef.whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        collectionRef.document(document.id).delete()
                    }
                }
        }

        //Para cancelar una reserva
        fun cancelBooking(booking: Booking, reason: String) {
            val updates = hashMapOf<String, Any>(
                "reason" to reason,
                "pending" to false
            )

            FirebaseFirestore.getInstance().collection("Bookings")
                .document(booking.bookingId)
                .update(updates)
        }

        //Para filtrar las reservas futuras o pasadas
        fun filterBookings(bookings: List<Booking>): List<Booking> {
            val today = Calendar.getInstance().timeInMillis
            return bookings.filter { booking ->
                val endDate = booking.datesList.last().timeInMillis
                endDate < today || booking.reason != null
            }
        }

        //Para filtrar las reservas canceladas
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

        //Para calcular la media de las reseñas del hotel
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