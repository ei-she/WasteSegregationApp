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

class NotificationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()

    // Replace with your current Pi IP from the screenshot
    private val fetchUrl = "http://192.168.0.147/waste_api/get_notifications.php"
    private val clearUrl = "http://192.168.0.147/waste_api/clear_notifications.php"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        val request = JsonArrayRequest(Request.Method.GET, fetchUrl, null,
            { response ->
                val newList = mutableListOf<NotificationItem>()
                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    newList.add(
                        NotificationItem(
                            obj.getString("title"),
                            obj.getString("message"),
                            obj.getString("time")
                        )
                    )
                }
                activity?.runOnUiThread {
                    notificationList.clear()
                    notificationList.addAll(newList)
                    adapter.notifyDataSetChanged()
                }
            },
            { error ->
                Log.e("API_ERROR", "Failed to fetch: ${error.message}")
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
        val queue = Volley.newRequestQueue(requireContext())
        val request = StringRequest(Request.Method.POST, clearUrl,
            {
                notificationList.clear()
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
            },
            { Toast.makeText(context, "Clear failed", Toast.LENGTH_SHORT).show() }
        )
        queue.add(request)
    }
}