package com.example.wastesegregationapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wastesegregationapp.model.Bin

class BinAdapter(private val binList: List<Bin>) :
    RecyclerView.Adapter<BinAdapter.BinViewHolder>() {

    class BinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binName: TextView = itemView.findViewById(R.id.binName)
        val wasteType: TextView = itemView.findViewById(R.id.wasteType)
        val percentage: TextView = itemView.findViewById(R.id.percentage)
        val status: TextView = itemView.findViewById(R.id.status)
        val binIcon: ImageView = itemView.findViewById(R.id.binIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bin, parent, false)
        return BinViewHolder(view)
    }

    override fun onBindViewHolder(holder: BinViewHolder, position: Int) {
        val bin = binList[position]
        holder.binName.text = bin.binName
        holder.wasteType.text = "Waste Type: ${bin.wasteType}"
        holder.percentage.text = "${bin.percentage}%"
        holder.status.text = "Status: ${bin.status}"
        holder.binIcon.setImageResource(bin.iconResId)
    }

    override fun getItemCount(): Int = binList.size
}
