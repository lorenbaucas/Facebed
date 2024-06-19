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
import com.facebed.controllers.Utils
import com.facebed.models.Booking
import com.facebed.models.Review
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewsAdapter(private var bookings: MutableList<Booking>, private val showButtons: Boolean) : RecyclerView.Adapter<ReviewsAdapter.ReviewsViewHolder>() {

    class ReviewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.username_text)
        val starRating: TextView = itemView.findViewById(R.id.stars_number_text)
        val date: TextView = itemView.findViewById(R.id.date_text)
        val reviewText: TextView = itemView.findViewById(R.id.review_text)
        val editIcon: ImageButton = itemView.findViewById(R.id.edit_icon)
        val deleteIcon: ImageButton = itemView.findViewById(R.id.delete_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewsViewHolder, position: Int) {
        val booking = bookings[position]
        val dateFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())

        val reviewsRef = FirebaseFirestore.getInstance().collection("Reviews").document(booking.bookingId)
        reviewsRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val review = documentSnapshot.toObject(Review::class.java)
                holder.username.text = review?.username
                holder.starRating.text = review?.stars.toString()
                holder.date.text = dateFormat.format(review?.currentDayInMillis)
                holder.reviewText.text = review?.reviewText

                if (showButtons) {
                    holder.deleteIcon.visibility = View.VISIBLE
                    holder.editIcon.visibility = View.VISIBLE

                    holder.editIcon.setOnClickListener {
                        openReviewDialog(holder.itemView.context, booking)
                    }

                    holder.deleteIcon.setOnClickListener {
                        reviewsRef.delete().addOnSuccessListener {
                            bookings.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, bookings.size)
                        }
                    }
                } else {
                    holder.deleteIcon.visibility = View.GONE
                    holder.editIcon.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int = bookings.size

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

        deleteButton.text = context.getString(R.string.cancel)
        deleteButton.setOnClickListener { alertDialog.dismiss() }

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
                    Toast.makeText(context, "Review updated successfully", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                    notifyDataSetChanged()
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