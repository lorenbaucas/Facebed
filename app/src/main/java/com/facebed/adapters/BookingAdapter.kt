package com.facebed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.controllers.FirebaseController
import com.facebed.models.Booking
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingAdapter(private var bookings: MutableList<Booking>) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelRoom: TextView = itemView.findViewById(R.id.primary_text)
        val date: TextView = itemView.findViewById(R.id.secondary_text)
        val cancelIcon: ImageButton = itemView.findViewById(R.id.cancel_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.hotelRoom.text = booking.hotelName + " - " + booking.roomName

        val dateFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
        val startDate = Date(booking.datesList.first().timeInMillis)
        val endDate = Date(booking.datesList.last().timeInMillis)

        val startDateText = dateFormat.format(startDate)
        val endDateText = dateFormat.format(endDate)

        val formattedDateText = if (booking.datesList.size > 1) {
            "$startDateText - $endDateText"
        } else {
            startDateText
        }

        holder.date.text = formattedDateText

        holder.cancelIcon.setOnClickListener {
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            if (userUid == booking.userUid) {
                FirebaseController.cancelBooking(booking) { success ->
                    if (success) {
                        bookings.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = bookings.size
}