package com.facebed.controllers

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference

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

        fun getHotelId(userUid: String, hotelName: String, onComplete: (DocumentSnapshot?) -> Unit) {
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

        fun getHotelServicesId(userUid: String, hotelId: String, onComplete: (DocumentSnapshot?) -> Unit) {
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
    }
}
