package com.facebed

import android.app.ActivityOptions
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.credentials.CredentialManager
import android.os.Bundle
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

object CredentialManagerSingleton {
    lateinit var credentialManager: CredentialManager
}

class SignInActivity : AppCompatActivity() {
    private lateinit var spSignIn: SharedPreferences

    private lateinit var emailText: AutoCompleteTextView
    private lateinit var passwordText: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in_activity)
        enableEdgeToEdge()

        spSignIn = getSharedPreferences("SignIn", Context.MODE_PRIVATE)
        // Initialize the credentialManager property
        CredentialManagerSingleton.credentialManager = CredentialManager.create(this@SignInActivity)

        emailText = findViewById(R.id.email_text)
        passwordText = findViewById(R.id.password_text)

        // Linear Gradient for the title
        val appTitle: TextView = findViewById(R.id.app_title)
        val width = appTitle.paint.measureText(appTitle.text.toString())
        val textShader: Shader = LinearGradient(0f, 0f, width, appTitle.textSize, intArrayOf(
            Color.parseColor("#F66C2C"),
            Color.parseColor("#F6A228")
        ), null, Shader.TileMode.REPEAT)
        appTitle.paint.setShader(textShader)

        // Check if user is already signed in
        if (spSignIn.getBoolean("isSignedIn", false)) {
            val isCompany = spSignIn.getBoolean("isCompany", false)
            val homeActivityIntent = if (isCompany) {
                Intent(this@SignInActivity, HomeCompanyActivity::class.java)
            } else {
                Intent(this@SignInActivity, HomeActivity::class.java)
            }
            startActivity(homeActivityIntent)
            finish() // Finish the SignInActivity to prevent user from going back
        }

        val googleSignInButton: Button = findViewById(R.id.google_sign_in_button)
        googleSignInButton.setOnClickListener {
            googleSignIn()
        }

        val emailSignInButton: Button = findViewById(R.id.email_sign_in_button)
        emailSignInButton.setOnClickListener {
            emailSignIn()
        }

        val createAccount: TextView = findViewById(R.id.text_create_account)
        createAccount.setOnClickListener {
            // Start the new activity and lets user go back
            startActivity(Intent(this@SignInActivity, RegisterActivity::class.java),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        val registerCompany: TextView = findViewById(R.id.text_here)
        registerCompany.setOnClickListener {
            // Start the new activity and lets user go back
            startActivity(Intent(this@SignInActivity, RegisterCompanyActivity::class.java),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    private fun googleSignIn() {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") {str, it -> str + "%02x".format(it)}

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("182714962194-qmouil19bv70nkts9hd2cq1el7dvcc11.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = CredentialManagerSingleton.credentialManager.getCredential(
                    request = request,
                    context = this@SignInActivity
                )
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                // Get an AuthCredential from the Google ID token
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                // Register credentials in FirebaseAuth
                FirebaseAuth.getInstance().signInWithCredential(authCredential).addOnSuccessListener {authResult ->
                    val user = FirebaseAuth.getInstance().currentUser
                    val userId = authResult.user?.uid
                    userId?.let {
                        // Guarda la información del usuario en la base de datos
                        val userData = hashMapOf(
                            "email" to user?.email,
                            "name" to user?.displayName,
                            "isCompany" to false
                        )
                        FirebaseDatabase.getInstance().getReference("user").child(it).setValue(userData)
                            .addOnSuccessListener {
                                // Save the sign-in state
                                spSignIn.edit {
                                    putBoolean("isSignedIn", true)
                                    putBoolean("isCompany", false)
                                }

                                startActivity(Intent(this@SignInActivity, HomeActivity::class.java),
                                    ActivityOptions.makeSceneTransitionAnimation(this@SignInActivity).toBundle())
                                finish() // Finish the SignInActivity to prevent user from going back

                                Toast.makeText(this@SignInActivity,
                                    getString(R.string.welcome_toast) + " " + user?.displayName.toString(),
                                    Toast.LENGTH_SHORT).show()

                            }.addOnFailureListener { Toast.makeText(this@SignInActivity, getString(R.string.error), Toast.LENGTH_SHORT).show() } }

                }.addOnFailureListener { Toast.makeText(this@SignInActivity,
                        getString(R.string.error), Toast.LENGTH_SHORT).show() }

                Log.i(TAG, googleIdTokenCredential.displayName.toString())
                Log.i(TAG, googleIdTokenCredential.id.toString())
                Log.i(TAG, googleIdTokenCredential.idToken.toString())
                Log.i(TAG, googleIdTokenCredential.familyName.toString())
                Log.i(TAG, googleIdTokenCredential.givenName.toString())
                Log.i(TAG, googleIdTokenCredential.phoneNumber.toString())
                Log.i(TAG, googleIdTokenCredential.profilePictureUri.toString())

            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                Toast.makeText(this@SignInActivity, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(this@SignInActivity, e.message, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun emailSignIn() {
        if (emailText.text.trim().toString().isNotEmpty()) {
            if (passwordText.text.trim().toString().isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    emailText.text.trim().toString(), passwordText.text.trim().toString())
                    .addOnSuccessListener { authResult ->
                        val userRef = FirebaseDatabase.getInstance().getReference("user").child(authResult.user!!.uid)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val isCompany = dataSnapshot.child("isCompany").getValue(Boolean::class.java)
                                if (isCompany != null) {
                                    if (isCompany) {
                                        // Usuario es una empresa, redirige a HomeCompanyActivity
                                        startActivity(Intent(this@SignInActivity, HomeCompanyActivity::class.java))
                                        spSignIn.edit { putBoolean("isCompany", true) }
                                    } else {
                                        // Usuario no es una empresa, redirige a HomeActivity
                                        startActivity(Intent(this@SignInActivity, HomeActivity::class.java))
                                        spSignIn.edit { putBoolean("isCompany", false) }
                                    }
                                    // Save the sign-in state
                                    spSignIn.edit { putBoolean("isSignedIn", true) }
                                    finish() // Finish the SignInActivity to prevent user from going back
                                } else {
                                    Toast.makeText(this@SignInActivity, "Error obteniendo información de usuario", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Toast.makeText(this@SignInActivity, "Error obteniendo información de usuario", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, getString(R.string.bad_credentials), Toast.LENGTH_SHORT).show()
                    }
            } else { passwordText.error = getString(R.string.required) }
        } else { emailText.error = getString(R.string.required) }
    }

}