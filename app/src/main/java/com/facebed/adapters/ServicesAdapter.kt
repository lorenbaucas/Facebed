package com.facebed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebed.R

class ServicesAdapter(private val servicesData: MutableList<String>, val clickListener: () -> Unit) : RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceName: TextView = itemView.findViewById(R.id.service_name_text)

        init {
            itemView.setOnClickListener { clickListener() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        //Para obtener la lista de servicios tanto de los hoteles como de las habitaciones
        val service = servicesData[position]
        holder.serviceName.text = service
    }

    override fun getItemCount(): Int = servicesData.size
}