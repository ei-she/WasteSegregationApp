package com.example.wastesegregationapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wastesegregationapp.Notification.NotificationItem

class BinsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter // 🔔 Changed from BinAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // We reuse fragment_bins layout since it already has a RecyclerView
        val view = inflater.inflate(R.layout.fragment_bins, container, false)

        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 📝 For now, let's use some sample data to make sure it works
        val sampleNotifications = listOf(
            NotificationItem("System Alert", "Residual bin is almost full!", "10:30 AM"),
            NotificationItem("Auth Update", "Email verified successfully.", "9:15 AM"),
            NotificationItem("Tip", "Rinse plastic containers before recycling!", "Yesterday")

        )

        notificationAdapter = NotificationAdapter(sampleNotifications)
        recyclerView.adapter = notificationAdapter

        return view
    }
}