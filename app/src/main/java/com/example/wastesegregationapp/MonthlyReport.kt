package com.example.wastesegregationapp

data class MonthlyReport(
    val monthName: String,
    val year: String,
    var fillCount: Int = 0 // Number of times it hit 90%+
)
