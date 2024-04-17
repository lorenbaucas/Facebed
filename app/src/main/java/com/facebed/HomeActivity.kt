package com.facebed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {
    private lateinit var spSignIn: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        spSignIn = getSharedPreferences("SignIn", Context.MODE_PRIVATE)

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
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                FirebaseDatabase.getInstance().getReference("user").child(user.uid).removeValue()
                    .addOnSuccessListener {
                        user.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    spSignIn.edit { putBoolean("isSignedIn", false) }
                                    startActivity(Intent(this@HomeActivity, SignInActivity::class.java))
                                    finish()
                                } else { Toast.makeText(baseContext, getString(R.string.error), Toast.LENGTH_SHORT).show() } } }
                    .addOnFailureListener { Toast.makeText(baseContext, getString(R.string.error), Toast.LENGTH_SHORT).show() }
            } else { Toast.makeText(baseContext, getString(R.string.not_signed_in), Toast.LENGTH_SHORT).show() }
        }
    }
}