package com.facebed.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.facebed.R
import com.facebed.controllers.Utils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

object CredentialManagerSingleton {
    lateinit var credentialManager: CredentialManager
}

class SignInActivity : AppCompatActivity() {
    private lateinit var spSignIn: SharedPreferences
    private lateinit var progressBarEmail: ProgressBar
    private lateinit var progressBarGoogle: ProgressBar

    private lateinit var googleSignInButton: Button
    private lateinit var emailSignInButton: Button

    private lateinit var emailText: AutoCompleteTextView
    private lateinit var passwordText: AutoCompleteTextView

    private lateinit var wait: TextView
    private lateinit var forgotPassword: TextView
    private lateinit var createAccount: TextView
    private lateinit var registerCompany: TextView

    private var signInCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in_activity)

        googleSignInButton = findViewById(R.id.google_sign_in_button)
        emailSignInButton = findViewById(R.id.email_sign_in_button)

        emailText = findViewById(R.id.email_text)
        passwordText = findViewById(R.id.password_text)

        wait = findViewById(R.id.wait_text)
        forgotPassword = findViewById(R.id.text_forgot_password)
        createAccount = findViewById(R.id.text_create_account)
        registerCompany = findViewById(R.id.text_here)

        progressBarEmail = findViewById(R.id.progress_bar_email)
        progressBarGoogle = findViewById(R.id.progress_bar_google)

        spSignIn = getSharedPreferences("SignIn", Context.MODE_PRIVATE)
        // Initialize the credentialManager property
        CredentialManagerSingleton.credentialManager = CredentialManager.create(this@SignInActivity)

        // Linear Gradient for the title
        val appTitle: TextView = findViewById(R.id.app_title)
        Utils.paintTitle(appTitle, "#F66C2C", "#F6A228")

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

        Utils.showPassword(passwordText)

        googleSignInButton.setOnClickListener {
            googleSignInButton.visibility = View.GONE
            progressBarGoogle.visibility = View.VISIBLE
            googleSignIn()
        }

        emailSignInButton.setOnClickListener {
            emailSignInButton.visibility = View.GONE
            progressBarEmail.visibility = View.VISIBLE
            emailSignIn()
        }

        forgotPassword.setOnClickListener {
            val email = emailText.text.trim().toString()
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.send_recovery))
                .setMessage(email)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    Toast.makeText(this, getString(R.string.sent_recovery), Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }

        createAccount.setOnClickListener {
            // Start the new activity and lets user go back
            startActivity(Intent(this@SignInActivity, RegisterActivity::class.java),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

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

                spSignIn.edit { putBoolean("googleId", true) }

                // Register credentials in FirebaseAuth
                FirebaseAuth.getInstance().signInWithCredential(authCredential)
                    .addOnSuccessListener {authResult ->
                    val user = FirebaseAuth.getInstance().currentUser
                    val userId = authResult.user?.uid
                    userId?.let {
                        // Guarda la información del usuario en la base de datos
                        val userData = hashMapOf(
                            "email" to user?.email,
                            "name" to user?.displayName,
                            "isCompany" to false
                        )

                        FirebaseFirestore.getInstance().collection("User")
                            .document(userId).set(userData)
                            .addOnSuccessListener {
                                // Guarda el estado de inicio de sesión
                                spSignIn.edit {
                                    putBoolean("isSignedIn", true)
                                    putBoolean("isCompany", false)
                                }

                                startActivity(
                                    Intent(this@SignInActivity, HomeActivity::class.java),
                                    ActivityOptions.makeSceneTransitionAnimation(this@SignInActivity).toBundle()
                                )
                                finish()

                                Toast.makeText(
                                    this@SignInActivity,
                                    getString(R.string.welcome_toast) + " " + user?.displayName,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Utils.error(this@SignInActivity)
                                loadingGoogle()}
                    }
                }.addOnFailureListener {
                    Utils.error(this@SignInActivity)
                    loadingGoogle()}
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                Toast.makeText(this@SignInActivity, e.message, Toast.LENGTH_SHORT).show()
                loadingGoogle()
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(this@SignInActivity, e.message, Toast.LENGTH_SHORT).show()
                loadingGoogle()} }
    }

    private fun emailSignIn() {
        val email = emailText.text.trim().toString()
        val password = passwordText.text.trim().toString()

        if (Utils.isEmailValid(email)) {
            if (Utils.isPasswordValid(password)) {
                val auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            if (user.isEmailVerified) {
                                val userRef = FirebaseFirestore.getInstance()
                                    .collection("User").document(user.uid)

                                userRef.get().addOnSuccessListener { document: DocumentSnapshot ->
                                    val isCompany = document.getBoolean("isCompany")
                                    if (isCompany != null) {
                                        if (isCompany) {
                                            startActivity(Intent(this@SignInActivity,
                                                HomeCompanyActivity::class.java))
                                            spSignIn.edit { putBoolean("isCompany", true) }
                                        } else {
                                            startActivity(Intent(this@SignInActivity,
                                                HomeActivity::class.java))
                                            spSignIn.edit { putBoolean("isCompany", false) }
                                        }
                                        spSignIn.edit { putBoolean("isSignedIn", true) }

                                        Toast.makeText(
                                            this@SignInActivity,
                                            getString(R.string.welcome_toast) + " " + user.displayName.toString(),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        finish()
                                    }
                                    loadingEmail()
                                }
                            } else {
                                user.sendEmailVerification().addOnCompleteListener {
                                    emailSignInButton.visibility = View.GONE
                                    wait.visibility = View.VISIBLE
                                    signInCounter++
                                    if (signInCounter == 1) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            for (i in 10 downTo 1) { wait(i) }

                                            emailSignInButton.visibility = View.VISIBLE
                                            wait.visibility = View.GONE
                                            signInCounter++
                                        }
                                    } else if (signInCounter == 3) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            for (i in 30 downTo 1) { wait(i) }

                                            emailSignInButton.visibility = View.VISIBLE
                                            wait.visibility = View.GONE
                                            signInCounter = 0
                                        }
                                    }

                                    Toast.makeText(
                                        this@SignInActivity,
                                        getString(R.string.email_verification),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                loadingEmail()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this@SignInActivity,
                            getString(R.string.bad_credentials),
                            Toast.LENGTH_SHORT
                        ).show()

                        loadingEmail()
                    }
            } else {
                passwordText.error = getString(R.string.password_not_valid)
                loadingEmail() }
        } else {
            emailText.error = getString(R.string.email_not_valid)
            loadingEmail() }
    }


    private fun loadingEmail() {
        emailSignInButton.visibility = View.VISIBLE
        progressBarEmail.visibility = View.GONE
    }

    private fun loadingGoogle() {
        googleSignInButton.visibility = View.VISIBLE
        progressBarGoogle.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    private suspend fun wait(seconds: Int) {
        wait.text = getString(R.string.wait) + " " + seconds
        delay(1000)
    }
}