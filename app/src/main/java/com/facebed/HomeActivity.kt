package com.facebed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {
    private lateinit var spSignIn: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        spSignIn = getSharedPreferences("SignIn", Context.MODE_PRIVATE)

        val user = FirebaseAuth.getInstance().currentUser

        val buttonLogout: Button = findViewById(R.id.button_logout)
        buttonLogout.setOnClickListener {

            CoroutineScope(Dispatchers.Main).launch {
                CredentialManagerSingleton.credentialManager.clearCredentialState(ClearCredentialStateRequest())
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
                spSignIn.edit { putBoolean("isSignedIn", false) }
                startActivity(Intent(applicationContext, SignInActivity::class.java))
                finish() // Finish the HomeActivity to prevent user from going back
            }
        }

        val buttonDeleteAccount: Button = findViewById(R.id.button_delete_account)
        buttonDeleteAccount.setOnClickListener {

            user?.delete()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AQUI", "User account deleted.")
                        // Clear the sign-in state
                        spSignIn.edit { putBoolean("isSignedIn", false) }
                        startActivity(Intent(this@HomeActivity, SignInActivity::class.java))
                        finish() // Finish the HomeActivity to prevent user from going back
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("AQUI", "deleteUser:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }
                }

        }

    }
}