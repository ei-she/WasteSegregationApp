package com.example.wastesegregationapp.model

data class Bin(
    val binName: String,
    val wasteType: String,
    val percentage: Int,
    val status: String,
    val iconResId: Int
)

