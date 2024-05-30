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

    private lateinit var progressBar: ProgressBar

    private lateinit var chipGroup: ChipGroup

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var imageUris: MutableList<Uri>
    private lateinit var imagesAdapter: AddImagesAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_room_activity)

        roomNameText = findViewById(R.id.room_name_text)
        maxPeopleNumber = findViewById(R.id.people_number)
        roomNumber = findViewById(R.id.room_number)
        roomPrice = findViewById(R.id.room_price)

        addPhotosButton = findViewById(R.id.add_photos_button)
        finishButton = findViewById(R.id.finish_button)

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

        addPhotosButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            imagePickerLauncher.launch(intent)
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                handleImagePickerResult(result)
            }
        }

        finishButton.setOnClickListener {
            val roomName = roomNameText.text.toString().trim()
            val maxPeople = maxPeopleNumber.text.toString().trim()
            val number = roomNumber.text.toString().trim()
            val price = roomPrice.text.toString().trim()

            if (roomName.isNotEmpty()) {
                if (maxPeople.isNotEmpty()) {
                    if (number.isNotEmpty()) {
                        if (price.isNotEmpty()) {
                            progressBar.visibility = View.VISIBLE
                            finishButton.visibility = View.GONE

                            val hotelId = intent.getStringExtra("hotelId").toString()

                            val services = hashMapOf(
                                "hotelId" to hotelId,
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
                                                .child("HotelsData/$userUid/$hotelId/$number")

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

        if (imageUris.size >= 5) {
            finishButton.visibility = View.VISIBLE
            chipGroup.visibility = View.VISIBLE
        } else {
            finishButton.visibility = View.GONE
            chipGroup.visibility = View.GONE
        }
    }

    private fun changeVisibility() {
        progressBar.visibility = View.GONE
        finishButton.visibility = View.VISIBLE
    }
}