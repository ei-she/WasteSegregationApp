package com.example.wastesegregationapp

    import androidx.lifecycle.LiveData
    import androidx.lifecycle.MutableLiveData
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.isActive
    import kotlinx.coroutines.Dispatchers
    import com.example.wastesegregationapp.model.Bin
    class BinDataViewModel : ViewModel() {

        private val _liveBinData = MutableLiveData<List<Bin>>()

        val liveBinData: LiveData<List<Bin>> = _liveBinData

        private var isFetchingActive = false

        fun startDataFetching() {
            if (isFetchingActive) return
            isFetchingActive = true

            viewModelScope.launch(Dispatchers.IO) {
                while (isActive) {
                    try {
                        val fetchedList = fetchDataFromEsp()
                        _liveBinData.postValue(fetchedList)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(500)
                }
            }
        }

        private fun fetchDataFromEsp(): List<Bin> {
            return listOf(
                Bin("Bin 1", "Non - Residual", (0..100).random(), "STATUS_1", R.drawable.non_residual),
                Bin("Bin 2", "Residual", (0..100).random(), "STATUS_2", R.drawable.compost),
                Bin("Bin 3", "Recyclable", (0..100).random(), "STATUS_3", R.drawable.recycle_bin),
            )
        }

        override fun onCleared() {
            super.onCleared()
            isFetchingActive = false
        }
    }
