package com.example.wastesegregationapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class NotificationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()
    private val dbUrl = "https://wise-wastee-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bins, container, false)

        recyclerView = view.findViewById(R.id.notificationRecyclerView)
        val btnClearAll: ImageButton = view.findViewById(R.id.btnClearAll)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        btnClearAll.setOnClickListener {
            showClearConfirmationDialog()
        }

        setupNotificationListener()

        return view
    }

    private fun setupNotificationListener() {
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("notifications")

        ref.limitToLast(20).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = mutableListOf<NotificationItem>()

                for (data in snapshot.children) {
                    val title = data.child("title").value.toString()
                    val message = data.child("message").value.toString()
                    val timestamp = data.child("timestamp").value.toString()

                    newList.add(NotificationItem(title, message, timestamp))
                }

                adapter.updateList(newList.reversed())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear History")
            .setMessage("Are you sure you want to delete all notification history? This cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                val ref = FirebaseDatabase.getInstance(dbUrl).getReference("notifications")
                ref.removeValue().addOnSuccessListener {
                    Toast.makeText(context, "Notification history cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}