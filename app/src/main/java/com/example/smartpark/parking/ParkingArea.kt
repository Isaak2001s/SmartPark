package com.example.smartpark.parking

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * Represents a parking area fetched from Firestore.
 *
 * The no-arg default values are REQUIRED for Firestore's automatic deserialization
 * (it uses reflection and needs to instantiate the class before populating fields).
 */
data class ParkingArea(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: GeoPoint? = null,
    val totalSpots: Int = 0,
    val availableSpots: Int = 0,
    val pricePerHour: Double = 0.0,
    val address: String = "",
    val occupancyLevel: String = "low" // "low" | "medium" | "high"
) {
    /** Helper: is this area bookable? */
    val hasAvailability: Boolean get() = availableSpots > 0

    /** Helper: percentage of spots occupied (0.0 to 1.0) */
    val occupancyRatio: Float
        get() = if (totalSpots == 0) 0f
        else (totalSpots - availableSpots).toFloat() / totalSpots
}