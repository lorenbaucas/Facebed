package com.facebed.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

class AddHotelActivity : AppCompatActivity() {
    private lateinit var stars: Array<ImageView>

    private lateinit var hotelNameText: AutoCompleteTextView
    private lateinit var locationText: AutoCompleteTextView
    private lateinit var descriptionText: AutoCompleteTextView

    private lateinit var addPhotosButton: Button
    private lateinit var nextButton: Button
    private lateinit var finishButton: Button

    private lateinit var progressBar: ProgressBar

    private var starCount: Int = 0

    private lateinit var chipGroup: ChipGroup

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val imageUris: MutableList<Uri> = mutableListOf()
    private lateinit var imagesAdapter: AddImagesAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var initialHotelData: HashMap<String, Any?>
    private lateinit var initialServicesData: HashMap<String, Any?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_hotel_activity)

        hotelNameText = findViewById(R.id.primary_text)
        locationText = findViewById(R.id.secondary_text)
        descriptionText = findViewById(R.id.description_text)

        addPhotosButton = findViewById(R.id.add_photos_button)
        nextButton = findViewById(R.id.next_button)
        finishButton = findViewById(R.id.finish_button)

        progressBar = findViewById(R.id.progress_bar)

        chipGroup = findViewById(R.id.chip_group)

        recyclerView = findViewById(R.id.rv_images)

        val firestore = FirebaseFirestore.getInstance()
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        imagesAdapter = AddImagesAdapter(imageUris)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = imagesAdapter

        val hotelNameExists = intent.getStringExtra("hotelName")
        if (!hotelNameExists.isNullOrEmpty()) {
            val hotelsCollectionRef = firestore.collection("Hotels")
            hotelsCollectionRef
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("hotelName", hotelNameExists)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        val hotelId = documentSnapshot.id

                        val hotelName = documentSnapshot.getString("hotelName")
                        val location = documentSnapshot.getString("location")
                        val description = documentSnapshot.getString("description")
                        val stars = documentSnapshot.getLong("stars")

                        hotelNameText.setText(hotelName)
                        locationText.setText(location)
                        descriptionText.setText(description)
                        changeStars(stars!!.toInt())

                        initialHotelData = hashMapOf(
                            "hotelName" to hotelName,
                            "location" to location,
                            "stars" to stars,
                            "description" to description,
                            "userUid" to userUid
                        )

                        val storageRef = FirebaseStorage.getInstance().reference
                            .child("HotelsData/$userUid/$hotelId/MainPhotos")

                        FirebaseController.getImages(storageRef, imageUris) {
                            imagesAdapter.notifyDataSetChanged()
                            finishButton.visibility = View.VISIBLE
                        }

                        FirebaseController.getHotelServices(userUid!!, hotelId) { documentSnapshot ->
                            chipGroup.visibility = View.VISIBLE
                            val servicesData = hashMapOf<String, Any?>()

                            documentSnapshot?.data?.forEach { (key, value) ->
                                if (key != "userUid" || key != "hotelName") {
                                    val chip = chipGroup.findViewWithTag<Chip>(key)
                                    chip?.isChecked = value as Boolean
                                    servicesData[key] = value
                                }
                            }
                            initialServicesData = servicesData
                        }
                    }
                }
        }

        finishButton.setOnClickListener {
            val updatedHotelData = hashMapOf<String, Any?>(
                "hotelName" to hotelNameText.text.toString().trim(),
                "location" to locationText.text.toString().trim(),
                "stars" to starCount,
                "description" to descriptionText.text.toString().trim(),
                "userUid" to userUid
            )

            if (imageUris.size >= 5) {
                progressBar.visibility = View.VISIBLE
                finishButton.visibility = View.GONE
                val hotelName = initialHotelData["hotelName"] as String
                var hotelId: String?

                FirebaseController.getHotel(userUid!!, hotelName) { documentSnapshot ->
                    documentSnapshot?.let {
                        hotelId = it.id
                        val hotelDocumentRef = it.reference

                        val updateHotelDataTask = hotelDocumentRef.update(updatedHotelData).addOnSuccessListener {
                            val storageRef = FirebaseStorage.getInstance().reference
                                .child("HotelsData/$userUid/$hotelId/MainPhotos")

                            FirebaseController.uploadImages(storageRef, imageUris) {}
                        }.addOnFailureListener { Utils.error(this) }

                        updateHotelDataTask.addOnSuccessListener {
                            val updatedServicesData = getServices(userUid, hotelId!!)

                            if (initialServicesData != updatedServicesData) {
                                val updatedHotelName = updatedHotelData["hotelName"] as String
                                FirebaseController.getHotelServices(userUid, hotelId!!) { documentSnapshot ->
                                    documentSnapshot?.let {
                                        val servicesDocumentRef = it.reference
                                        val servicesDataToUpdate = hashMapOf<String, Any?>()
                                        updatedServicesData.forEach { (key, value) ->
                                            if (key != "userUid" && key != "hotelName") {
                                                servicesDataToUpdate[key] = value
                                            }
                                        }
                                        servicesDataToUpdate["hotelName"] = updatedHotelName
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

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openImagePicker()
            } else {
                Toast.makeText(
                    this, getString(R.string.permission_not_granted),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        addPhotosButton.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(permission)
            } else { openImagePicker() }
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) { handleImagePickerResult(result) }
        }

        nextButton.setOnClickListener {
            val hotelName = hotelNameText.text.toString().trim()
            val location = locationText.text.toString().trim()
            val description = descriptionText.text.toString().trim()

            if (hotelName.isNotEmpty()) {
                if (location.isNotEmpty()) {
                    if (starCount > 0) {
                        progressBar.visibility = View.VISIBLE
                        nextButton.visibility = View.GONE

                        val hotelData = hashMapOf(
                            "userUid" to userUid,
                            "hotelName" to hotelName,
                            "location" to location,
                            "stars" to starCount,
                            "description" to description
                        )

                        val hotelDocumentRef =
                            firestore.collection("Hotels").document()

                        hotelDocumentRef.set(hotelData)
                            .addOnSuccessListener {
                                val hotelId = hotelDocumentRef.id
                                val storageRef = FirebaseStorage.getInstance().reference
                                    .child("HotelsData/$userUid/$hotelId/MainPhotos")

                                FirebaseController.uploadImages(storageRef, imageUris) {
                                    val services = getServices(userUid!!, hotelId)

                                    val servicesDocumentRef =
                                        firestore.collection("HotelServices").document()
                                    servicesDocumentRef.set(services)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, getString(R.string.data_saved_successfully),
                                                Toast.LENGTH_SHORT).show()
                                            startActivity(
                                                Intent(this, AddRoomActivity::class.java)
                                                    .putExtra("hotelId", hotelId)
                                            )
                                            progressBar.visibility = View.GONE
                                            nextButton.visibility = View.VISIBLE

                                            finish()
                                        }
                                        .addOnFailureListener {
                                            progressBar.visibility = View.GONE
                                            nextButton.visibility = View.VISIBLE
                                            Utils.error(this)
                                        }
                                }
                            }
                    } else { Toast.makeText(this, getString(R.string.select_stars),
                        Toast.LENGTH_SHORT).show() }
                } else { locationText.error = getString(R.string.provide_location) }
            } else { hotelNameText.error = getString(R.string.provide_hotel_name) }
        }

        stars = arrayOf(
            findViewById(R.id.star1),
            findViewById(R.id.star2),
            findViewById(R.id.star3),
            findViewById(R.id.star4),
            findViewById(R.id.star5)
        )

        for ((index, star) in stars.withIndex()) {
            star.setOnClickListener { changeStars(index + 1) }
        }
    }

    private fun changeStars(count: Int) {
        for (star in stars) {
            star.setImageResource(R.drawable.baseline_star_border_24)
        }

        for (i in 0 until count) {
            stars[i].setImageResource(R.drawable.baseline_star_24)
        }

        starCount = count
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun handleImagePickerResult(result: ActivityResult) {
        val data = result.data
        data?.clipData?.let {
            for (i in 0 until it.itemCount) {
                imageUris.add(it.getItemAt(i).uri)
            }
        }

        data?.data?.let { imageUris.add(it) }

        val hotelNameExists = intent.getStringExtra("hotelName")

        if (imageUris.size >= 5) {
            if (!hotelNameExists.isNullOrEmpty()) {
                nextButton.visibility = View.GONE
                chipGroup.visibility = View.VISIBLE
            } else {
                nextButton.visibility = View.VISIBLE
                chipGroup.visibility = View.VISIBLE
            }
        } else {
            nextButton.visibility = View.GONE
            chipGroup.visibility = View.GONE
        }

        imagesAdapter.notifyDataSetChanged()
    }

    private fun getServices(userUid: String, hotelId: String): HashMap<String, Any> {
        return hashMapOf(
            "userUid" to userUid,
            "hotelId" to hotelId,
            "swimming_pool" to findViewById<Chip>(R.id.swimming_pool).isChecked,
            "restaurant" to findViewById<Chip>(R.id.restaurant).isChecked,
            "spa" to findViewById<Chip>(R.id.spa).isChecked,
            "adults_only" to findViewById<Chip>(R.id.adults_only).isChecked,
            "gym" to findViewById<Chip>(R.id.gym).isChecked,
            "water_park" to findViewById<Chip>(R.id.water_park).isChecked,
            "bowling" to findViewById<Chip>(R.id.bowling).isChecked,
            "padel_courts" to findViewById<Chip>(R.id.padel_courts).isChecked,
            "seafront" to findViewById<Chip>(R.id.seafront).isChecked,
            "rural" to findViewById<Chip>(R.id.rural).isChecked
        )
    }
}