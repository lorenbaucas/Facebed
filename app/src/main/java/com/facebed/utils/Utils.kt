package com.facebed.utils

import android.widget.TextView
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.AutoCompleteTextView

class Utils {
    companion object {

        fun isEmailValid(email: String): Boolean {
            val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
            return emailPattern.matches(email)
        }

        fun isPasswordValid(password: String): Boolean {
            val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[._@$!#%^&+=])(?=\\S+\$).{8,}\$".toRegex()
            return passwordPattern.matches(password)
        }

        fun isFICValid(fic: String): Boolean {
            val ficPattern = "[A-Z][0-9]{8}".toRegex()
            return ficPattern.matches(fic)
        }

        fun paintTitle(title: TextView, startColor: String, endColor: String): TextView {
            val width = title.paint.measureText(title.text.toString())
            val textShader: Shader = LinearGradient(0f, 0f, width, title.textSize, intArrayOf(
                Color.parseColor(startColor),
                Color.parseColor(endColor)
            ), null, Shader.TileMode.REPEAT)
            title.paint.setShader(textShader)
            return title
        }

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
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
    }
}