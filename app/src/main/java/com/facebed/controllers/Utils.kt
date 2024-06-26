package com.facebed.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import com.facebed.R

class Utils {
    companion object {
        //Comprueba si el email es valido
        fun isEmailValid(email: String): Boolean {
            val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
            return emailPattern.matches(email)
        }

        //Comprueba si la contraseña es valida
        fun isPasswordValid(password: String): Boolean {
            val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[._@$!#%^&+=])(?=\\S+\$).{8,}\$".toRegex()
            return passwordPattern.matches(password)
        }

        //Comprueba si el CIF es valido
        fun isFICValid(fic: String): Boolean {
            val ficPattern = "[A-Z][0-9]{8}".toRegex()
            return ficPattern.matches(fic)
        }

        //Para coloear el texto del titulo
        fun paintTitle(title: TextView, startColor: String, endColor: String): TextView {
            val width = title.paint.measureText(title.text.toString())
            val textShader: Shader = LinearGradient(0f, 0f, width, title.textSize, intArrayOf(
                Color.parseColor(startColor),
                Color.parseColor(endColor)
            ), null, Shader.TileMode.REPEAT)
            title.paint.setShader(textShader)
            return title
        }

        //Para mostrar la contraseña
        @SuppressLint("ClickableViewAccessibility")
        fun showPassword(view: AutoCompleteTextView) {
            var isPasswordVisible = false
            view.setOnTouchListener { _, event ->
                val DRAWABLE_RIGHT = 2

                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.rawX >= (view.right - view.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                        isPasswordVisible = !isPasswordVisible

                        if (isPasswordVisible) {
                            view.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        } else {
                            view.transformationMethod = PasswordTransformationMethod.getInstance()
                        }

                        view.setSelection(view.text.length)
                    }
                }
                false
            }
        }

        //Para obtener la lista de los servicios del hotel
        fun getHotelServiceKeys(): Map<String, Int> {
            return mapOf(
                "swimming_pool" to R.string.swimming_pool,
                "restaurant" to R.string.restaurant,
                "spa" to R.string.spa,
                "adults_only" to R.string.adults_only,
                "gym" to R.string.gym,
                "water_park" to R.string.water_park,
                "bowling" to R.string.bowling,
                "padel_courts" to R.string.padel_courts,
                "seafront" to R.string.seafront,
                "rural" to R.string.rural
            )
        }

        //Para obtener la lista de los servicios de la habitacion
        fun getRoomServiceKeys(): Map<String, Int> {
            return mapOf(
                "hot_tub" to R.string.hot_tub,
                "air_conditioning" to R.string.air_conditioning,
                "minibar" to R.string.minibar,
                "balcony_terrace" to R.string.balcony_terrace,
                "heating" to R.string.heating,
                "tv" to R.string.tv,
                "breakfast" to R.string.breakfast,
                "wifi" to R.string.wifi,
                "microwave" to R.string.microwave,
                "ceiling_fan" to R.string.ceiling_fan
            )
        }

        //Toast que marca un error general
        fun error(context: Context) {
            Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show()
        }

        //Para comprobar si el DNI es valido y posiblemente exista de verdad
        fun isIdValid(id: String): Boolean {
            val idPattern = "^[0-9]{8}[A-Z]$".toRegex()
            if (!idPattern.matches(id)) return false

            val idNumber = id.substring(0, 8).toInt()
            val idLetter = id[8]

            val letters = "TRWAGMYFPDXBNJZSQVHLCKE"
            val expectedLetter = letters[idNumber % 23]

            return idLetter == expectedLetter
        }
    }
}