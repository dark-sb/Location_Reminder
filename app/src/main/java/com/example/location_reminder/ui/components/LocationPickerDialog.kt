package com.example.location_reminder.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.location_reminder.ui.theme.bounceClickable
import com.example.location_reminder.ui.theme.pressScale
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.location.LocationGeocodingHelper
import com.example.location_reminder.data.location.PlaceSearchResult
import com.example.location_reminder.data.location.SuggestionFilterHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val TAG = "LocationPickerDialog"

// OpenFreeMap Bright Style - Fully featured vector tiles with roads and POIs
private const val OPEN_FREE_MAP_BRIGHT_STYLE_URL = "https://tiles.openfreemap.org/styles/bright"

private const val MAP_ATTRIBUTION_NORMAL = "OpenFreeMap © OpenMapTiles © OpenStreetMap"
private const val MAP_ATTRIBUTION_SATELLITE = "Esri, Maxar, Earthstar Geographics"

private enum class MapMode {
    NORMAL,
    SATELLITE
}

// Esri World Imagery + Reference Boundaries style definition
private const val ESRI_SATELLITE_STYLE_JSON = """{
  "version": 8,
  "sources": {
    "esri-satellite": {
      "type": "raster",
      "tiles": [
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
      ],
      "tileSize": 256,
      "maxzoom": 19,
      "attribution": "Esri, Maxar, Earthstar Geographics"
    },
    "esri-labels": {
      "type": "raster",
      "tiles": [
        "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
      ],
      "tileSize": 256,
      "maxzoom": 19
    }
  },
  "layers": [
    {
      "id": "esri-satellite-layer",
      "type": "raster",
      "source": "esri-satellite",
      "minzoom": 0,
      "maxzoom": 22
    },
    {
      "id": "esri-labels-layer",
      "type": "raster",
      "source": "esri-labels",
      "minzoom": 0,
      "maxzoom": 22
    }
  ]
}"""

private fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNet = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNet) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    initialLatitude: Double = 0.0,
    initialLongitude: Double = 0.0,
    initialLocationName: String = "",
    recentLocations: List<LocationHistoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onLocationConfirmed: (PlaceSearchResult) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Determine initial coordinates: use passed coordinates if valid, otherwise do NOT use hardcoded center
    val hasValidInitial = (initialLatitude != 0.0 || initialLongitude != 0.0) &&
            initialLatitude in -90.0..90.0 && initialLongitude in -180.0..180.0

    var selectedPoint by remember {
        mutableStateOf<LatLng?>(if (hasValidInitial) LatLng(initialLatitude, initialLongitude) else null)
    }
    var selectedPlaceTitle by remember {
        mutableStateOf(if (initialLocationName.isNotBlank()) initialLocationName else "")
    }
    var detectedLocationName by remember { mutableStateOf("") }
    var isCustomName by remember {
        mutableStateOf(initialLocationName.isNotBlank())
    }
    var selectedAddress by remember { mutableStateOf("") }
    var isReverseGeocoding by remember { mutableStateOf(false) }
    var isGpsLocating by remember { mutableStateOf(false) }

    // Custom Name Editing State
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var editNameError by remember { mutableStateOf<String?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Map & Marker references
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var markerRef by remember { mutableStateOf<Marker?>(null) }

    // Map state & clean error handling
    var isNetworkOnline by remember { mutableStateOf(isNetworkConnected(context)) }
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Map camera padding calculation to keep marker visually centered in unobstructed viewport
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 120.dp.roundToPx() }
    val bottomPaddingPx = with(density) { 260.dp.roundToPx() }

    // Map style mode persistence (persisting user preference across dialog openings)
    val prefs = remember { context.getSharedPreferences("map_picker_prefs", Context.MODE_PRIVATE) }
    var currentMapMode by remember {
        val savedMode = prefs.getString("selected_map_mode", MapMode.NORMAL.name)
        mutableStateOf(if (savedMode == MapMode.SATELLITE.name) MapMode.SATELLITE else MapMode.NORMAL)
    }

    fun applyMapStyle(map: MapLibreMap, mode: MapMode) {
        val styleBuilder = when (mode) {
            MapMode.NORMAL -> Style.Builder().fromUri(OPEN_FREE_MAP_BRIGHT_STYLE_URL)
            MapMode.SATELLITE -> Style.Builder().fromJson(ESRI_SATELLITE_STYLE_JSON)
        }
        map.setStyle(styleBuilder) { _ ->
            isMapLoaded = true
            mapLoadError = null

            // Initial camera position: focus on selectedPoint if present
            val currentPt = selectedPoint
            if (currentPt != null) {
                if (map.cameraPosition.zoom < 2.0) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(currentPt)
                        .zoom(16.0)
                        .build()
                }

                try {
                    markerRef?.remove()
                } catch (_: Exception) {}
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(currentPt)
                        .title(selectedPlaceTitle.ifBlank { "Selected Location" })
                        .snippet(selectedAddress)
                )
                markerRef = marker
            }
        }
    }

    // Handles applying a high-accuracy GPS fix to the map and reverse geocoding it
    fun applyGpsLocation(loc: Location) {
        val newPoint = LatLng(loc.latitude, loc.longitude)
        selectedPoint = newPoint
        isCustomName = false
        detectedLocationName = ""
        mapLibreMapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 16.5))

        try {
            markerRef?.remove()
        } catch (_: Exception) {}

        mapLibreMapRef?.let { map ->
            markerRef = map.addMarker(
                MarkerOptions()
                    .position(newPoint)
                    .title("Current Location")
            )
        }

        coroutineScope.launch {
            isReverseGeocoding = true
            val result = LocationGeocodingHelper.reverseGeocode(context, loc.latitude, loc.longitude)
            if (result != null) {
                detectedLocationName = result.title
                if (!isCustomName) {
                    selectedPlaceTitle = result.title
                }
                selectedAddress = result.address
            } else {
                detectedLocationName = "Unknown location"
                if (!isCustomName) {
                    selectedPlaceTitle = "Unknown location"
                }
                selectedAddress = "Unknown location"
            }
            markerRef?.title = selectedPlaceTitle
            markerRef?.snippet = selectedAddress
            isReverseGeocoding = false
        }
    }

    // Requests the best available GPS accuracy using FusedLocationProviderClient with PRIORITY_HIGH_ACCURACY
    fun requestBestGpsLocation(onComplete: (() -> Unit)? = null) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            onComplete?.invoke()
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        isGpsLocating = true

        // 1. Check lastLocation first for immediate response
        try {
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && selectedPoint == null) {
                    applyGpsLocation(lastLoc)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on lastLocation", e)
        }

        // 2. Request fresh high-accuracy GPS location
        val cts = CancellationTokenSource()
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { freshLoc ->
                    isGpsLocating = false
                    if (freshLoc != null) {
                        applyGpsLocation(freshLoc)
                    } else if (selectedPoint == null) {
                        // Fallback to LocationManager if FusedLocation returned null
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val fallbackLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (fallbackLoc != null) {
                            applyGpsLocation(fallbackLoc)
                        } else {
                            Toast.makeText(context, "Location unavailable. Please ensure GPS is enabled.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    onComplete?.invoke()
                }
                .addOnFailureListener {
                    isGpsLocating = false
                    onComplete?.invoke()
                }
        } catch (e: SecurityException) {
            isGpsLocating = false
            Log.e(TAG, "SecurityException on getCurrentLocation", e)
            onComplete?.invoke()
        }
    }

    // Permission launcher for "My Location" and auto-GPS on open
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            requestBestGpsLocation()
        } else {
            Toast.makeText(context, "Location permission is required to detect current location.", Toast.LENGTH_SHORT).show()
        }
    }

    // On opening the picker: automatically request real current GPS location if no valid initial location
    LaunchedEffect(Unit) {
        if (!hasValidInitial) {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fineGranted || coarseGranted) {
                requestBestGpsLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            // Reverse geocode the initial location if address is empty
            if (selectedAddress.isBlank()) {
                isReverseGeocoding = true
                val result = LocationGeocodingHelper.reverseGeocode(context, initialLatitude, initialLongitude)
                if (result != null) {
                    detectedLocationName = result.title
                    selectedAddress = result.address
                    if (selectedPlaceTitle.isBlank()) {
                        selectedPlaceTitle = result.title
                        isCustomName = false
                    }
                }
                isReverseGeocoding = false
            }
        }
    }

    fun performSearch() {
        if (searchQuery.trim().isBlank()) return
        coroutineScope.launch {
            isSearching = true
            searchError = null
            val results = LocationGeocodingHelper.searchPlaces(context, searchQuery.trim())
            isSearching = false
            searchResults = results
            if (results.isEmpty()) {
                searchError = "No locations found for '${searchQuery.trim()}'"
            }
        }
    }

    val canConfirm = selectedPoint != null && !isGpsLocating && !isReverseGeocoding

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Select Location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentMapMode == MapMode.NORMAL) MAP_ATTRIBUTION_NORMAL else MAP_ATTRIBUTION_SATELLITE,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.pressScale(0.90f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                selectedPoint?.let { pt ->
                                    onLocationConfirmed(
                                        PlaceSearchResult(
                                            title = selectedPlaceTitle.ifBlank { "Unknown location" },
                                            address = selectedAddress.ifBlank { "Unknown location" },
                                            latitude = pt.latitude,
                                            longitude = pt.longitude
                                        )
                                    )
                                }
                            },
                            enabled = canConfirm,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .pressScale(0.95f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Interactive MapLibre Map Component
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapLibre.getInstance(ctx)
                        MapView(ctx).apply {
                            mapViewRef = this
                            getMapAsync { map ->
                                mapLibreMapRef = map

                                // Apply camera padding so target coordinates are centered in the unobstructed upper viewport
                                map.setPadding(0, topPaddingPx, 0, bottomPaddingPx)
                                map.setMaxZoomPreference(20.0)
                                map.setMinZoomPreference(2.0)
                                map.uiSettings.isLogoEnabled = false
                                map.uiSettings.isAttributionEnabled = false

                                // Load selected style (Normal Bright Vector or Satellite Imagery)
                                applyMapStyle(map, currentMapMode)

                                // Interactive Tap-to-Select Listener
                                map.addOnMapClickListener { latLng ->
                                    selectedPoint = latLng
                                    searchResults = emptyList()
                                    isCustomName = false
                                    detectedLocationName = ""

                                    try {
                                        markerRef?.remove()
                                    } catch (_: Exception) {}

                                    markerRef = map.addMarker(
                                        MarkerOptions()
                                            .position(latLng)
                                            .title("Selected Point")
                                    )

                                    // Smoothly pan camera so the tapped point stays in the unobstructed viewport
                                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng))

                                    // Reverse geocode tapped coordinates
                                    coroutineScope.launch {
                                        isReverseGeocoding = true
                                        val res = LocationGeocodingHelper.reverseGeocode(ctx, latLng.latitude, latLng.longitude)
                                        if (res != null) {
                                            detectedLocationName = res.title
                                            if (!isCustomName) {
                                                selectedPlaceTitle = res.title
                                            }
                                            selectedAddress = res.address
                                        } else {
                                            detectedLocationName = "Unknown location"
                                            if (!isCustomName) {
                                                selectedPlaceTitle = "Unknown location"
                                            }
                                            selectedAddress = "Unknown location"
                                        }
                                        markerRef?.title = selectedPlaceTitle
                                        markerRef?.snippet = selectedAddress
                                        isReverseGeocoding = false
                                    }
                                    true
                                }
                            }

                            // Capture map load failure for clean error presentation
                            addOnDidFailLoadingMapListener { errorMsg ->
                                mapLoadError = errorMsg ?: "Map tiles could not be loaded."
                            }
                        }
                    },
                    update = {
                        selectedPoint?.let { pt ->
                            markerRef?.position = pt
                        }
                    }
                )

                // Clean Offline Warning Banner if device has no connectivity
                if (!isNetworkOnline) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 68.dp)
                            .align(Alignment.TopCenter),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device is offline. Some map tiles and search results may be unavailable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Clean Map Load Error Warning
                if (mapLoadError != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 68.dp)
                            .align(Alignment.TopCenter),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Map Tile Loading Warning",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tiles could not be loaded from map provider. Place search, GPS current location, and coordinate confirmation remain fully functional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    mapLoadError = null
                                    mapLibreMapRef?.let { applyMapStyle(it, currentMapMode) }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry Loading Map", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Search Bar Overlay at Top
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp)
                            )

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    if (it.isBlank()) {
                                        searchResults = emptyList()
                                        searchError = null
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "Search place (e.g. Hospital, College)...",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            searchResults = emptyList()
                                            searchError = null
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { performSearch() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.pressScale(0.95f)
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Find")
                                }
                            }
                        }
                    }

                    // Map Type Mode Selector (Normal vs Satellite)
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentMapMode == MapMode.NORMAL,
                            onClick = {
                                if (currentMapMode != MapMode.NORMAL) {
                                    currentMapMode = MapMode.NORMAL
                                    prefs.edit().putString("selected_map_mode", MapMode.NORMAL.name).apply()
                                    mapLibreMapRef?.let { applyMapStyle(it, MapMode.NORMAL) }
                                }
                            },
                            label = {
                                Text(
                                    "Normal Map",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (currentMapMode == MapMode.NORMAL) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Normal Map",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.pressScale(0.96f)
                        )

                        FilterChip(
                            selected = currentMapMode == MapMode.SATELLITE,
                            onClick = {
                                if (currentMapMode != MapMode.SATELLITE) {
                                    currentMapMode = MapMode.SATELLITE
                                    prefs.edit().putString("selected_map_mode", MapMode.SATELLITE.name).apply()
                                    mapLibreMapRef?.let { applyMapStyle(it, MapMode.SATELLITE) }
                                }
                            },
                            label = {
                                Text(
                                    "Satellite Map",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (currentMapMode == MapMode.SATELLITE) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Satellite,
                                    contentDescription = "Satellite Map",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.pressScale(0.96f)
                        )
                    }

                    // Matching Saved Locations from History (prioritizing custom names)
                    val matchingSavedLocations = remember(searchQuery, recentLocations) {
                        if (recentLocations.isEmpty() || searchQuery.trim().isBlank()) emptyList()
                        else SuggestionFilterHelper.filterLocations(query = searchQuery, history = recentLocations, maxItems = 5)
                    }

                    // Search Results & Saved Locations Dropdown List
                    AnimatedVisibility(visible = searchResults.isNotEmpty() || searchError != null || matchingSavedLocations.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .heightIn(max = 240.dp),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (searchError != null && matchingSavedLocations.isEmpty() && searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = searchError ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                LazyColumn {
                                    if (matchingSavedLocations.isNotEmpty()) {
                                        item(key = "header_saved_locations") {
                                            Text(
                                                text = "SAVED LOCATIONS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                        items(
                                            items = matchingSavedLocations,
                                            key = { "saved_${it.id}_${it.name}" },
                                            contentType = { "saved_location" }
                                        ) { savedLoc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bounceClickable {
                                                        val newPoint = LatLng(savedLoc.latitude, savedLoc.longitude)
                                                        selectedPoint = newPoint
                                                        selectedPlaceTitle = savedLoc.name
                                                        isCustomName = true
                                                        detectedLocationName = ""
                                                        selectedAddress = savedLoc.address

                                                        mapLibreMapRef?.animateCamera(
                                                            CameraUpdateFactory.newLatLngZoom(newPoint, 16.5)
                                                        )

                                                        try {
                                                            markerRef?.remove()
                                                        } catch (_: Exception) {}

                                                        markerRef = mapLibreMapRef?.addMarker(
                                                            MarkerOptions()
                                                                .position(newPoint)
                                                                .title(savedLoc.name)
                                                                .snippet(savedLoc.address)
                                                        )

                                                        searchResults = emptyList()
                                                        searchQuery = ""
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = savedLoc.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                        ) {
                                                            Text(
                                                                text = "Saved",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    if (savedLoc.address.isNotBlank() && savedLoc.address != savedLoc.name) {
                                                        Text(
                                                            text = savedLoc.address,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (searchResults.isNotEmpty()) {
                                        if (matchingSavedLocations.isNotEmpty()) {
                                            item(key = "header_search_results") {
                                                Text(
                                                    text = "MAP SEARCH RESULTS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                        items(
                                            items = searchResults,
                                            key = { "${it.latitude}_${it.longitude}_${it.title}" },
                                            contentType = { "search_result" }
                                        ) { result ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bounceClickable {
                                                        val newPoint = LatLng(result.latitude, result.longitude)
                                                        selectedPoint = newPoint
                                                        isCustomName = false
                                                        detectedLocationName = result.title
                                                        selectedPlaceTitle = result.title
                                                        selectedAddress = result.address

                                                        mapLibreMapRef?.animateCamera(
                                                            CameraUpdateFactory.newLatLngZoom(newPoint, 16.5)
                                                        )

                                                        try {
                                                            markerRef?.remove()
                                                        } catch (_: Exception) {}

                                                        markerRef = mapLibreMapRef?.addMarker(
                                                            MarkerOptions()
                                                                .position(newPoint)
                                                                .title(result.title)
                                                                .snippet(result.address)
                                                        )

                                                        // Dismiss dropdown
                                                        searchResults = emptyList()
                                                        searchQuery = ""
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = result.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (result.address.isNotBlank() && result.address != result.title) {
                                                        Text(
                                                            text = result.address,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Action Button: My Location (Right Side)
                FloatingActionButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasFine || hasCoarse) {
                            requestBestGpsLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 270.dp, end = 16.dp)
                        .pressScale(0.92f),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    if (isGpsLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use my current location")
                    }
                }

                // Bottom Confirmation & Selected Coordinates Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val displayTitle = when {
                                    isGpsLocating && selectedPoint == null -> "Acquiring GPS location..."
                                    isReverseGeocoding && selectedPlaceTitle.isBlank() -> "Resolving place name..."
                                    selectedPoint == null -> "No location selected"
                                    selectedPlaceTitle.isNotBlank() -> selectedPlaceTitle
                                    else -> "Unknown location"
                                }
                                val displayAddress = when {
                                    isGpsLocating && selectedPoint == null -> "Detecting device using best GPS accuracy..."
                                    selectedPoint == null -> "Tap on map or search above to pick a location"
                                    selectedAddress.isNotBlank() -> selectedAddress
                                    else -> "Unknown location"
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCustomName && selectedPlaceTitle.isNotBlank() && selectedPoint != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Custom",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (selectedPoint != null && !isGpsLocating) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                editNameInput = selectedPlaceTitle
                                                editNameError = null
                                                showEditNameDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .pressScale(0.95f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Edit Name",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                if (isCustomName && detectedLocationName.isNotBlank() &&
                                    !detectedLocationName.equals(selectedPlaceTitle, ignoreCase = true) &&
                                    !detectedLocationName.equals("Unknown location", ignoreCase = true)
                                ) {
                                    Text(
                                        text = "Detected: $detectedLocationName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = displayAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Coordinates summary pill (only visible when a point is selected)
                        selectedPoint?.let { pt ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Lat: ${String.format(Locale.US, "%.5f", pt.latitude)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Lng: ${String.format(Locale.US, "%.5f", pt.longitude)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        // Bottom Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    selectedPoint?.let { pt ->
                                        onLocationConfirmed(
                                            PlaceSearchResult(
                                                title = selectedPlaceTitle.ifBlank { "Unknown location" },
                                                address = selectedAddress.ifBlank { "Unknown location" },
                                                latitude = pt.latitude,
                                                longitude = pt.longitude
                                            )
                                        )
                                    }
                                },
                                enabled = canConfirm,
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            ) {
                                Text("Select Location")
                            }
                        }
                    }
                }
            }
        }

        // Custom Location Name Edit Dialog
        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = {
                    showEditNameDialog = false
                    editNameError = null
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = "Edit Location Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Enter a custom name for this location (e.g. Library, Home, Office). Geographic coordinates will remain unchanged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = editNameInput,
                            onValueChange = {
                                editNameInput = it
                                if (editNameError != null) editNameError = null
                            },
                            label = { Text("Location Name *") },
                            placeholder = { Text("e.g. Library") },
                            singleLine = true,
                            isError = editNameError != null,
                            supportingText = editNameError?.let { { Text(it) } },
                            trailingIcon = {
                                if (editNameInput.isNotBlank()) {
                                    IconButton(onClick = { editNameInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val trimmed = editNameInput.trim()
                                    if (trimmed.isBlank()) {
                                        editNameError = "Location name cannot be empty"
                                    } else {
                                        selectedPlaceTitle = trimmed
                                        isCustomName = true
                                        markerRef?.title = selectedPlaceTitle
                                        showEditNameDialog = false
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (detectedLocationName.isNotBlank() &&
                            !detectedLocationName.equals(editNameInput.trim(), ignoreCase = true) &&
                            !detectedLocationName.equals("Unknown location", ignoreCase = true)
                        ) {
                            Surface(
                                onClick = {
                                    editNameInput = detectedLocationName
                                    editNameError = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(0.97f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Reset to detected name:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = detectedLocationName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = editNameInput.trim()
                            if (trimmed.isBlank()) {
                                editNameError = "Location name cannot be empty"
                            } else {
                                selectedPlaceTitle = trimmed
                                isCustomName = true
                                markerRef?.title = selectedPlaceTitle
                                showEditNameDialog = false
                            }
                        },
                        modifier = Modifier.pressScale(0.95f)
                    ) {
                        Text("Save Name")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditNameDialog = false
                            editNameError = null
                        },
                        modifier = Modifier.pressScale(0.95f)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Lifecycle Observer for MapView inside Dialog
    DisposableEffect(lifecycleOwner, mapViewRef) {
        val mapView = mapViewRef
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
        }
    }
}
