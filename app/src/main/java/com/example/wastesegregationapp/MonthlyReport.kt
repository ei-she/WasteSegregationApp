package com.example.wastesegregationapp

data class MonthlyReport(
    val monthName: String,
    val year: String,
    var overflowCount: Int = 0 // Matches the variable name in your Fragment
)