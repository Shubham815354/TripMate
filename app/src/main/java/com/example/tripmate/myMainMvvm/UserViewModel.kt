package com.example.tripmate.myMainMvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.myModel.Details
import com.example.tripmate.myModel.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // Existing LiveData
    private val _place = MutableLiveData<Place>()
    val place: LiveData<Place> = _place

    private val _user = MutableLiveData<Details?>()
    val user: LiveData<Details?> = _user

    // NEW: Error handling LiveData
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Map direction LiveData
    private val _walkingInfo = MutableLiveData<String>()
    val walkingInfo: LiveData<String> = _walkingInfo

    private val _bikingInfo = MutableLiveData<String>()
    val bikingInfo: LiveData<String> = _bikingInfo

    private val _drivingInfo = MutableLiveData<String>()
    val drivingInfo: LiveData<String> = _drivingInfo

    private val _routePolyline = MutableLiveData<String>()
    val routePolyline: LiveData<String> = _routePolyline

    // Rate limiting properties
    private val requestHistory = mutableListOf<Long>()
    private val maxRequestsPerMinute = 20
    private val timeWindow = 60_000L // 1 minute
    private var lastRequestTime = 0L
    private val minRequestInterval = 1000L // 1 second minimum between requests

    fun searchdata(search: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.postValue(null) // Clear previous errors
                _isLoading.postValue(true)

                Log.d("UserViewModel", "Starting search for: $search")

                // Call repository with callback handling
                repository.findPlaces(search) { result ->
                    viewModelScope.launch(Dispatchers.Main) {
                        try {
                            Log.d("UserViewModel", "Search result received: ${result.names.size} places")
                            _place.value = result
                            _isLoading.value = false
                        } catch (e: Exception) {
                            Log.e("UserViewModel", "Error processing search result", e)
                            _error.value = "Error processing search results: ${e.message}"
                            _place.value = Place(emptyList())
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Search error", e)
                withContext(Dispatchers.Main) {
                    _error.value = "Search failed: ${e.message}"
                    _place.value = Place(listOf(search)) // Fallback to search term
                    _isLoading.value = false
                }
            }
        }
    }

    fun get_details(query: String) {
        viewModelScope.launch {
            try {
                _error.postValue(null) // Clear previous errors

                // Check rate limiting before making request
                if (!canMakeRequest()) {
                    Log.w("UserViewModel", "Rate limit check failed for: $query")
                    _error.postValue("Rate limit exceeded. Please wait...")
                    return@launch
                }

                // Apply minimum interval between requests
                val currentTime = System.currentTimeMillis()
                val timeSinceLastRequest = currentTime - lastRequestTime
                if (timeSinceLastRequest < minRequestInterval) {
                    val delayTime = minRequestInterval - timeSinceLastRequest
                    Log.d("UserViewModel", "Applying delay of ${delayTime}ms for: $query")
                    delay(delayTime)
                }

                recordRequest()
                lastRequestTime = System.currentTimeMillis()

                Log.d("UserViewModel", "Making API request for: $query")

                val response = withContext(Dispatchers.IO) {
                    repository.get_data(query)
                }

                Log.d("UserViewModel", "API response received for: ${response.title}")
                _user.postValue(response)

            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    429 -> "Too many requests. Please wait..."
                    500 -> "Server error. Please try again later."
                    404 -> "Place not found: $query"
                    else -> "HTTP error ${e.code()}: ${e.message()}"
                }
                Log.e("UserViewModel", "HTTP error for $query: $errorMessage", e)
                _error.postValue(errorMessage)
                _user.postValue(null)

            } catch (e: SocketTimeoutException) {
                Log.e("UserViewModel", "Timeout error for $query", e)
                _error.postValue("Request timeout. Please check your connection.")
                _user.postValue(null)

            } catch (e: UnknownHostException) {
                Log.e("UserViewModel", "Network error for $query", e)
                _error.postValue("Network error. Please check your connection.")
                _user.postValue(null)

            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Unknown error occurred"
                Log.e("UserViewModel", "Error getting details for $query: $errorMessage", e)
                _error.postValue("Error loading $query: $errorMessage")
                _user.postValue(null)
            }
        }
    }

    private fun canMakeRequest(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Remove requests older than time window
        requestHistory.removeAll { it < currentTime - timeWindow }

        val canMake = requestHistory.size < maxRequestsPerMinute
        Log.d("UserViewModel", "Rate limit check: ${requestHistory.size}/$maxRequestsPerMinute requests in last minute. Can make request: $canMake")

        return canMake
    }

    private fun recordRequest() {
        val currentTime = System.currentTimeMillis()
        requestHistory.add(currentTime)
        Log.d("UserViewModel", "Recorded request. Total in window: ${requestHistory.size}")
    }

    fun getMapDirections(startLat: Double, startLng: Double, endLat: Double, endLng: Double) {
        viewModelScope.launch {
            try {
                _error.postValue(null) // Clear previous errors

                // Get walking directions
                repository.getDirections(startLat, startLng, endLat, endLng, "walking") { distance, duration, polyline ->
                    val info = if (distance != null && duration != null) {
                        "🚶 Walking\n$duration\n$distance"
                    } else {
                        "🚶 Walking\n--"
                    }
                    _walkingInfo.postValue(info)
                }

                // Add small delay between direction requests
                delay(500)

                // Get driving directions
                repository.getDirections(startLat, startLng, endLat, endLng, "driving") { distance, duration, polyline ->
                    val info = if (distance != null && duration != null) {
                        "🚗 Driving\n$duration\n$distance"
                    } else {
                        "🚗 Driving\n--"
                    }
                    _drivingInfo.postValue(info)

                    if (polyline != null) {
                        _routePolyline.postValue(polyline)
                    }
                }

            } catch (e: Exception) {
                Log.e("UserViewModel", "Error getting directions", e)
                _error.postValue("Error loading directions: ${e.message}")
            }
        }
    }

    // Method to clear errors
    fun clearError() {
        _error.value = null
    }

    // Method to get current rate limit status
    fun getRateLimitInfo(): String {
        val currentTime = System.currentTimeMillis()
        requestHistory.removeAll { it < currentTime - timeWindow }
        return "${requestHistory.size}/$maxRequestsPerMinute requests in last minute"
    }

    override fun onCleared() {
        super.onCleared()
        requestHistory.clear()
        Log.d("UserViewModel", "ViewModel cleared")
    }
}