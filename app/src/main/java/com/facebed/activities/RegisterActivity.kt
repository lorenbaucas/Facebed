package com.facebed.activities

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
import androidx.appcompat.app.AppCompatActivity
import com.facebed.R
import com.facebed.controllers.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var spSignIn: SharedPreferences

    private lateinit var progressBar: ProgressBar

    private lateinit var registerButton: Button

    private lateinit var emailText: AutoCompleteTextView
    private lateinit var nameText: AutoCompleteTextView
    private lateinit var passwordText: AutoCompleteTextView
    private lateinit var confirmationText: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        /*val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (...) {

                } else {
                    // Si viewVerify no es visible, permite el comportamiento normal de retroceso
                    isEnabled = false // Desactivar el callback para permitir la acción predeterminada
                    onBackPressedDispatcher.onBackPressed() // Llamar al evento predeterminado
                }
            }
        }

        // Añadir el callback al dispatcher
        this.onBackPressedDispatcher.addCallback(this, callback)*/

        spSignIn = getSharedPreferences("SignIn", Context.MODE_PRIVATE)

        emailText = findViewById(R.id.email_text)
        nameText = findViewById(R.id.name_text)
        passwordText = findViewById(R.id.password_text)
        confirmationText = findViewById(R.id.confirmation_text)

        progressBar = findViewById(R.id.progress_bar)

        registerButton = findViewById(R.id.register_button)

        // Linear Gradient for the title
        val appTitle: TextView = findViewById(R.id.app_title)
        Utils.paintTitle(appTitle, "#F66C2C", "#F6A228")

        Utils.showPassword(passwordText)
        Utils.showPassword(confirmationText)

        registerButton.setOnClickListener {
            registerButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            emailRegister()
        }
    }

    private fun emailRegister() {
        val email = emailText.text.trim().toString()
        val password = passwordText.text.trim().toString()
        val confirmation = confirmationText.text.trim().toString()
        val name = nameText.text.trim().toString()

        if (Utils.isEmailValid(email)) {
            if (name.isNotEmpty()) {
                if (Utils.isPasswordValid(password)) {
                    if (password == confirmation) {
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val user = FirebaseAuth.getInstance().currentUser
                                user?.sendEmailVerification()?.addOnCompleteListener {
                                    val userData = hashMapOf(
                                        "email" to email,
                                        "name" to name,
                                        "isCompany" to false
                                    )

                                    FirebaseFirestore.getInstance().collection("User").document(user.uid).set(userData)
                                        .addOnSuccessListener {
                                            val profileUpdates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(name).build()
                                            user.updateProfile(profileUpdates)
                                                .addOnCompleteListener { profileUpdateTask ->
                                                    if (profileUpdateTask.isSuccessful) {
                                                        Toast.makeText(this, getString(R.string.please_verify),
                                                            Toast.LENGTH_LONG).show()

                                                        loadingRegister()

                                                        startActivity(
                                                            Intent(this, SignInActivity::class.java),
                                                            ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                                                        )
                                                        finish()
                                                    } else { Utils.error(this) } }
                                        }
                                        .addOnFailureListener {
                                            Utils.error(this)
                                            loadingRegister() } }
                            }
                            .addOnFailureListener {
                                emailText.error = getString(R.string.email_used)
                                Toast.makeText(this, getString(R.string.email_used), Toast.LENGTH_SHORT).show()
                                loadingRegister() }
                    } else {
                        confirmationText.error = getString(R.string.passwords_do_not_match)
                        loadingRegister() }
                } else {
                    passwordText.error = getString(R.string.password_not_valid)
                    loadingRegister() }
            } else {
                nameText.error = getString(R.string.required)
                loadingRegister() }
        } else {
            emailText.error = getString(R.string.email_not_valid)
            loadingRegister() }
    }

    private fun loadingRegister() {
        registerButton.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }
}