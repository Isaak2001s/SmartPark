package com.example.smartpark.parking

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ParkingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val parkingCollection = firestore.collection("parking_areas")
    private val reservationsCollection = firestore.collection("reservations")

    /** Live-updating Flow of all parking areas. */
    fun observeParkingAreas(): Flow<List<ParkingArea>> = callbackFlow {
        val registration = parkingCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val areas = snapshot.toObjects(ParkingArea::class.java)
                trySend(areas)
            }
        }
        awaitClose { registration.remove() }
    }

    /** Live-updating Flow of reservations for a specific user. */
    fun observeUserReservations(userId: String): Flow<List<Reservation>> = callbackFlow {
        val registration = reservationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("reservedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reservations = snapshot.toObjects(Reservation::class.java)
                    trySend(reservations)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Creates a reservation atomically:
     * 1. Verifies the parking area still has available spots
     * 2. Decrements availableSpots by 1
     * 3. Creates the reservation document
     *
     * Uses a Firestore transaction to prevent race conditions (e.g., two users
     * reserving the last spot simultaneously).
     */
    suspend fun reserveSpot(
        userId: String,
        area: ParkingArea,
        durationMinutes: Int = 15
    ): Result<String> {
        return try {
            val areaRef = parkingCollection.document(area.id)
            val newReservationRef = reservationsCollection.document()

            firestore.runTransaction { transaction ->
                val freshArea = transaction.get(areaRef).toObject(ParkingArea::class.java)
                    ?: throw IllegalStateException("Parking area not found")

                if (freshArea.availableSpots <= 0) {
                    throw IllegalStateException("No spots available")
                }

                // Decrement available spots atomically
                transaction.update(areaRef, "availableSpots", FieldValue.increment(-1))

                // Re-evaluate occupancy level based on new ratio
                val newAvailable = freshArea.availableSpots - 1
                val newRatio = (freshArea.totalSpots - newAvailable).toFloat() / freshArea.totalSpots
                val newLevel = when {
                    newRatio >= 0.8f -> "high"
                    newRatio >= 0.5f -> "medium"
                    else -> "low"
                }
                transaction.update(areaRef, "occupancyLevel", newLevel)

                // Create reservation with 15-minute expiry
                val now = Timestamp.now()
                val expiresAt = Calendar.getInstance().apply {
                    time = now.toDate()
                    add(Calendar.MINUTE, durationMinutes)
                }.time

                val reservation = Reservation(
                    userId = userId,
                    parkingAreaId = area.id,
                    parkingAreaName = area.name,
                    pricePerHour = area.pricePerHour,
                    reservedAt = now,
                    expiresAt = Timestamp(expiresAt),
                    status = "active"
                )
                transaction.set(newReservationRef, reservation)
                null // transaction return value (we don't need it)
            }.await()

            Result.success(newReservationRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Cancels a reservation and returns the spot to availability. */
    suspend fun cancelReservation(reservation: Reservation): Result<Unit> {
        return try {
            val areaRef = parkingCollection.document(reservation.parkingAreaId)
            val reservationRef = reservationsCollection.document(reservation.id)

            firestore.runTransaction { transaction ->
                // ALL READS FIRST (Firestore rule)
                val freshArea = transaction.get(areaRef).toObject(ParkingArea::class.java)
                val freshReservation = transaction.get(reservationRef).toObject(Reservation::class.java)

                // Guard: don't cancel twice
                if (freshReservation?.status != "active") {
                    throw IllegalStateException("Reservation is not active")
                }

                // NOW DO WRITES
                transaction.update(areaRef, "availableSpots", FieldValue.increment(1))
                transaction.update(reservationRef, "status", "cancelled")

                // Recalculate occupancy level based on what the count WILL be after increment
                if (freshArea != null) {
                    val updatedAvailable = freshArea.availableSpots + 1
                    val newRatio = (freshArea.totalSpots - updatedAvailable).toFloat() / freshArea.totalSpots
                    val newLevel = when {
                        newRatio >= 0.8f -> "high"
                        newRatio >= 0.5f -> "medium"
                        else -> "low"
                    }
                    transaction.update(areaRef, "occupancyLevel", newLevel)
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}