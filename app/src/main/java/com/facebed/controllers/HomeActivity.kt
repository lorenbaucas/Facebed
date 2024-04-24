package com.facebed.controllers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.fragment.app.Fragment
import com.facebed.R
import com.facebed.databinding.HomeActivityBinding
import com.facebed.fragments.EventsFragment
import com.facebed.fragments.HomeFragment
import com.facebed.fragments.ProfileFragment
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var spSignIn: SharedPreferences
    private lateinit var homeActivityBinding: HomeActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)
        enableEdgeToEdge()

        homeActivityBinding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(homeActivityBinding.root)
        homeActivityBinding.bottomNavigation.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
        replaceFragment(HomeFragment())

        homeActivityBinding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> replaceFragment(HomeFragment())
                R.id.navigation_events -> replaceFragment(EventsFragment())
                R.id.navigation_profile -> replaceFragment(ProfileFragment())
                else -> {}
            }
            true
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    /*spSignIn = getSharedPreferences("SignIn", Context.MODE_PRIVATE)

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
}*/
}