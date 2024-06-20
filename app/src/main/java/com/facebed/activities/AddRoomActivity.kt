package com.facebed.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.adapters.AddImagesAdapter
import com.facebed.controllers.FirebaseController
import com.facebed.controllers.Utils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AddRoomActivity : AppCompatActivity() {
    private lateinit var roomNameText: AutoCompleteTextView
    private lateinit var maxPeopleNumber: EditText
    private lateinit var roomNumber: EditText
    private lateinit var roomPrice: EditText

    private lateinit var addPhotosButton: Button
    private lateinit var finishButton: Button
    private lateinit var updateButton: Button

    private lateinit var progressBar: ProgressBar

    private lateinit var chipGroup: ChipGroup

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var imageUris: MutableList<Uri>
    private lateinit var imagesAdapter: AddImagesAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var initialRoomData: HashMap<String, Any?>
    private lateinit var initialServicesData: HashMap<String, Any?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_room_activity)

        roomNameText = findViewById(R.id.room_name_text)
        maxPeopleNumber = findViewById(R.id.people_number)
        roomNumber = findViewById(R.id.room_number)
        roomPrice = findViewById(R.id.room_price)

        addPhotosButton = findViewById(R.id.add_photos_button)
        finishButton = findViewById(R.id.finish_button)
        updateButton = findViewById(R.id.update_button)

        progressBar = findViewById(R.id.progress_bar)

        chipGroup = findViewById(R.id.chip_group)

        imageUris = mutableListOf()
        imagesAdapter = AddImagesAdapter(imageUris)

        recyclerView = findViewById(R.id.rv_images)

        val firestore = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val userUid = user?.uid

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = imagesAdapter

        //Precargamos los datos de la habitacion que ya existia para modificarla
        val roomNumberExists = intent.getStringExtra("number")
        if (!roomNumberExists.isNullOrEmpty()) {
            val roomsCollectionRef = firestore.collection("Rooms")
            roomsCollectionRef
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("number", roomNumberExists)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        val roomId = documentSnapshot.id

                        val hotelId = documentSnapshot.getString("hotelId")
                        val hotelName = documentSnapshot.getString("hotelName")
                        val maxPeople = documentSnapshot.getString("maxPeople")
                        val price = documentSnapshot.getString("price")
                        val roomName = documentSnapshot.getString("roomName")

                        roomNameText.setText(roomName)
                        maxPeopleNumber.setText(maxPeople)
                        roomNumber.setText(roomNumberExists)
                        roomPrice.setText(price)

                        initialRoomData = hashMapOf(
                            "hotelId" to hotelId,
                            "hotelName" to hotelName,
                            "maxPeople" to maxPeople,
                            "number" to roomNumberExists,
                            "price" to price,
                            "roomName" to roomName,
                            "userUid" to userUid
                        )

                        val storageRef = FirebaseStorage.getInstance().reference
                            .child("HotelsData/$userUid/$hotelId/$roomId")

                        FirebaseController.getImages(storageRef, imageUris) {
                            imagesAdapter.notifyDataSetChanged()
                            updateButton.visibility = View.VISIBLE
                        }

                        FirebaseController.getRoomServices(hotelId!!, roomNumberExists) { documentSnapshot ->
                            chipGroup.visibility = View.VISIBLE
                            val servicesData = hashMapOf<String, Any?>()

                            documentSnapshot?.data?.forEach { (key, value) ->

                                val chip = chipGroup.findViewWithTag<Chip>(key)
                                chip?.isChecked = value as Boolean
                                servicesData[key] = value

                            }
                            initialServicesData = servicesData
                        }
                    }
                }
        }

        //Guardamos los datos de la habitacion modificada
        updateButton.setOnClickListener {
            val updatedRoomData = hashMapOf<String, Any?>(
                "userUid" to userUid,
                "hotelId" to intent.getStringExtra("hotelId").toString(),
                "hotelName" to intent.getStringExtra("hotelName"),
                "roomName" to roomNameText.text.toString().trim(),
                "maxPeople" to maxPeopleNumber.text.toString().trim(),
                "number" to roomNumber.text.toString().trim(),
                "price" to roomPrice.text.toString().trim()
            )

            if (imageUris.size > 2) {
                progressBar.visibility = View.VISIBLE
                updateButton.visibility = View.GONE
                val hotelId = updatedRoomData["hotelId"] as String
                val userUid = updatedRoomData["userUid"] as String
                val number = updatedRoomData["number"] as String

                FirebaseController.getRoom(hotelId, number) { documentSnapshot ->
                    documentSnapshot?.let {
                        val roomId = it.id
                        val roomDocumentRef = it.reference

                        val updateRoomDataTask = roomDocumentRef.update(updatedRoomData).addOnSuccessListener {
                            val storageRef = FirebaseStorage.getInstance().reference
                                .child("HotelsData/$userUid/$hotelId/$roomId")

                            FirebaseController.uploadImages(storageRef, imageUris) {}
                        }.addOnFailureListener { Utils.error(this) }

                        updateRoomDataTask.addOnSuccessListener {
                            val updatedServicesData = getServices(hotelId, userUid, number)

                            if (initialServicesData != updatedServicesData) {
                                val updatedRoomName = updatedRoomData["number"] as String
                                FirebaseController.getRoomServices(hotelId, number) { documentSnapshot ->
                                    documentSnapshot?.let {
                                        val servicesDocumentRef = it.reference
                                        val servicesDataToUpdate = hashMapOf<String, Any?>()
                                        updatedServicesData.forEach { (key, value) ->
                                            if (key != "hotelId" || key != "userUid") {
                                                servicesDataToUpdate[key] = value
                                            }
                                        }
                                        servicesDataToUpdate["roomName"] = updatedRoomName
                                        val updateServicesDataTask = servicesDocumentRef.update(servicesDataToUpdate)

                                        updateServicesDataTask.addOnSuccessListener {
                                            Toast.makeText(
                                                this,
                                                getString(R.string.data_saved_successfully),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            startActivity(Intent(this, HomeCompanyActivity::class.java))
                                            finish()
                                        }.addOnFailureListener {
                                            progressBar.visibility = View.GONE
                                            finishButton.visibility = View.VISIBLE
                                            Utils.error(this)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //Añadimos fotos
        addPhotosButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            imagePickerLauncher.launch(intent)
        }

        //Seleccionamos las fotos
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                handleImagePickerResult(result)
            }
        }

        //Añadimos una nueva habitacion
        finishButton.setOnClickListener {
            val roomName = roomNameText.text.toString().trim()
            val maxPeople = maxPeopleNumber.text.toString().trim()
            val number = roomNumber.text.toString().trim()
            val price = roomPrice.text.toString().trim()

            if (roomName.isNotEmpty()) {
                if (maxPeople.isNotEmpty()) {
                    if (number.isNotEmpty()) {
                        if (price.isNotEmpty()) {
                            if (imageUris.size > 2) {
                                progressBar.visibility = View.VISIBLE
                                finishButton.visibility = View.GONE

                                val hotelId = intent.getStringExtra("hotelId").toString()

                                val services = hashMapOf(
                                    "hotelId" to hotelId,
                                    "userUid" to userUid,
                                    "number" to number,
                                    "hot_tub" to findViewById<Chip>(R.id.hot_tub).isChecked,
                                    "air_conditioning" to findViewById<Chip>(R.id.air_conditioning).isChecked,
                                    "minibar" to findViewById<Chip>(R.id.minibar).isChecked,
                                    "balcony_terrace" to findViewById<Chip>(R.id.balcony_terrace).isChecked,
                                    "heating" to findViewById<Chip>(R.id.heating).isChecked,
                                    "tv" to findViewById<Chip>(R.id.tv).isChecked,
                                    "breakfast" to findViewById<Chip>(R.id.breakfast).isChecked,
                                    "wifi" to findViewById<Chip>(R.id.wifi).isChecked,
                                    "microwave" to findViewById<Chip>(R.id.microwave).isChecked,
                                    "ceiling_fan" to findViewById<Chip>(R.id.ceiling_fan).isChecked
                                )

                                val servicesDocumentRef =
                                    firestore.collection("RoomServices").document()
                                servicesDocumentRef.set(services)
                                    .addOnSuccessListener {
                                        val roomData = hashMapOf(
                                            "userUid" to userUid,
                                            "hotelId" to hotelId,
                                            "hotelName" to intent.getStringExtra("hotelName"),
                                            "roomName" to roomName,
                                            "maxPeople" to maxPeople,
                                            "number" to number,
                                            "price" to price
                                        )

                                        val roomDocumentRef =
                                            firestore.collection("Rooms").document()
                                        roomDocumentRef.set(roomData)
                                            .addOnSuccessListener {
                                                val storageRef = FirebaseStorage.getInstance().reference
                                                    .child("HotelsData/$userUid/$hotelId/${roomDocumentRef.id}")

                                                imageUris.forEachIndexed { index, uri ->
                                                    val imageRef = storageRef.child("image_$index.jpg")
                                                    val uploadTask = imageRef.putFile(uri)

                                                    uploadTask.addOnSuccessListener {
                                                        if (index == imageUris.lastIndex) {
                                                            Toast.makeText(this, getString(R.string.data_saved_successfully), Toast.LENGTH_SHORT).show()
                                                            startActivity(Intent(this, HomeCompanyActivity::class.java))
                                                            changeVisibility()
                                                            finish()
                                                        }
                                                    }.addOnFailureListener {
                                                        changeVisibility()
                                                        Utils.error(this)
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                changeVisibility()
                                                Utils.error(this)
                                            }
                                    }
                                    .addOnFailureListener {
                                        changeVisibility()
                                        Utils.error(this)
                                    }
                            } else { Toast.makeText(this, getString(R.string.add_photos), Toast.LENGTH_SHORT).show() }
                        } else { roomPrice.error = getString(R.string.provide_price_per_night) }
                    } else { roomNumber.error = getString(R.string.provide_room_number) }
                } else { maxPeopleNumber.error = getString(R.string.provide_max_people) }
            } else { roomNameText.error = getString(R.string.provide_room_name) }
        }
    }

    private fun handleImagePickerResult(result: ActivityResult) {
        val data = result.data
        data?.clipData?.let {
            for (i in 0 until it.itemCount) {
                imageUris.add(it.getItemAt(i).uri)
            }
        }

        data?.data?.let { imageUris.add(it) }

        imagesAdapter.notifyDataSetChanged()

        val roomNumberExists = intent.getStringExtra("number")

        if (imageUris.size >= 3) {
            if (!roomNumberExists.isNullOrEmpty()) {
                finishButton.visibility = View.GONE
                chipGroup.visibility = View.VISIBLE
            } else {
                finishButton.visibility = View.VISIBLE
                chipGroup.visibility = View.VISIBLE
            }
        } else {
            finishButton.visibility = View.GONE
            chipGroup.visibility = View.GONE
        }
    }

    private fun changeVisibility() {
        progressBar.visibility = View.GONE
        finishButton.visibility = View.VISIBLE
    }

    private fun getServices(userUid: String, hotelId: String, number: String): HashMap<String, Any> {
        return hashMapOf(
            "hotelId" to hotelId,
            "userUid" to userUid,
            "number" to number,
            "hot_tub" to findViewById<Chip>(R.id.hot_tub).isChecked,
            "air_conditioning" to findViewById<Chip>(R.id.air_conditioning).isChecked,
            "minibar" to findViewById<Chip>(R.id.minibar).isChecked,
            "balcony_terrace" to findViewById<Chip>(R.id.balcony_terrace).isChecked,
            "heating" to findViewById<Chip>(R.id.heating).isChecked,
            "tv" to findViewById<Chip>(R.id.tv).isChecked,
            "breakfast" to findViewById<Chip>(R.id.breakfast).isChecked,
            "wifi" to findViewById<Chip>(R.id.wifi).isChecked,
            "microwave" to findViewById<Chip>(R.id.microwave).isChecked,
            "ceiling_fan" to findViewById<Chip>(R.id.ceiling_fan).isChecked
        )
    }
}