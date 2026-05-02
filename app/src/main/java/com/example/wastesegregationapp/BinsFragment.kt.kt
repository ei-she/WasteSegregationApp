package com.example.wastesegregationapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class NotificationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()

    // Updated IP to your current one
    private val apiUrl = "http://10.129.125.136/waste_api/get_notifications.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ensure fragment_bins is the correct layout name
        val view = inflater.inflate(R.layout.fragment_bins, container, false)

        recyclerView = view.findViewById(R.id.notificationRecyclerView)
        val btnClearAll: ImageButton = view.findViewById(R.id.btnClearAll)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        btnClearAll.setOnClickListener {
            showClearConfirmationDialog()
        }

        fetchNotifications()

        return view
    }

    private fun fetchNotifications() {
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonArrayRequest(Request.Method.GET, apiUrl, null,
            { response ->
                val newList = mutableListOf<NotificationItem>()
                try {
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        newList.add(
                            NotificationItem(
                                obj.optString("title", "No Title"),
                                obj.optString("message", "No Message"),
                                obj.optString("timestamp", "")
                            )
                        )
                    }
                    adapter.updateList(newList)
                } catch (e: Exception) {
                    Log.e("NOTIF_ERROR", "JSON Error: ${e.message}")
                }
            },
            { error ->
                Log.e("NOTIF_ERROR", "Volley Error: ${error.message}")
                Toast.makeText(context, "Cannot connect to Pi", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear History")
            .setMessage("Are you sure you want to delete all notification history?")
            .setPositiveButton("Clear All") { _, _ ->
                clearNotificationsOnServer()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearNotificationsOnServer() {
        // Pointing to your clear script
        val clearUrl = "http://10.129.125.136/waste_api/clear_notifications.php"
        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(Request.Method.POST, clearUrl,
            {
                notificationList.clear()
                adapter.updateList(emptyList())
                Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
            },
            { Toast.makeText(context, "Failed to clear", Toast.LENGTH_SHORT).show() }
        )
        queue.add(request)
    }
}