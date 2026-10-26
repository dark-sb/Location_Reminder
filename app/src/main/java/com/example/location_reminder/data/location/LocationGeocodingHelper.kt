package com.example.location_reminder.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object LocationGeocodingHelper {

    private const val TAG = "LocationGeocodingHelper"

    // Regex to match Open Location Codes (Plus Codes) such as "8V8V+7V", "7JVW52GR+3X", "CWJ3+42"
    private val PLUS_CODE_PATTERN = Regex("""\b[23456789CFGHJMPQRVWX]{2,8}\+[23456789CFGHJMPQRVWX]{2,7}\b""", RegexOption.IGNORE_CASE)

    // Regex to detect strings that are only numbers or coordinates (e.g. "18.4536, 73.8563" or "411046")
    private val COORDINATES_OR_NUMBERS = Regex("""^[\d\s.,+-]+$""")

    /**
     * Determines whether a candidate text is invalid as a human-readable place name.
     * Rejects null, blank, Plus Codes, raw coordinates, and placeholder strings.
     */
    fun isInvalidPlaceName(text: String?): Boolean {
        if (text.isNullOrBlank()) return true
        val trimmed = text.trim()
        if (trimmed.equals("null", ignoreCase = true) || trimmed.equals("unnamed road", ignoreCase = true)) return true
        if (trimmed.length <= 1) return true
        if (PLUS_CODE_PATTERN.containsMatchIn(trimmed) || (trimmed.contains("+") && trimmed.length <= 16)) return true
        if (COORDINATES_OR_NUMBERS.matches(trimmed)) return true
        return false
    }

    /**
     * Extracts a clean, human-readable place title from an Android Geocoder [Address].
     */
    private fun extractCleanTitle(address: Address, fallbackQuery: String? = null): String {
        val candidates = listOfNotNull(
            address.featureName?.takeUnless { isInvalidPlaceName(it) },
            address.subLocality?.takeUnless { isInvalidPlaceName(it) },
            address.thoroughfare?.takeUnless { isInvalidPlaceName(it) },
            address.locality?.takeUnless { isInvalidPlaceName(it) },
            address.subAdminArea?.takeUnless { isInvalidPlaceName(it) },
            address.adminArea?.takeUnless { isInvalidPlaceName(it) },
            address.countryName?.takeUnless { isInvalidPlaceName(it) }
        )
        val best = candidates.firstOrNull()
        if (!best.isNullOrBlank()) return best
        if (!fallbackQuery.isNullOrBlank() && !isInvalidPlaceName(fallbackQuery)) {
            return fallbackQuery.trim()
        }
        return "Unknown location"
    }

    /**
     * Builds a comprehensive human-readable address with area, road, city, state, and country.
     * Guaranteed not to include Plus Codes or raw coordinates.
     */
    private fun extractCleanAddress(address: Address, title: String): String {
        val road = listOfNotNull(
            address.subThoroughfare?.takeUnless { isInvalidPlaceName(it) },
            address.thoroughfare?.takeUnless { isInvalidPlaceName(it) }
        ).joinToString(" ").takeIf { it.isNotBlank() }

        val area = address.subLocality?.takeUnless { isInvalidPlaceName(it) }
        val city = address.locality?.takeUnless { isInvalidPlaceName(it) }
        val district = address.subAdminArea?.takeUnless { isInvalidPlaceName(it) || it.equals(city, ignoreCase = true) }
        val state = address.adminArea?.takeUnless { isInvalidPlaceName(it) }
        val country = address.countryName?.takeUnless { isInvalidPlaceName(it) }

        val components = listOfNotNull(road, area, city, district, state, country)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (components.isNotEmpty()) {
            return components.joinToString(", ")
        }

        val rawLines = (0..address.maxAddressLineIndex)
            .mapNotNull { address.getAddressLine(it) }
            .joinToString(", ")

        val cleaned = rawLines
            .replace(PLUS_CODE_PATTERN, "")
            .replace(Regex("""\s*,\s*,"""), ",")
            .replace(Regex("""^\s*,\s*"""), "")
            .replace(Regex("""\s*,\s*$"""), "")
            .trim()

        if (cleaned.isNotBlank() && !isInvalidPlaceName(cleaned)) {
            return cleaned
        }

        return if (title != "Unknown location") title else "Unknown location"
    }

    /**
     * Searches places by name or address using Android's Geocoder with OpenStreetMap Nominatim fallback.
     * Executes safely on Dispatchers.IO.
     * Returns structured place names, full addresses (area, road, city, state, country), and coordinates.
     */
    suspend fun searchPlaces(
        context: Context,
        query: String,
        maxResults: Int = 5
    ): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext emptyList()
        }

        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(trimmedQuery, maxResults) ?: emptyList()

                if (addresses.isNotEmpty()) {
                    return@withContext addresses.map { address ->
                        val title = extractCleanTitle(address, fallbackQuery = trimmedQuery)
                        val fullAddress = extractCleanAddress(address, title)
                        PlaceSearchResult(
                            title = title,
                            address = fullAddress,
                            latitude = address.latitude,
                            longitude = address.longitude
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Android Geocoder search failed for '$trimmedQuery', trying OSM Nominatim fallback", e)
            }
        }

        // Fallback to OSM Nominatim
        searchPlacesNominatim(trimmedQuery, maxResults)
    }

    /**
     * Reverse-geocodes latitude and longitude into a readable place name and address.
     * Executes safely on Dispatchers.IO.
     * Shows actual human-readable place name and address (area, road, city, state, country),
     * never Plus Codes or raw coordinates. Falls back to "Unknown location" if unresolvable.
     */
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): PlaceSearchResult? = withContext(Dispatchers.IO) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return@withContext null
        }

        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
                val address = addresses.firstOrNull()
                if (address != null) {
                    val title = extractCleanTitle(address)
                    val fullAddress = extractCleanAddress(address, title)
                    return@withContext PlaceSearchResult(
                        title = title,
                        address = fullAddress,
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Android Geocoder reverse geocode failed for ($latitude, $longitude), trying OSM Nominatim fallback", e)
            }
        }

        // Fallback to OSM Nominatim
        reverseGeocodeNominatim(latitude, longitude)
    }

    /**
     * Searches OpenStreetMap Nominatim with structured addressdetails=1.
     */
    private fun searchPlacesNominatim(query: String, maxResults: Int): List<PlaceSearchResult> {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=$maxResults")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "LocationReminderApp/1.0 (Android)")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonText)
                val list = mutableListOf<PlaceSearchResult>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val lat = item.optDouble("lat", 0.0)
                    val lon = item.optDouble("lon", 0.0)
                    if (lat == 0.0 && lon == 0.0) continue

                    val addressObj = item.optJSONObject("address")
                    val poiName = item.optString("name").takeUnless { isInvalidPlaceName(it) }
                    val road = (addressObj?.optString("road")
                        ?: addressObj?.optString("street")
                        ?: addressObj?.optString("pedestrian")
                        ?: addressObj?.optString("highway"))?.takeUnless { isInvalidPlaceName(it) }
                    val area = (addressObj?.optString("suburb")
                        ?: addressObj?.optString("neighbourhood")
                        ?: addressObj?.optString("residential")
                        ?: addressObj?.optString("quarter"))?.takeUnless { isInvalidPlaceName(it) }
                    val city = (addressObj?.optString("city")
                        ?: addressObj?.optString("town")
                        ?: addressObj?.optString("village")
                        ?: addressObj?.optString("municipality"))?.takeUnless { isInvalidPlaceName(it) }
                    val district = (addressObj?.optString("county")
                        ?: addressObj?.optString("city_district")
                        ?: addressObj?.optString("subdistrict"))?.takeUnless { isInvalidPlaceName(it) || it.equals(city, ignoreCase = true) }
                    val state = addressObj?.optString("state")?.takeUnless { isInvalidPlaceName(it) }
                    val country = addressObj?.optString("country")?.takeUnless { isInvalidPlaceName(it) }

                    val title = poiName ?: area ?: road ?: city ?: district ?: state ?: query.trim()

                    val addressParts = listOfNotNull(
                        poiName.takeIf { it != title },
                        road,
                        area,
                        city,
                        district,
                        state,
                        country
                    ).map { it.trim() }.filter { it.isNotBlank() }.distinct()

                    val formattedAddress = if (addressParts.isNotEmpty()) {
                        addressParts.joinToString(", ")
                    } else {
                        item.optString("display_name", "")
                            .replace(PLUS_CODE_PATTERN, "")
                            .replace(Regex("""\s*,\s*,"""), ",")
                            .replace(Regex("""^\s*,\s*"""), "")
                            .replace(Regex("""\s*,\s*$"""), "")
                            .trim()
                            .ifBlank { title }
                    }

                    list.add(
                        PlaceSearchResult(
                            title = title,
                            address = formattedAddress,
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
                return list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Nominatim search fallback failed", e)
        }
        return emptyList()
    }

    /**
     * Reverse-geocodes with OpenStreetMap Nominatim using structured addressdetails=1.
     */
    private fun reverseGeocodeNominatim(latitude: Double, longitude: Double): PlaceSearchResult? {
        try {
            val url = URL(String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json&addressdetails=1", latitude, longitude))
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "LocationReminderApp/1.0 (Android)")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonText)
                val addressObj = obj.optJSONObject("address")

                val poiName = obj.optString("name").takeUnless { isInvalidPlaceName(it) }
                val road = (addressObj?.optString("road")
                    ?: addressObj?.optString("street")
                    ?: addressObj?.optString("pedestrian")
                    ?: addressObj?.optString("highway"))?.takeUnless { isInvalidPlaceName(it) }
                val area = (addressObj?.optString("suburb")
                    ?: addressObj?.optString("neighbourhood")
                    ?: addressObj?.optString("residential")
                    ?: addressObj?.optString("quarter"))?.takeUnless { isInvalidPlaceName(it) }
                val city = (addressObj?.optString("city")
                    ?: addressObj?.optString("town")
                    ?: addressObj?.optString("village")
                    ?: addressObj?.optString("municipality"))?.takeUnless { isInvalidPlaceName(it) }
                val district = (addressObj?.optString("county")
                    ?: addressObj?.optString("city_district")
                    ?: addressObj?.optString("subdistrict")
                    ?: addressObj?.optString("state_district"))?.takeUnless { isInvalidPlaceName(it) || it.equals(city, ignoreCase = true) }
                val state = addressObj?.optString("state")?.takeUnless { isInvalidPlaceName(it) }
                val country = addressObj?.optString("country")?.takeUnless { isInvalidPlaceName(it) }

                val title = poiName ?: area ?: road ?: city ?: district ?: state ?: country ?: "Unknown location"

                val addressParts = listOfNotNull(
                    poiName.takeIf { it != title },
                    road,
                    area,
                    city,
                    district,
                    state,
                    country
                ).map { it.trim() }.filter { it.isNotBlank() }.distinct()

                val formattedAddress = if (addressParts.isNotEmpty()) {
                    addressParts.joinToString(", ")
                } else {
                    val rawDisplay = obj.optString("display_name", "")
                        .replace(PLUS_CODE_PATTERN, "")
                        .replace(Regex("""\s*,\s*,"""), ",")
                        .replace(Regex("""^\s*,\s*"""), "")
                        .replace(Regex("""\s*,\s*$"""), "")
                        .trim()
                    if (rawDisplay.isNotBlank()) rawDisplay else title
                }

                return PlaceSearchResult(
                    title = title,
                    address = formattedAddress,
                    latitude = latitude,
                    longitude = longitude
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Nominatim reverse geocode fallback failed", e)
        }
        return PlaceSearchResult(
            title = "Unknown location",
            address = "Unknown location",
            latitude = latitude,
            longitude = longitude
        )
    }
}
