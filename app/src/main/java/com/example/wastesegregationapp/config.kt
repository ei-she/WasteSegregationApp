package com.example.wastesegregationapp
object config {
    private const val IP_ADDRESS = "192.168.0.166"

    const val BASE_URL = "http://$IP_ADDRESS/waste_api/"
    const val GET_DATA_URL = BASE_URL + "get_bins.php"}