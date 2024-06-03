package com.facebed.activities

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.applandeo.materialcalendarview.CalendarView
import com.facebed.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class BookActivity : AppCompatActivity() {
    private lateinit var calendar: CalendarView
    private lateinit var acceptButton: Button
    private lateinit var nameText: AutoCompleteTextView
    private lateinit var dialog: BottomSheetDialog

    private var datesList: MutableList<Calendar> = mutableListOf()
    private var disabledDates: MutableList<Calendar> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.book_activity)

        calendar = findViewById(R.id.calendar)
        acceptButton = findViewById(R.id.accept_button)
        nameText = findViewById(R.id.name_text)

        val hotelId = intent.getStringExtra("hotelId")
        val hotelName = intent.getStringExtra("hotelName")
        val roomId = intent.getStringExtra("roomId")
        val roomName = intent.getStringExtra("roomName")
        val price = intent.getStringExtra("price")
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        // Set minimum date to today
        val calendar = Calendar.getInstance()
        this.calendar.setMinimumDate(calendar)

        val bookingsCollectionRef = FirebaseFirestore.getInstance().collection("Bookings")
        bookingsCollectionRef
            .whereEqualTo("hotelId", hotelId)
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val datesListFromDb = document.get("datesList") as List<Map<String, Long>>
                    for (dateMap in datesListFromDb) {
                        val timeInMillis = dateMap["timeInMillis"] ?: continue
                        val calendarDate = Calendar.getInstance()
                        calendarDate.timeInMillis = timeInMillis
                        disabledDates.add(calendarDate)
                    }
                }
                this.calendar.setDisabledDays(disabledDates)
            }


        acceptButton.setOnClickListener {
            datesList.clear()
            datesList.addAll(this.calendar.selectedDates)

            val finalPrice = price!!.toInt() * datesList.size
            val name = nameText.text.toString()

            if (datesList.isNotEmpty() && name.isNotEmpty()) {
                val dialogView = layoutInflater.inflate(R.layout.booking_confirmation, null)
                dialog = BottomSheetDialog(this)
                dialog.setContentView(dialogView)

                val priceText: TextView = dialogView.findViewById(R.id.price_text)
                priceText.text = "$finalPrice€"
                val okButton: Button = dialogView.findViewById(R.id.accept_button)
                okButton.setOnClickListener {

                    val bookingData = hashMapOf(
                        "userUid" to userUid,
                        "hotelId" to hotelId,
                        "hotelName" to hotelName,
                        "roomId" to roomId,
                        "roomName" to roomName,
                        "name" to nameText.text.toString(),
                        "pending" to true,
                        "finalPrice" to finalPrice,
                        "datesList" to datesList.map { date ->
                            hashMapOf("timeInMillis" to date.timeInMillis)
                        }
                    )

                    FirebaseFirestore.getInstance().collection("Bookings")
                        .add(bookingData)
                        .addOnSuccessListener { documentReference ->
                            val bookingId = documentReference.id
                            FirebaseFirestore.getInstance().collection("Bookings")
                                .document(bookingId)
                                .update("bookingId", bookingId)
                                .addOnSuccessListener {
                                    Toast.makeText(this, getString(R.string.booking_successful), Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                }
                        }
                }
                dialog.show()

            } else {
                Toast.makeText(this, getString(R.string.select_date), Toast.LENGTH_SHORT).show()
            }
        }

    }
}