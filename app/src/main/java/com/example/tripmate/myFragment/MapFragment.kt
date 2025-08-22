package com.example.tripmate.myFragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.tripmate.R
import com.example.tripmate.databinding.FragmentMapBinding
import com.example.tripmate.myMainMvvm.UserRepository
import com.example.tripmate.myMainMvvm.UserViewModel
import com.example.tripmate.myMainMvvm.UserViewModelFactory
import com.example.tripmate.myModel.SharedViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapFragment : Fragment(), OnMapReadyCallback {
    lateinit var binding: FragmentMapBinding
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharedViewModel: SharedViewModel

    // Set your destination coordinates here
    private var destinationLat = 28.6139 // Default: New Delhi
    private var destinationLng = 77.2090
    private var destinationTitle = "Destination"

    // Flag to track if destination data is loaded
    private var isDestinationDataLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val repository = UserRepository()
        val factory = UserViewModelFactory(repository)
        userViewModel = ViewModelProvider(requireActivity(), factory)
            .get(UserViewModel::class.java)

        // Initialize SharedViewModel
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        setupUI()
        setupMap()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val details = sharedViewModel.selectedDetails

        // Load image
        val image = details?.thumbnail?.source
        if (image != null && image.isNotEmpty()) {
            Glide.with(requireActivity())
                .load(image)
                .error(R.drawable.ic_launcher_background)
                .into(binding.image)
        } else {
            binding.image.setImageResource(R.drawable.img)
        }

        // Set text details
        binding.nameDetail.text = details?.title.toString()
        binding.descriptionDetail.text = details?.description.toString()
        binding.extractDetails.text = details?.extract.toString()

        // Set destination coordinates if available
        if (details?.coordinates?.lat != null && details?.coordinates?.lon != null) {
            destinationLat = details.coordinates.lat.toString().toDouble()
            destinationLng = details.coordinates.lon.toString().toDouble()
            destinationTitle = details.title ?: "Destination"
            isDestinationDataLoaded = true

            Log.d("MapFragment", "Destination loaded: $destinationLat, $destinationLng")

            // If map is already ready, show destination immediately
            if (googleMap != null) {
                showDestinationOnMap()
            }
        } else {
            Log.w("MapFragment", "Destination coordinates not available")
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupClickListeners() {
        var isMapVisible = false
        binding.btnShowMap.setOnClickListener {
            if (isMapVisible) {
                binding.mapCard.visibility = View.GONE
                isMapVisible = false
                binding.btnShowMap.text = "Check-In with Google Maps"
                binding.travelInfoPanel.visibility = View.GONE
                binding.mapLegend.visibility = View.GONE
            } else {
                binding.mapCard.visibility = View.VISIBLE
                binding.travelInfoPanel.visibility = View.VISIBLE
                checkLocationPermission()
                isMapVisible = true
                binding.mapLegend.visibility = View.VISIBLE
                binding.btnShowMap.text = "Close Map"

                // Show destination immediately if data is available
                if (isDestinationDataLoaded && googleMap != null) {
                    showDestinationOnMap()
                }
            }
        }
    }

    private fun observeViewModel() {
        userViewModel.walkingInfo.observe(viewLifecycleOwner) { info ->
            binding.tvWalkingInfo.text = info
        }

        userViewModel.drivingInfo.observe(viewLifecycleOwner) { info ->
            binding.tvDrivingInfo.text = info
        }

        userViewModel.routePolyline.observe(viewLifecycleOwner) { polyline ->
            drawRoute(polyline)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        Log.d("MapFragment", "Map is ready")

        // Show destination immediately if data is available
        if (isDestinationDataLoaded) {
            showDestinationOnMap()
        }

        checkLocationPermission()
    }

    private fun showDestinationOnMap() {
        googleMap?.let { map ->
            map.clear()

            // Add destination marker
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(destinationLat, destinationLng))
                    .title(destinationTitle)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            // Move camera
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(destinationLat, destinationLng),
                    12f
                )
            )

            // 🔑 Ensure hotels are added only AFTER map finishes rendering
            map.setOnMapLoadedCallback {
                showNearbyHotels(destinationLat, destinationLng)
            }
        }
    }


    private fun showNearbyHotels(lat: Double, lng: Double) {
        val apiKey = "\"GET_Your_Own_KEY\"" // your API key in strings.xml
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=$lat,$lng" +
                "&radius=3000" +   // 3 km radius
                "&type=lodging" +  // lodging = hotels, resorts, guesthouses
                "&key=$apiKey"

        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()
                connection.disconnect()

                val jsonObject = org.json.JSONObject(response)
                val results = jsonObject.getJSONArray("results")

                requireActivity().runOnUiThread {
                    for (i in 0 until results.length()) {
                        val hotel = results.getJSONObject(i)
                        val name = hotel.getString("name")
                        val geometry = hotel.getJSONObject("geometry").getJSONObject("location")
                        val hotelLat = geometry.getDouble("lat")
                        val hotelLng = geometry.getDouble("lng")

                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(hotelLat, hotelLng))
                                .title(name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                    }
                    Toast.makeText(requireContext(), "Nearby hotels added", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Error fetching hotels: ${e.message}")
            }
        }.start()
    }


    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationEnabled()
            googleMap?.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun checkLocationEnabled() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(), "Please turn on location", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun getCurrentLocation() {
        binding.progressBar.visibility = View.VISIBLE

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                binding.progressBar.visibility = View.GONE

                if (location != null) {
                    setupMapWithLocations(location.latitude, location.longitude)

                    // Add a small delay before getting directions to ensure map is fully ready
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        // Only get directions if destination data is loaded
                        if (isDestinationDataLoaded) {
                            userViewModel.getMapDirections(
                                location.latitude,
                                location.longitude,
                                destinationLat,
                                destinationLng
                            )
                        } else {
                            Log.w("MapFragment", "Cannot get directions: destination data not loaded")
                        }
                    }, 500) // 500ms delay

                } else {
                    // If lastLocation is null, try to get fresh location
                    requestCurrentLocation()
                }
            }.addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Log.e("MapFragment", "Failed to get location: ${exception.message}")
                requestCurrentLocation()
            }
        }
    }

    private fun requestCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 5000
                fastestInterval = 2000
                numUpdates = 1
            }

            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    binding.progressBar.visibility = View.GONE
                    locationResult.lastLocation?.let { location ->
                        fusedLocationClient.removeLocationUpdates(this)

                        setupMapWithLocations(location.latitude, location.longitude)

                        // Add delay before getting directions
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (isDestinationDataLoaded) {
                                userViewModel.getMapDirections(
                                    location.latitude,
                                    location.longitude,
                                    destinationLat,
                                    destinationLng
                                )
                            }
                        }, 800) // Slightly longer delay for fresh location
                    }
                }
            }

            binding.progressBar.visibility = View.VISIBLE
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireActivity().mainLooper)
        }
    }

    private fun setupMapWithLocations(currentLat: Double, currentLng: Double) {
        googleMap?.apply {
            // Clear existing markers and polylines
            clear()

            // Add current location marker
            addMarker(
                MarkerOptions()
                    .position(LatLng(currentLat, currentLng))
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )

            // Add destination marker (re-add it since we cleared the map)
            if (isDestinationDataLoaded) {
                addMarker(
                    MarkerOptions()
                        .position(LatLng(destinationLat, destinationLng))
                        .title(destinationTitle)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                // Move camera to show both locations
                val bounds = LatLngBounds.builder()
                    .include(LatLng(currentLat, currentLng))
                    .include(LatLng(destinationLat, destinationLng))
                    .build()

                moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
            } else {
                // If no destination, just show current location
                moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLat, currentLng), 15f))
            }
        }
    }

    private fun drawRoute(polyline: String) {
        try {
            val decodedPath = decodePolyline(polyline)
            val polylineOptions = PolylineOptions()
                .addAll(decodedPath)
                .color(resources.getColor(android.R.color.holo_blue_dark, null))
                .width(8f)

            googleMap?.addPolyline(polylineOptions)
            Log.d("MapFragment", "Route drawn successfully")

        } catch (e: Exception) {
            Log.e("MapFragment", "Error drawing route: ${e.message}")
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Add a small delay when permission is first granted
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                enableMyLocation()
            }, 1000) // 1 second delay
        }
    }
}