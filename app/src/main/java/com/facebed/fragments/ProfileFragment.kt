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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.facebed.R
import com.facebed.activities.CredentialManagerSingleton
import com.facebed.activities.SignInActivity
import com.facebed.controllers.Utils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private lateinit var spSignIn: SharedPreferences

    private lateinit var profileImage: CircleImageView

    private lateinit var cardViewProfile: CardView
    private lateinit var cardViewSettings: CardView

    private lateinit var settingsButton: ImageButton
    private lateinit var checkButton: ImageButton

    private lateinit var progressBarSettings: ProgressBar

    private lateinit var logoutButton: Button
    private lateinit var deleteButton: Button

    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var createdText: TextView

    private lateinit var editName: AutoCompleteTextView

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.profile_fragment, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImage = view.findViewById(R.id.profile_image)

        cardViewProfile = view.findViewById(R.id.cardview_profile)
        cardViewSettings = view.findViewById(R.id.cardview_settings)

        progressBarSettings = view.findViewById(R.id.progress_bar_settings)

        settingsButton = view.findViewById(R.id.settings_button)
        checkButton = view.findViewById(R.id.check_button)
        logoutButton = view.findViewById(R.id.logout_button)
        deleteButton = view.findViewById(R.id.delete_button)

        nameText = view.findViewById(R.id.name_text)
        emailText = view.findViewById(R.id.email_text)
        createdText = view.findViewById(R.id.created_text)

        editName = view.findViewById(R.id.edit_name_text)

        spSignIn = requireContext().getSharedPreferences("SignIn", Context.MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser

        nameText.text = user?.displayName
        emailText.text = user?.email
        editName.setText(user?.displayName)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        createdText.text = getString(R.string.account_created_on) + " " + sdf.format(Date(user!!.metadata!!.creationTimestamp))

        if (user.photoUrl != null) {
            Glide.with(requireContext())
                .load(user.photoUrl)
                .into(profileImage)
        } else {
            Glide.with(requireContext())
                .load(R.drawable.anon)
                .into(profileImage)
        }

        settingsButton.setOnClickListener {
            cardViewProfile.visibility = View.GONE
            cardViewSettings.visibility = View.VISIBLE
        }

        logoutButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                CredentialManagerSingleton.credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
                spSignIn.edit().clear().apply()
                startActivity(Intent(requireContext(), SignInActivity::class.java))
                requireActivity().finish()
            }
        }

        deleteButton.setOnClickListener {
            if (spSignIn.getBoolean("googleId", false)) {
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("182714962194-qmouil19bv70nkts9hd2cq1el7dvcc11.apps.googleusercontent.com")
                    .setAutoSelectEnabled(true)
                    .build()

                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = CredentialManagerSingleton.credentialManager.getCredential(
                            request = request,
                            context = requireContext()
                        )
                        val credential = result.credential
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        // Get an AuthCredential from the Google ID token
                        val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        user.reauthenticate(authCredential).addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                Toast.makeText(requireContext(),
                                    getString(R.string.account_deleted), Toast.LENGTH_SHORT).show()
                                proceedToDeleteAccount(spSignIn)
                            } else { Utils.error(requireContext()) }
                        }
                    } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_account, null)
                val passwordField = dialogView.findViewById<AutoCompleteTextView>(R.id.password_text)
                val confirmCheckBox = dialogView.findViewById<CheckBox>(R.id.check_box_confirm)
                val deleteButton = dialogView.findViewById<Button>(R.id.confirmation_delete_button)
                val progressBarConfirmation = dialogView.findViewById<ProgressBar>(R.id.progress_bar_delete_confirmation)

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
                        val credential = EmailAuthProvider.getCredential(emailText.text.toString(), password)

                        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                deleteButton.visibility = View.VISIBLE
                                progressBarConfirmation.visibility = View.GONE
                                Toast.makeText(requireContext(),
                                    getString(R.string.account_deleted), Toast.LENGTH_SHORT).show()
                                proceedToDeleteAccount(spSignIn)
                            } else {
                                deleteButton.visibility = View.VISIBLE
                                progressBarConfirmation.visibility = View.GONE
                                Toast.makeText(requireContext(), getString(R.string.bad_credentials),
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        deleteButton.visibility = View.VISIBLE
                        progressBarConfirmation.visibility = View.GONE
                        Toast.makeText(requireContext(), getString(R.string.password_and_checkbox),
                            Toast.LENGTH_SHORT).show()
                    }
                }

                dialog.show()
            }
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
                            cardViewProfile.visibility = View.VISIBLE
                            cardViewSettings.visibility = View.GONE

                            val userData = hashMapOf(
                                "name" to user.displayName,
                            )
                            FirebaseFirestore.getInstance().collection("User")
                                .document(user.uid).update(userData as Map<String, Any>)
                            editName.setText(user.displayName)
                            nameText.text = user.displayName

                            Toast.makeText(requireContext(), getString(R.string.updated_name),
                                Toast.LENGTH_SHORT).show()
                        } else {
                            checkButton.visibility = View.VISIBLE
                            progressBarSettings.visibility = View.GONE
                            cardViewProfile.visibility = View.VISIBLE
                            cardViewSettings.visibility = View.GONE
                            Utils.error(requireContext())
                        }
                    }
            } else {
                cardViewProfile.visibility = View.VISIBLE
                cardViewSettings.visibility = View.GONE
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openImagePicker()
            } else {
                Toast.makeText(requireContext(), getString(R.string.permission_not_granted),
                    Toast.LENGTH_SHORT).show()
            }
        }

        profileImage.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            } else {
                openImagePicker()
            }
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val selectedImageUri = data?.data

                if (selectedImageUri != null) {
                    profileImage.visibility = View.GONE

                    Glide.with(this)
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
                                        Toast.makeText(requireContext(), getString(R.string.updated_photo),
                                            Toast.LENGTH_SHORT).show()
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

    private fun proceedToDeleteAccount(spSignIn: SharedPreferences) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user?.uid != null) {
            try {
                FirebaseStorage.getInstance().reference.child("ProfileImages")
                    .child(user.uid).delete()
                FirebaseFirestore.getInstance().collection("User")
                    .document(user.uid).delete()
            } finally {
                user.delete().addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        spSignIn.edit().clear().apply()
                        startActivity(Intent(requireContext(), SignInActivity::class.java))
                        requireActivity().finish()
                    } else { Utils.error(requireContext()) }
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }
}