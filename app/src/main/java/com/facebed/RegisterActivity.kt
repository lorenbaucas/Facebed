package com.facebed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var spSignIn: SharedPreferences

    private lateinit var emailText: AutoCompleteTextView
    private lateinit var nameText: AutoCompleteTextView
    private lateinit var passwordText: AutoCompleteTextView
    private lateinit var confirmationText: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)
        enableEdgeToEdge()

        spSignIn = getSharedPreferences("SignIn", Context.MODE_PRIVATE)

        emailText = findViewById(R.id.email_text)
        nameText = findViewById(R.id.name_text)
        passwordText = findViewById(R.id.password_text)
        confirmationText = findViewById(R.id.confirmation_text)

        // Linear Gradient for the title
        val appTitle: TextView = findViewById(R.id.app_title)
        val width = appTitle.paint.measureText(appTitle.text.toString())
        val textShader: Shader = LinearGradient(0f, 0f, width, appTitle.textSize, intArrayOf(
            Color.parseColor("#F66C2C"),
            Color.parseColor("#F6A228")
        ), null, Shader.TileMode.REPEAT)
        appTitle.paint.setShader(textShader)

        val registerButton: Button = findViewById(R.id.register)
        registerButton.setOnClickListener {
            emailRegister()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[._@$!#%^&+=])(?=\\S+\$).{8,}\$".toRegex()
        return passwordPattern.matches(password)
    }

    private fun emailRegister() {
        if (emailText.text.trim().toString().isNotEmpty()) {
            if (nameText.text.trim().toString().isNotEmpty()) {
                if (passwordText.text.trim().toString().isNotEmpty() &&
                    isPasswordValid(passwordText.text.trim().toString())) {
                    if (confirmationText.text.trim().toString().isNotEmpty()) {
                        if (passwordText.text.trim().toString() == confirmationText.text.trim().toString()) {
                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                                emailText.text.trim().toString(),
                                passwordText.text.trim().toString()
                            ).addOnSuccessListener { authResult ->
                                val user = FirebaseAuth.getInstance().currentUser
                                val userId = authResult.user?.uid
                                userId?.let {
                                    // Guarda la información del usuario en la base de datos
                                    val companyData = hashMapOf(
                                        "email" to emailText.text.trim().toString(),
                                        "name" to nameText.text.trim().toString(),
                                        "isCompany" to false
                                    )
                                    FirebaseDatabase.getInstance().getReference("user").child(it).setValue(companyData)
                                        .addOnSuccessListener {
                                            val profileUpdates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(nameText.text.trim().toString()).build()
                                            user?.updateProfile(profileUpdates)
                                                ?.addOnCompleteListener { profileUpdateTask ->
                                                    if (profileUpdateTask.isSuccessful) {
                                                        // Save the sign-in state
                                                        spSignIn.edit {
                                                            putBoolean("isSignedIn", true)
                                                            putBoolean("isCompany", false)
                                                        }

                                                        val intent = Intent(this, HomeActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        startActivity(intent)

                                                        Toast.makeText(this,
                                                            getString(R.string.welcome_toast) + " " + user.displayName.toString(),
                                                            Toast.LENGTH_SHORT).show()

                                                    } else { Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show() } }
                                        }.addOnFailureListener { Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show() } }
                            }.addOnFailureListener { Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show() }
                        } else { confirmationText.error = getString(R.string.passwords_do_not_match) }
                    } else { confirmationText.error = getString(R.string.required) }
                } else { passwordText.error = getString(R.string.valid_password) }
            } else { nameText.error = getString(R.string.required) }
        } else { emailText.error = getString(R.string.required) }
    }
}