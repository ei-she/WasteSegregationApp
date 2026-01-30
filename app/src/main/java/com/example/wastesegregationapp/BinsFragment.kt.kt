package com.example.wastesegregationapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.activityViewModels // 🔑 NEW: For Shared ViewModel
import androidx.lifecycle.Observer // 🔑 NEW: To observe LiveData

class BinsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var binAdapter: BinAdapter

    private val viewModel: BinDataViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bins, container, false)

        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerBins)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binAdapter = BinAdapter(emptyList())
        recyclerView.adapter = binAdapter // Set the adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.liveBinData.observe(viewLifecycleOwner, Observer { binList ->

            if (binList.isNotEmpty()) {
                binAdapter.updateData(binList)
            }
        })
    }
}
