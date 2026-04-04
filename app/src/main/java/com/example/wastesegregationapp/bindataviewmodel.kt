package com.example.wastesegregationapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.*
import org.json.JSONObject

class BinDataViewModel(application: Application) : AndroidViewModel(application) {

    private val _liveBinData = MutableLiveData<Map<String, Int>>()
    val liveBinData: LiveData<Map<String, Int>> = _liveBinData

    private var fetchJob: Job? = null
    // Use a single instance of the request queue
    private val requestQueue = Volley.newRequestQueue(application)

    fun startDataFetching() {
        if (fetchJob?.isActive == true) return

        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchFromPi()
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun fetchFromPi() {
        val stringRequest = StringRequest(Request.Method.GET, config.GET_DATA_URL,
            { response ->
                try {
                    // Log the raw response for final confirmation during defense
                    Log.d("BIN_DATA", "Raw Response: $response")

                    val json = JSONObject(response)

                    // Access the "levels" object inside your JSON
                    val levels = json.getJSONObject("levels")

                    // KEY FIX: Use the exact keys from the Logcat response
                    // Ensure your UI (Activity/Fragment) observers look for these keys!
                    val dataMap = mapOf(
                        "Bio" to levels.optInt("Bio", 0),
                        "Non" to levels.optInt("Non", 0),
                        "others" to levels.optInt("others", 0)
                    )

                    _liveBinData.postValue(dataMap)
                } catch (e: Exception) {
                    Log.e("ViewModel", "Parsing Error: ${e.message}")
                }
            },
            { error ->
                Log.e("ViewModel", "Network Error: ${error.message}")
            }
        )
        requestQueue.add(stringRequest)
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }
}