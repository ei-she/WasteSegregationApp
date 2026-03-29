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

// Use AndroidViewModel so we can access 'application' for Volley
class BinDataViewModel(application: Application) : AndroidViewModel(application) {

    private val _liveBinData = MutableLiveData<Map<String, Int>>()
    val liveBinData: LiveData<Map<String, Int>> = _liveBinData

    private var fetchJob: Job? = null
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
                    val json = JSONObject(response)
                    val dataMap = mapOf(
                        "residual" to json.getInt("residual"),
                        "non_residual" to json.getInt("non_residual"),
                        "recyclable" to json.getInt("recyclable")
                    )
                    _liveBinData.postValue(dataMap)
                } catch (e: Exception) {
                    Log.e("ViewModel", "JSON Error: ${e.message}")
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