package com.facebed.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.facebed.R
import com.facebed.activities.SignInActivity
import com.facebed.controllers.FirebaseController
import com.facebed.controllers.Utils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Date
import java.util.Locale

class ProfileCompanyFragment : Fragment() {
    private lateinit var spSignIn: SharedPreferences

    private lateinit var profileImage: CircleImageView

    private lateinit var cardViewProfileCompany: CardView
    private lateinit var cardViewSettingsCompany: CardView

    private lateinit var settingsButton: ImageButton
    private lateinit var checkButton: ImageButton

    private lateinit var progressBarSettings: ProgressBar

    private lateinit var logoutButton: Button
    private lateinit var deleteButton: Button

    private lateinit var nameText: TextView
    private lateinit var walletText: TextView
    private lateinit var emailText: TextView
    private lateinit var createdText: TextView

    private lateinit var editName: AutoCompleteTextView

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.profile_company_fragment, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImage = view.findViewById(R.id.profile_image)

        cardViewProfileCompany = view.findViewById(R.id.cardview_profile_company)
        cardViewSettingsCompany = view.findViewById(R.id.cardview_settings)

        progressBarSettings = view.findViewById(R.id.progress_bar_settings)

        settingsButton = view.findViewById(R.id.settings_button)
        checkButton = view.findViewById(R.id.check_button)
        logoutButton = view.findViewById(R.id.logout_button)
        deleteButton = view.findViewById(R.id.delete_button)

        nameText = view.findViewById(R.id.name_text)
        walletText = view.findViewById(R.id.wallet_text)
        emailText = view.findViewById(R.id.email_text)
        createdText = view.findViewById(R.id.created_text)

        editName = view.findViewById(R.id.edit_name_text)

        spSignIn = requireContext().getSharedPreferences("SignIn", Context.MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser

        nameText.text = user?.displayName

        val userRef = FirebaseFirestore.getInstance()
            .collection("User").document(user!!.uid)
        userRef.get().addOnSuccessListener { document: DocumentSnapshot ->
            val earnings = document.getDouble("earnings")
            walletText.text = earnings.toString()
        }


        emailText.text = user.email
        editName.setText(user.displayName)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        createdText.text =
            getString(R.string.account_created_on) + " " + sdf.format(Date(user.metadata!!.creationTimestamp))

        if (user.photoUrl != null) {
            Glide.with(requireContext())
                .load(user.photoUrl)
                .into(profileImage)
        } else {
            Glide.with(requireContext())
                .load(R.drawable.icon)
                .into(profileImage)
        }

        settingsButton.setOnClickListener {
            cardViewProfileCompany.visibility = View.GONE
            cardViewSettingsCompany.visibility = View.VISIBLE
        }

        logoutButton.setOnClickListener {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()
            spSignIn.edit().clear().apply()
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
        }

        deleteButton.setOnClickListener {
            val dialogView =
                LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_delete_account, null)
            val passwordField =
                dialogView.findViewById<AutoCompleteTextView>(R.id.password_text)
            val confirmCheckBox = dialogView.findViewById<CheckBox>(R.id.check_box_confirm)
            val deleteButton = dialogView.findViewById<Button>(R.id.confirmation_delete_button)
            val progressBarConfirmation =
                dialogView.findViewById<ProgressBar>(R.id.progress_bar_delete_confirmation)

            Utils.showPassword(passwordField)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()

            deleteButton.setOnClickListener {
                deleteButton.visibility = View.GONE
                progressBarConfirmation.visibility = View.VISIBLE
                val password = passwordField.text.toString()
                val isConfirmed = confirmCheckBox.isChecked

                if (isConfirmed && password.isNotEmpty()) {
                    val credential =
                        EmailAuthProvider.getCredential(emailText.text.toString(), password)

                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            deleteButton.visibility = View.VISIBLE
                            progressBarConfirmation.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.account_deleted), Toast.LENGTH_SHORT
                            ).show()
                            try {
                                FirebaseStorage.getInstance().reference.child("ProfileImages")
                                    .child(user.uid).delete()
                                FirebaseFirestore.getInstance().collection("User")
                                    .document(user.uid).delete()
                                val firestore = FirebaseFirestore.getInstance()
                                FirebaseController.deleteFirebaseData(firestore.collection("Hotels"), user.uid)
                                FirebaseController.deleteFirebaseData(firestore.collection("Rooms"), user.uid)
                                FirebaseController.deleteFirebaseData(firestore.collection("HotelServices"), user.uid)
                                FirebaseController.deleteFirebaseData(firestore.collection("RoomServices"), user.uid)
                            } finally {
                                user.delete().addOnCompleteListener { deleteTask ->
                                    if (deleteTask.isSuccessful) {
                                        spSignIn.edit().clear().apply()
                                        startActivity(
                                            Intent(
                                                requireContext(),
                                                SignInActivity::class.java
                                            )
                                        )
                                        requireActivity().finish()
                                    } else { Utils.error(requireContext()) }
                                }
                            }
                        } else {
                            deleteButton.visibility = View.VISIBLE
                            progressBarConfirmation.visibility = View.GONE
                            Toast.makeText(
                                requireContext(), getString(R.string.bad_credentials),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    deleteButton.visibility = View.VISIBLE
                    progressBarConfirmation.visibility = View.GONE
                    Toast.makeText(
                        requireContext(), getString(R.string.password_and_checkbox),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            dialog.show()
        }

        checkButton.setOnClickListener {
            if (editName.text.trim().toString() != user.displayName.toString()) {
                progressBarSettings.visibility = View.VISIBLE
                checkButton.visibility = View.GONE

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(editName.text.trim().toString())
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            checkButton.visibility = View.VISIBLE
                            progressBarSettings.visibility = View.GONE
                            cardViewProfileCompany.visibility = View.VISIBLE
                            cardViewSettingsCompany.visibility = View.GONE

                            val userData = hashMapOf(
                                "name" to user.displayName,
                            )
                            FirebaseFirestore.getInstance().collection("User")
                                .document(user.uid).update(userData as Map<String, Any>)
                            editName.setText(user.displayName)
                            nameText.text = user.displayName

                            Toast.makeText(
                                requireContext(), getString(R.string.updated_name),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            checkButton.visibility = View.VISIBLE
                            progressBarSettings.visibility = View.GONE
                            cardViewProfileCompany.visibility = View.VISIBLE
                            cardViewSettingsCompany.visibility = View.GONE
                            Utils.error(requireContext())
                        }
                    }
            } else {
                cardViewProfileCompany.visibility = View.VISIBLE
                cardViewSettingsCompany.visibility = View.GONE
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                imagePickerLauncher.launch(intent)
            } else {
                Toast.makeText(
                    requireContext(), getString(R.string.permission_not_granted),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        profileImage.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(permission)
            } else {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                imagePickerLauncher.launch(intent)
            }
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val selectedImageUri = data?.data

                if (selectedImageUri != null) {
                    profileImage.visibility = View.GONE

                    Glide.with(requireContext())
                        .load(selectedImageUri)
                        .into(profileImage)

                    val storageRef =
                        FirebaseStorage.getInstance().reference.child("ProfileImages")
                            .child(FirebaseAuth.getInstance().currentUser?.uid.toString())
                    val uploadTask = storageRef.putFile(selectedImageUri)

                    uploadTask.addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setPhotoUri(uri)
                                .build()

                            FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(), getString(R.string.updated_photo),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        profileImage.visibility = View.VISIBLE
                                    } else {
                                        Utils.error(requireContext())
                                        profileImage.visibility = View.VISIBLE

                                        Glide.with(this)
                                            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
                                            .into(profileImage)
                                    }
                                }
                        }
                    }.addOnFailureListener {
                        profileImage.visibility = View.VISIBLE
                        Utils.error(requireContext())
                    }
                }
            }
        }
    }
}