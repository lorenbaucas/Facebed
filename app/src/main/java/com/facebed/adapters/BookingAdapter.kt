package com.facebed.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R
import com.facebed.controllers.FirebaseController
import com.facebed.controllers.Utils
import com.facebed.models.Booking
import com.facebed.models.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingAdapter(private var bookings: MutableList<Booking>, private val showReviewButton: Boolean) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelRoom: TextView = itemView.findViewById(R.id.primary_text)
        val date: TextView = itemView.findViewById(R.id.secondary_text)
        val cancelIcon: ImageButton = itemView.findViewById(R.id.cancel_icon)
        val reviewButton: ImageButton = itemView.findViewById(R.id.review_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        //Fecha de la reserva
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

        //Muestra las reservas y las que se han cancelado no dejara escribir reseña
        if (showReviewButton) {
            if (booking.reason == null || booking.reason == "") {
                holder.cancelIcon.visibility = View.GONE
                holder.reviewButton.visibility = View.VISIBLE
                holder.reviewButton.setOnClickListener {
                    openReviewDialog(holder.itemView.context, booking)
                }
            } else {
                holder.cancelIcon.visibility = View.GONE
                holder.reviewButton.visibility = View.GONE
            }
        } else {
            holder.cancelIcon.visibility = View.VISIBLE
            holder.reviewButton.visibility = View.GONE
            holder.cancelIcon.setOnClickListener {
                val userUid = FirebaseAuth.getInstance().currentUser?.uid
                if (userUid == booking.userUid) {
                    val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
                    alertDialogBuilder
                        .setMessage(holder.itemView.context.getString(R.string.are_you_sure))
                        .setCancelable(false)
                        .setPositiveButton("Sí") { dialog, id ->
                            FirebaseController.cancelBooking(booking, "Cancelled by user")
                            bookings.removeAt(position)
                            notifyItemRemoved(position)
                        }
                        .setNegativeButton("No") { dialog, id ->
                            dialog.dismiss()
                        }

                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.show()
                }
            }

        }
    }

    override fun getItemCount(): Int = bookings.size

    //Dialogo para escribir la reseña y guardarla en la base de datos
    private fun openReviewDialog(context: Context, booking: Booking) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null)
        val reviewText = dialogView.findViewById<AutoCompleteTextView>(R.id.description_text)
        var starCount = 0

        val stars = arrayOf<ImageView>(
            dialogView.findViewById(R.id.star1),
            dialogView.findViewById(R.id.star2),
            dialogView.findViewById(R.id.star3),
            dialogView.findViewById(R.id.star4),
            dialogView.findViewById(R.id.star5)
        )

        fun changeStars(count: Int) {
            for (star in stars) {
                star.setImageResource(R.drawable.baseline_star_border_24)
            }

            for (i in 0 until count) {
                stars[i].setImageResource(R.drawable.baseline_star_24)
            }

            starCount = count
        }

        for ((index, star) in stars.withIndex()) {
            star.setOnClickListener { changeStars(index + 1) }
        }

        val deleteButton = dialogView.findViewById<Button>(R.id.delete_button)
        val acceptButton = dialogView.findViewById<Button>(R.id.accept_button)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar_delete_confirmation)

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.show()

        val reviewsRef = FirebaseFirestore.getInstance().collection("Reviews").document(booking.bookingId)

        reviewsRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val review = documentSnapshot.toObject(Review::class.java)
                reviewText.setText(review?.reviewText)
                starCount = review!!.stars
                changeStars(starCount)
            }
        }

        deleteButton.setOnClickListener {
            reviewsRef.get().addOnSuccessListener { documentSnapshot ->
                documentSnapshot.reference.delete()
                alertDialog.dismiss()
            }
        }

        acceptButton.setOnClickListener {
            val reviewContent = reviewText.text?.trim().toString()
            if (starCount > 0) {
                deleteButton.visibility = View.GONE
                acceptButton.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                val data = hashMapOf(
                    "bookingId" to booking.bookingId,
                    "userUid" to booking.userUid,
                    "username" to booking.name,
                    "hotelId" to booking.hotelId,
                    "hotelName" to booking.hotelName,
                    "stars" to starCount,
                    "reviewText" to reviewContent,
                    "currentDayInMillis" to System.currentTimeMillis()
                )

                reviewsRef.set(data).addOnSuccessListener {
                    deleteButton.visibility = View.VISIBLE
                    acceptButton.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, context.getString(R.string.review_saved), Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }.addOnFailureListener {
                    deleteButton.visibility = View.VISIBLE
                    acceptButton.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    Utils.error(context)
                }
            } else {
                Toast.makeText(context, "Please provide a rating", Toast.LENGTH_SHORT).show()
            }
        }
    }
}