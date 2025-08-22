package com.example.tripmate.myFragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripmate.R
import com.example.tripmate.databinding.FragmentHome2Binding
import com.example.tripmate.myAdapter.PopularAdapter
import com.example.tripmate.myAdapter.RecyAdapter
import com.example.tripmate.myMainMvvm.UserRepository
import com.example.tripmate.myMainMvvm.UserViewModel
import com.example.tripmate.myMainMvvm.UserViewModelFactory
import com.example.tripmate.myModel.Details
import com.example.tripmate.myModel.Place
import com.example.tripmate.myModel.SharedViewModel
import com.example.tripmate.myRoom.AppDatabase

class HomeFragment : Fragment() {
    lateinit var binding: FragmentHome2Binding
    lateinit var adapter: RecyAdapter
    lateinit var adapt: PopularAdapter
    lateinit var viewmodel: UserViewModel
    private val allDetails = mutableListOf<Details>()
    private val defaultDetails = mutableListOf<Details>()
    private var isSearchMode = false
    private var defaultDataCount = 0
    private var searchExpectedCount = 0
    private val totalDefaultPlaces = 8
    lateinit var viewmodel_fav: com.example.tripmate.myRoomMvvm.UserViewModel

    // Enhanced rate limiting properties
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var defaultLoaded = false
    private val requestQueue = mutableListOf<RequestItem>()
    private var isProcessingQueue = false
    private val minRequestInterval = 1200L // 1.2 seconds between requests
    private var lastRequestTime = 0L
    private val maxRetries = 2
    private val retryDelay = 3000L // 3 seconds
    private var currentRetryCount = 0
    private var isFragmentActive = false // Track fragment state

    // Cache for search results to avoid re-requesting
    private val searchCache = mutableMapOf<String, List<Details>>()
    private var lastSearchQuery = ""
    private var isRateLimited = false

    // Data class for queue items
    data class RequestItem(
        val placeName: String,
        val isForSearch: Boolean,
        val retryCount: Int = 0
    )

    private val defaultPlaces = listOf(
        "Kashmir", "Shimla", "Varanasi",
        "Darjeeling", "Haridwar", "Goa",
        "Rameshwaram", "Visakhapatnam"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home2, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = UserRepository()
        val factory = UserViewModelFactory(repository)
        viewmodel = ViewModelProvider(this, factory)[UserViewModel::class.java]

        adapt = PopularAdapter()
        adapter = RecyAdapter()

        binding.recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.recyclerViewExplore.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.recyclerView.adapter = adapt
        binding.recyclerViewExplore.adapter = adapter

        setupObservers()
        setupClickListeners()
        setupFavoriteFeature()

        isFragmentActive = true
    }

    private fun setupClickListeners() {
        // Search button
        binding.imageSearch.setOnClickListener {
            performSearch()
        }

        // Debounced search typing with increased delay
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val query = s?.toString()?.trim() ?: ""
                    if (query.isNotEmpty()) {
                        // Check cache first for typing search
                        val cachedResults = searchCache[query.lowercase()]
                        if (cachedResults != null && cachedResults.isNotEmpty()) {
                            Log.d("HomeFragment", "Using cached results for typing: $query")
                            isSearchMode = true
                            allDetails.clear()
                            allDetails.addAll(cachedResults)
                            updateRecyclerViews(allDetails)
                        } else {
                            performSearchWithRateLimitCheck(query)
                        }
                    } else {
                        // If search is cleared, show default data
                        showDefaultData()
                    }
                }
                handler.postDelayed(searchRunnable!!, 1000) // Increased delay
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        adapter.onSetclickListner { details ->
            sharedViewModel.selectedDetails = details
            findNavController().navigate(R.id.action_homeFragment_to_mapFragment)
        }
        adapt.onSetclickListner { details ->
            sharedViewModel.selectedDetails = details
            findNavController().navigate(R.id.action_homeFragment_to_mapFragment)
        }
    }

    private fun setupFavoriteFeature() {
        val repo_fav = com.example.tripmate.myRoomMvvm.UserRepository(
            AppDatabase.get_instance(requireActivity()).user_dao()
        )
        val factory_fav = com.example.tripmate.myRoomMvvm.UserViewModelFactory(repo_fav)
        viewmodel_fav = ViewModelProvider(
            requireActivity(),
            factory_fav
        )[com.example.tripmate.myRoomMvvm.UserViewModel::class.java]

        adapt.setOnchoose { userlist ->
            viewmodel_fav.check_existing(userlist)
        }
        adapter.setOnchoose { userlist ->
            viewmodel_fav.check_existing(userlist)
        }

        viewmodel_fav.fav_.observe(viewLifecycleOwner) { fav_list ->
            adapt.setOnFav(fav_list)
            adapter.setOnFav(fav_list)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "onResume called")

        // Load favorites
        viewmodel_fav.load_data()

        // Check the current search text and handle accordingly
        val searchText = binding.editTextSearch.text?.toString()?.trim() ?: ""

        Log.d("HomeFragment", "Search text on resume: '$searchText'")

        // Cancel any pending operations
        cancelAllPendingOperations()

        if (searchText.isEmpty()) {
            // Search is empty, show default data
            Log.d("HomeFragment", "Search empty, showing default data")
            showDefaultData()
        } else {
            // Search has text, check cache first before making new requests
            Log.d("HomeFragment", "Search has text: '$searchText'")

            // Check if we have cached results for this search
            val cachedResults = searchCache[searchText.lowercase()]
            if (cachedResults != null && cachedResults.isNotEmpty()) {
                Log.d("HomeFragment", "Found cached results for '$searchText', using cache")
                isSearchMode = true
                allDetails.clear()
                allDetails.addAll(cachedResults)
                updateRecyclerViews(allDetails)
            } else {
                Log.d("HomeFragment", "No cache for '$searchText', checking rate limit before search")
                // Add delay to ensure UI is ready, then perform search with rate limit check
                handler.postDelayed({
                    performSearchWithRateLimitCheck(searchText)
                }, 100)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isFragmentActive = false
        Log.d("HomeFragment", "onPause called")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "onDestroyView called")
        isFragmentActive = false
        // Clean up to prevent memory leaks
        cancelAllPendingOperations()
    }

    private fun cancelAllPendingOperations() {
        requestQueue.clear()
        handler.removeCallbacksAndMessages(null)
        searchRunnable?.let { handler.removeCallbacks(it) }
        isProcessingQueue = false
        currentRetryCount = 0
        // Don't reset isRateLimited here as it should persist until successful request
    }

    private fun resetToDefaultState() {
        binding.editTextSearch.setText("")
        isSearchMode = false
        defaultDataCount = 0
        searchExpectedCount = 0
        currentRetryCount = 0
        allDetails.clear()
        defaultDetails.clear()
        requestQueue.clear()
        adapter.submitList(emptyList())
        adapt.submitList(emptyList())
        loadDefaultData()
    }

    private fun setupObservers() {
        viewmodel.place.observe(viewLifecycleOwner) { place ->
            if (!isFragmentActive) return@observe

            if (isSearchMode) {
                allDetails.clear()
                searchExpectedCount = 0
                requestQueue.clear() // Clear existing queue

                if (place.names.isNotEmpty()) {
                    searchExpectedCount = minOf(place.names.size)

                    // Add search requests to queue
                    place.names.forEach { name ->
                        requestQueue.add(RequestItem(name, true))
                    }

                    processRequestQueue()
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "No places found", Toast.LENGTH_SHORT).show()
                    showDefaultData()
                }
            }
        }

        viewmodel.user.observe(viewLifecycleOwner) { details ->
            if (!isFragmentActive) return@observe

            if (details != null) {
                handleSuccessfulResponse(details)
            } else {
                handleNullResponse()
            }
        }

        // Observe errors - now properly available in ViewModel
        viewmodel.error.observe(viewLifecycleOwner) { error ->
            if (!isFragmentActive) return@observe

            if (error != null) {
                handleErrorResponse(error)
                // Clear the error after handling it
                handler.postDelayed({
                    if (isFragmentActive) {
                        viewmodel.clearError()
                    }
                }, 3000)
            }
        }
    }

    private fun handleSuccessfulResponse(details: Details) {
        if (!isFragmentActive) return

        Log.d("HomeFragment", "Received details for: ${details.title}")

        if (isSearchMode) {
            if (!allDetails.contains(details)) {
                allDetails.add(details)
            }
            if (allDetails.size >= searchExpectedCount) {
                // Cache the search results
                if (lastSearchQuery.isNotEmpty()) {
                    searchCache[lastSearchQuery.lowercase()] = ArrayList(allDetails)
                    Log.d("HomeFragment", "Cached results for: $lastSearchQuery")
                }
                updateRecyclerViews(allDetails)
                isRateLimited = false // Reset rate limit flag on success
            }
        } else {
            if (!defaultDetails.contains(details) && defaultDataCount < totalDefaultPlaces) {
                defaultDetails.add(details)
                defaultDataCount++
                if (defaultDataCount >= totalDefaultPlaces) {
                    updateRecyclerViews(defaultDetails)
                    isRateLimited = false // Reset rate limit flag on success
                }
            }
        }

        // Reset retry count on success
        currentRetryCount = 0
    }

    private fun handleNullResponse() {
        if (!isFragmentActive) return
        Log.w("HomeFragment", "Received null response")
        // Handle null response - might indicate rate limiting or API issues
    }

    private fun handleErrorResponse(error: String) {
        if (!isFragmentActive) return

        Log.e("HomeFragment", "API Error: $error")

        if (error.contains("429") || error.contains("Too Many Requests") || error.contains("Rate limit exceeded")) {
            isRateLimited = true

            // If we're rate limited and have cached data for current search, use it
            val currentSearchText = binding.editTextSearch.text?.toString()?.trim() ?: ""
            if (currentSearchText.isNotEmpty() && isSearchMode) {
                val cachedResults = searchCache[currentSearchText.lowercase()]
                if (cachedResults != null && cachedResults.isNotEmpty()) {
                    Log.d("HomeFragment", "Rate limited, but found cached data for '$currentSearchText'")
                    binding.progressBar.visibility = View.GONE
                    allDetails.clear()
                    allDetails.addAll(cachedResults)
                    updateRecyclerViews(allDetails)
                    if (isFragmentActive) {
                        Toast.makeText(requireContext(), "Showing cached results due to rate limit", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
            }

            // Handle rate limit with retries
            currentRetryCount++

            if (currentRetryCount <= maxRetries) {
                Log.d("HomeFragment", "Rate limited, retrying in ${retryDelay * currentRetryCount}ms (attempt $currentRetryCount)")

                // Pause processing and retry after longer delay
                isProcessingQueue = false
                handler.postDelayed({
                    if (isFragmentActive) {
                        Log.d("HomeFragment", "Retrying after rate limit...")
                        processRequestQueue()
                    }
                }, retryDelay * currentRetryCount) // Exponential backoff

            } else {
                binding.progressBar.visibility = View.GONE
                if (isFragmentActive) {
                    Toast.makeText(requireContext(), "Server is busy. Showing available data.", Toast.LENGTH_LONG).show()
                }
                currentRetryCount = 0

                // Show cached data if available, otherwise show what we have
                if (isSearchMode) {
                    if (allDetails.isNotEmpty()) {
                        updateRecyclerViews(allDetails)
                    } else {
                        // Try to show default data if no search results
                        showDefaultData()
                    }
                } else {
                    if (defaultDetails.isNotEmpty()) {
                        updateRecyclerViews(defaultDetails)
                    }
                }
            }
        } else {
            binding.progressBar.visibility = View.GONE
            if (isFragmentActive) {
                Toast.makeText(requireContext(), "Error loading data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processRequestQueue() {
        if (!isFragmentActive || isProcessingQueue || requestQueue.isEmpty()) {
            return
        }

        Log.d("HomeFragment", "Starting to process ${requestQueue.size} requests")
        isProcessingQueue = true
        processNextRequest()
    }

    private fun processNextRequest() {
        if (!isFragmentActive || requestQueue.isEmpty()) {
            Log.d("HomeFragment", "Request queue empty or fragment inactive, stopping processing")
            isProcessingQueue = false
            return
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime

        if (timeSinceLastRequest >= minRequestInterval) {
            // Execute the next request immediately
            executeNextRequest()
        } else {
            // Wait until minimum interval has passed
            val delay = minRequestInterval - timeSinceLastRequest
            Log.d("HomeFragment", "Waiting ${delay}ms before next request")
            handler.postDelayed({
                if (isFragmentActive) {
                    executeNextRequest()
                }
            }, delay)
        }
    }

    private fun executeNextRequest() {
        if (!isFragmentActive || requestQueue.isEmpty()) {
            isProcessingQueue = false
            return
        }

        val request = requestQueue.removeAt(0)
        lastRequestTime = System.currentTimeMillis()

        Log.d("HomeFragment", "Executing request for: ${request.placeName}")

        try {
            viewmodel.get_details(request.placeName)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error executing request for ${request.placeName}", e)
        }

        // Schedule next request with proper interval
        if (requestQueue.isNotEmpty()) {
            handler.postDelayed({
                if (isFragmentActive) {
                    processNextRequest()
                }
            }, minRequestInterval)
        } else {
            isProcessingQueue = false
            Log.d("HomeFragment", "All requests processed")
        }
    }

    private fun updateRecyclerViews(detailsList: List<Details>) {
        if (!isFragmentActive) return

        binding.progressBar.visibility = View.GONE
        if (detailsList.isNotEmpty()) {
            val shuffledList = detailsList.shuffled()
            val midPoint = (shuffledList.size + 1) / 2
            adapter.submitList(ArrayList(shuffledList.take(midPoint)))
            adapt.submitList(ArrayList(shuffledList.drop(midPoint)))

            Log.d("HomeFragment", "Updated RecyclerViews with ${detailsList.size} items")
        } else {
            adapter.submitList(emptyList())
            adapt.submitList(emptyList())
        }
    }

    private fun performSearch(query: String = binding.editTextSearch.text.toString().trim()) {
        if (!isFragmentActive) return

        if (query.isEmpty()) {
            Log.d("HomeFragment", "Empty search query, loading default data")
            showDefaultData()
            return
        }

        Log.d("HomeFragment", "Performing search for: $query")
        lastSearchQuery = query

        // Check cache first
        val cachedResults = searchCache[query.lowercase()]
        if (cachedResults != null && cachedResults.isNotEmpty()) {
            Log.d("HomeFragment", "Using cached results for: $query")
            isSearchMode = true
            allDetails.clear()
            allDetails.addAll(cachedResults)
            updateRecyclerViews(allDetails)
            return
        }

        // Cancel any pending requests and clear queue
        cancelAllPendingOperations()

        isSearchMode = true
        allDetails.clear()
        adapter.submitList(emptyList())
        adapt.submitList(emptyList())
        binding.progressBar.visibility = View.VISIBLE

        // Add delay before search to avoid rapid consecutive searches
        handler.postDelayed({
            if (isFragmentActive) {
                viewmodel.searchdata(query.lowercase())
            }
        }, 300)
    }

    private fun performSearchWithRateLimitCheck(query: String) {
        if (!isFragmentActive) return

        if (isRateLimited) {
            Log.d("HomeFragment", "Currently rate limited, checking for cached data")
            val cachedResults = searchCache[query.lowercase()]
            if (cachedResults != null && cachedResults.isNotEmpty()) {
                Log.d("HomeFragment", "Using cached results due to rate limit")
                isSearchMode = true
                allDetails.clear()
                allDetails.addAll(cachedResults)
                updateRecyclerViews(allDetails)
                Toast.makeText(requireContext(), "Showing cached results", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("HomeFragment", "No cached data available and rate limited, showing default data")
                Toast.makeText(requireContext(), "Rate limited. Please try again later.", Toast.LENGTH_LONG).show()
                showDefaultData()
            }
            return
        }

        // Proceed with normal search
        performSearch(query)
    }

    private fun loadDefaultData() {
        if (!isFragmentActive) return

        defaultDetails.clear()
        defaultDataCount = 0
        requestQueue.clear()
        binding.progressBar.visibility = View.VISIBLE

        Log.d("HomeFragment", "Loading default data")

        // Add default place requests to queue
        defaultPlaces.forEach { place ->
            requestQueue.add(RequestItem(place, false))
        }

        processRequestQueue()
    }

    private fun showDefaultData() {
        if (!isFragmentActive) return

        Log.d("HomeFragment", "Showing default data")

        // Cancel any search-related operations
        cancelAllPendingOperations()

        isSearchMode = false
        allDetails.clear()
        adapter.submitList(emptyList())
        adapt.submitList(emptyList())

        if (defaultDetails.isNotEmpty() && defaultDetails.size >= totalDefaultPlaces) {
            updateRecyclerViews(defaultDetails)
        } else {
            loadDefaultData()
        }
    }

    fun onBackPressed(): Boolean {
        if (isSearchMode) {
            binding.editTextSearch.setText("") // Clear search text
            showDefaultData()
            return true
        }
        return false
    }
}