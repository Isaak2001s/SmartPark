package com.example.smartpark.parking

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Reservation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val parkingAreaId: String = "",
    val parkingAreaName: String = "",      // denormalized for easy display
    val pricePerHour: Double = 0.0,        // denormalized
    val reservedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val status: String = "active"          // "active" | "completed" | "cancelled"
)