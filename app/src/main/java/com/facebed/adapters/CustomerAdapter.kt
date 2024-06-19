package com.facebed.adapters

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.controllers.FirebaseController
import com.facebed.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class CustomerAdapter(private var bookings: MutableList<Booking>) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkIcon: ImageButton = itemView.findViewById(R.id.check_icon)
        val deleteIcon: ImageButton = itemView.findViewById(R.id.delete_icon)
        val usernameText: TextView = itemView.findViewById(R.id.username_text)
        val hotelNameText: TextView = itemView.findViewById(R.id.hotel_name_text)
        val roomNameText: TextView = itemView.findViewById(R.id.room_name_text)
        val dateText: TextView = itemView.findViewById(R.id.date_text)
        val phoneText: TextView = itemView.findViewById(R.id.phone_text)
        val idText: TextView = itemView.findViewById(R.id.id_text)
        val bookingIdText: TextView = itemView.findViewById(R.id.booking_id_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    private fun showCancelDialog(context: Context, booking: Booking, position: Int) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_reason)

        val reasonText = dialog.findViewById<TextView>(R.id.reason_text)
        val descriptionText = dialog.findViewById<AutoCompleteTextView>(R.id.description_text)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        val okButton = dialog.findViewById<Button>(R.id.accept_button)

        reasonText.text = context.getString(R.string.why_cancel)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            val reason = descriptionText.text.toString().trim()
            if (reason.trim().length > 3) {
                FirebaseController.cancelBooking(booking, reason)

                if (booking.accepted) {
                    val firestore = FirebaseFirestore.getInstance()
                    val userUid = FirebaseAuth.getInstance().currentUser?.uid

                    userUid?.let { uid ->
                        firestore.collection("User").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val currentEarnings = document.getLong("earnings") ?: 0
                                    val newEarnings = currentEarnings - booking.finalPrice

                                    firestore.collection("User").document(uid)
                                        .update("earnings", newEarnings)
                                        .addOnSuccessListener {
                                            bookings.removeAt(position)
                                            notifyItemRemoved(position)
                                            dialog.dismiss()
                                        }
                                }
                            }
                    }
                } else { dialog.dismiss() }
            } else {
                Toast.makeText(context, context.getString(R.string.reason_required), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val booking = bookings[position]
        holder.usernameText.text = booking.name
        holder.hotelNameText.text = booking.hotelName
        holder.roomNameText.text = "${booking.roomId} - ${booking.roomName}"
        holder.phoneText.text = booking.phone
        holder.idText.text = booking.id
        holder.bookingIdText.text = booking.bookingId

        val dateFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
        val startDate = booking.datesList.first().timeInMillis
        val endDate = booking.datesList.last().timeInMillis

        val startDateText = dateFormat.format(startDate)
        val endDateText = dateFormat.format(endDate)

        holder.dateText.text = if (booking.datesList.size > 1) {
            "$startDateText - $endDateText"
        } else {
            startDateText
        }

        if (booking.accepted) {
            holder.checkIcon.setImageResource(R.drawable.baseline_check_circle_outline_30)
            holder.checkIcon.clearColorFilter()
        } else {
            holder.checkIcon.setImageResource(R.drawable.baseline_check_24)
            holder.checkIcon.setOnClickListener {
                booking.accepted = true
                val firestore = FirebaseFirestore.getInstance()
                val userUid = FirebaseAuth.getInstance().currentUser?.uid

                if (userUid != null) {
                    firestore.collection("User").document(userUid).get()
                        .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val currentEarnings = document.getLong("earnings") ?: 0
                            val newEarnings = currentEarnings + booking.finalPrice

                            firestore.collection("User").document(userUid)
                                .update("earnings", newEarnings)
                                .addOnSuccessListener {
                                    firestore.collection("Bookings").document(booking.bookingId)
                                        .update("accepted", true)
                                        .addOnSuccessListener {
                                            Toast.makeText(holder.itemView.context, "Booking accepted", Toast.LENGTH_SHORT).show()
                                            notifyItemChanged(position)
                                        }
                                }
                        }
                        }
                }
            }
        }

        holder.deleteIcon.setOnClickListener {
            showCancelDialog(holder.itemView.context, booking, position)
        }
    }

    override fun getItemCount(): Int = bookings.size
}