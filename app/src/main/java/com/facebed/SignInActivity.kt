package com.facebed

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class SignInActivity : ComponentActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in_activity)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Check if user is already signed in
        if (sharedPreferences.getBoolean("isSignedIn", false)) {
            startActivity(Intent(this@SignInActivity, HomeActivity::class.java))
            finish() // Finish the SignInActivity to prevent user from going back
        }

        val signInButton: Button = findViewById(R.id.sign_in_button)
        signInButton.setOnClickListener {
            googleSignIn()
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
            .setServerClientId("182714962194-55q1uderpr4ba9kq899koqnmff5f889d.apps.googleusercontent.com")
            .setNonce(hashedNonce)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@SignInActivity)
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@SignInActivity
                )
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.displayName

                Log.i(TAG, googleIdToken.toString())

                // Save the sign-in state
                sharedPreferences.edit { putBoolean("isSignedIn", true)}

                startActivity(Intent(this@SignInActivity, HomeActivity::class.java))
                finish() // Finish the SignInActivity to prevent user from going back

                Toast.makeText(this@SignInActivity, getString(R.string.welcome_toast) + " " + googleIdToken, Toast.LENGTH_SHORT).show()
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                //Toast.makeText(this@SignInActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.i(TAG, e.message.toString())
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(this@SignInActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}