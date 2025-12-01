package com.example.wastesegregationapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wastesegregationapp.model.Bin

// 🔑 FIX 1: Change 'val' to 'var' in the constructor
// This makes 'bins' a mutable property of the Adapter class itself.
class BinAdapter(private var bins: List<Bin>) :
    RecyclerView.Adapter<BinAdapter.BinViewHolder>() {

    class BinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binName: TextView = itemView.findViewById(R.id.binName)
        val wasteType: TextView = itemView.findViewById(R.id.wasteType)
        val percentage: TextView = itemView.findViewById(R.id.percentage)
        val status: TextView = itemView.findViewById(R.id.status)
        val binIcon: ImageView = itemView.findViewById(R.id.binIcon)

        // ❌ REMOVED: updateData() and getItemCount() were here
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bin, parent, false)
        return BinViewHolder(view)
    }

    override fun onBindViewHolder(holder: BinViewHolder, position: Int) {
        // 🔑 FIX 2: Use the class property 'bins' instead of the old parameter name 'binList'
        val bin = bins[position]
        holder.binName.text = bin.binId // Use binId or binName, depending on your data class
        holder.wasteType.text = "Waste Type: ${bin.wasteType}"
        holder.percentage.text = "${bin.percentage}%"
        holder.status.text = "Status: ${bin.statusText}" // Use statusText from BinStatus
        holder.binIcon.setImageResource(bin.iconResId)
    }

    fun updateData(newBins: List<Bin>) {
        this.bins = newBins
        notifyDataSetChanged()
    }

    // 🔑 FIX 4: Use the class property 'bins' size
    override fun getItemCount(): Int = bins.size
}