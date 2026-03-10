package com.example.wastesegregationapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Ensure your NotificationItem model matches these field names
data class NotificationItem(
    val title: String = "",
    val message: String = "",
    val time: String = ""
)

class NotificationAdapter(private var notifications: MutableList<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notifTitle)
        val message: TextView = view.findViewById(R.id.notifMessage)
        val time: TextView = view.findViewById(R.id.notifTime)
        val icon: ImageView = view.findViewById(R.id.notifIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bin, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = notifications[position]

        holder.title.text = item.title
        holder.message.text = item.message
        holder.time.text = item.time

        if (item.title.contains("Full", ignoreCase = true)) {
            holder.icon.setImageResource(R.drawable.redstatus_dot)
            holder.title.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        } else {
            holder.icon.setImageResource(R.drawable.greenstatus_dot)
            holder.title.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        }
    }

    override fun getItemCount() = notifications.size

    fun updateList(newList: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newList)
        notifyDataSetChanged()
    }
}