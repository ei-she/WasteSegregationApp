package com.example.wastesegregationapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wastesegregationapp.model.Bin

class BinsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bins, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerBins)
        recyclerView.layoutManager = LinearLayoutManager(requireContext()) // Vertical list

        val bins = listOf(
            Bin("Bin 01", "Plastic", 92, "FULL", R.drawable.plastic),
            Bin("Bin 02", "Metal", 53, "HALF", R.drawable.metal),
            Bin("Bin 03", "Biodegradable", 10, "EMPTY", R.drawable.bio),
            Bin("Bin 04", "Plastic Bottles", 89, "FULL", R.drawable.plasticbottel)
        )

        recyclerView.adapter = BinAdapter(bins)

        return view
    }
}
