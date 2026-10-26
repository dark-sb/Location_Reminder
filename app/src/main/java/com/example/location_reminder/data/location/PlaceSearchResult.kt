package com.example.location_reminder.data.location

data class PlaceSearchResult(
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String = ""
)
