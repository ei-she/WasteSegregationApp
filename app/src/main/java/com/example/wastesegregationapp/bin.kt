package com.example.wastesegregationapp.model

data class Bin(
    val binId: String,
    val wasteType: String,
    val percentage: Int,
    val statusText: String,
    val iconResId: Int
)
