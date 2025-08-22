package com.example.tripmate.myMainMvvm

import android.util.Log
import com.example.tripmate.myModel.Details
import com.example.tripmate.myModel.Place
import com.example.tripmate.myRetrofit.API
import com.example.tripmate.myRetrofit.obj
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.delay

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    // Rate limiting properties
    private val requestHistory = mutableListOf<Long>()
    private val maxRequestsPerMinute = 25
    private val timeWindow = 60_000L // 1 minute
    private var lastApiRequestTime = 0L
    private val minApiRequestInterval = 800L // 0.8 seconds minimum between API requests

    suspend fun get_data(query: String): Details {
        try {
            // Apply rate limiting
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRequest = currentTime - lastApiRequestTime

            if (timeSinceLastRequest < minApiRequestInterval) {
                val delayTime = minApiRequestInterval - timeSinceLastRequest
                Log.d("UserRepository", "Applying rate limit delay of ${delayTime}ms for: $query")
                delay(delayTime)
            }

            if (!canMakeRequest()) {
                Log.w("UserRepository", "Rate limit exceeded for: $query")
                throw Exception("Rate limit exceeded. Please wait before making more requests.")
            }

            recordRequest()
            lastApiRequestTime = System.currentTimeMillis()

            val api: API = obj.api
            Log.d("UserRepository", "Making API call for: $query")

            val result = api.get_data(query)
            Log.d("UserRepository", "API call successful for: $query")

            return result

        } catch (e: HttpException) {
            when (e.code()) {
                429 -> {
                    Log.e("UserRepository", "Rate limited (429) for: $query")
                    throw Exception("Too many requests. Please wait...")
                }
                500 -> {
                    Log.e("UserRepository", "Server error (500) for: $query")
                    throw Exception("Server error. Please try again later.")
                }
                404 -> {
                    Log.e("UserRepository", "Not found (404) for: $query")
                    throw Exception("Place not found: $query")
                }
                else -> {
                    Log.e("UserRepository", "HTTP error ${e.code()} for: $query", e)
                    throw Exception("Network error: ${e.message()}")
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e("UserRepository", "Timeout for: $query", e)
            throw Exception("Request timeout. Please check your connection.")
        } catch (e: UnknownHostException) {
            Log.e("UserRepository", "Network error for: $query", e)
            throw Exception("Network error. Please check your connection.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting data for: $query", e)
            throw e
        }
    }

    private fun canMakeRequest(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Remove requests older than time window
        requestHistory.removeAll { it < currentTime - timeWindow }

        return requestHistory.size < maxRequestsPerMinute
    }

    private fun recordRequest() {
        requestHistory.add(System.currentTimeMillis())
    }

    fun findPlaces(search: String, callback: (Place) -> Unit) {
        val searchKeyword = search.trim().lowercase()
        Log.d("UserRepository", "Searching for: '$searchKeyword'")

        try {
            db.collection("states").get()
                .addOnSuccessListener { statesSnapshot ->
                    Log.d("UserRepository", "Found ${statesSnapshot.documents.size} states")

                    // Find matching state doc by name (now stored in the document)
                    val matchedState = statesSnapshot.documents.firstOrNull { doc ->
                        val stateName = doc.getString("name")?.trim()?.lowercase() ?: ""
                        val documentId = doc.id.lowercase()

                        Log.d("UserRepository", "Checking state: name='$stateName', id='$documentId'")

                        // Match either by document ID or by name field
                        stateName == searchKeyword || documentId == searchKeyword
                    }

                    if (matchedState != null) {
                        Log.d("UserRepository", "Found matching state: ${matchedState.getString("name")}")
                        // Fetch all places inside that state
                        matchedState.reference.collection("places").get()
                            .addOnSuccessListener { placesSnapshot ->
                                Log.d("UserRepository", "Found ${placesSnapshot.documents.size} places in state")

                                val allNames = mutableListOf<String>()
                                for (doc in placesSnapshot) {
                                    val names = doc.get("names") as? List<String> ?: emptyList()
                                    allNames.addAll(names)
                                }
                                Log.d("UserRepository", "State match '$searchKeyword': $allNames")
                                callback(Place(allNames))
                            }
                            .addOnFailureListener { exception ->
                                Log.e("UserRepository", "Failed to fetch places for state '$searchKeyword'", exception)
                                callback(Place(emptyList()))
                            }
                    } else {
                        Log.d("UserRepository", "No state match, searching cities...")
                        // Search by city across all states
                        db.collectionGroup("places").get()
                            .addOnSuccessListener { citySnapshot ->
                                Log.d("UserRepository", "Collection group query returned ${citySnapshot.documents.size} documents")

                                val cityNames = mutableListOf<String>()
                                for (doc in citySnapshot) {
                                    val cityName = (doc.getString("city") ?: "").trim().lowercase()
                                    if (cityName == searchKeyword) {
                                        val names = doc.get("names") as? List<String> ?: emptyList()
                                        cityNames.addAll(names)
                                        Log.d("UserRepository", "City match found: $names")
                                    }
                                }

                                if (cityNames.isNotEmpty()) {
                                    Log.d("UserRepository", "City match '$searchKeyword': $cityNames")
                                    callback(Place(cityNames))
                                } else {
                                    Log.d("UserRepository", "No state or city match for '$searchKeyword'")
                                    callback(Place(emptyList()))
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("UserRepository", "City query failed for '$searchKeyword'", exception)
                                callback(Place(emptyList()))
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("UserRepository", "Failed to fetch states", exception)
                    callback(Place(emptyList()))
                }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error in findPlaces for '$searchKeyword'", e)
            callback(Place(emptyList()))
        }
    }

    fun getDirections(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        travelMode: String,
        callback: (String?, String?, String?) -> Unit
    ) {
        try {
            val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$startLat,$startLng" +
                    "&destination=$endLat,$endLng" +
                    "&mode=$travelMode" +
                    "&key=\"GET_Your_Own_KEY\""

            val thread = Thread {
                try {
                    Log.d("UserRepository", "Making directions API call for mode: $travelMode")

                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000 // 10 seconds
                    connection.readTimeout = 15000 // 15 seconds

                    val responseCode = connection.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val jsonObject = JSONObject(response)

                        if (jsonObject.getJSONArray("routes").length() > 0) {
                            val route = jsonObject.getJSONArray("routes").getJSONObject(0)
                            val leg = route.getJSONArray("legs").getJSONObject(0)

                            val distance = leg.getJSONObject("distance").getString("text")
                            val duration = leg.getJSONObject("duration").getString("text")
                            val polyline = route.getJSONObject("overview_polyline").getString("points")

                            Log.d("UserRepository", "Directions successful: $travelMode - $distance, $duration")
                            callback(distance, duration, polyline)
                        } else {
                            Log.w("UserRepository", "No routes found for $travelMode")
                            callback(null, null, null)
                        }
                    } else {
                        Log.e("UserRepository", "HTTP error $responseCode for directions")
                        callback(null, null, null)
                    }

                } catch (e: SocketTimeoutException) {
                    Log.e("UserRepository", "Timeout error getting directions for $travelMode", e)
                    callback(null, null, null)
                } catch (e: Exception) {
                    Log.e("UserRepository", "Error getting directions for $travelMode: ${e.message}", e)
                    callback(null, null, null)
                }
            }
            thread.start()

        } catch (e: Exception) {
            Log.e("UserRepository", "Error creating directions request for $travelMode: ${e.message}", e)
            callback(null, null, null)
        }
    }

    // Method to get current rate limit status
    fun getRateLimitStatus(): String {
        val currentTime = System.currentTimeMillis()
        requestHistory.removeAll { it < currentTime - timeWindow }
        return "Requests in last minute: ${requestHistory.size}/$maxRequestsPerMinute"
    }
}