package com.example.wastesegregationapp
object config {
    private const val IP_ADDRESS = "10.129.125.136"

    const val BASE_URL = "http://$IP_ADDRESS/waste_api/"
    const val GET_DATA_URL = BASE_URL + "get_bins.php"}