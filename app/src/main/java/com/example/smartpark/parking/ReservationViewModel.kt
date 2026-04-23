package com.example.smartpark.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** State for the reservation action (booking a spot). */
sealed class ReservationActionState {
    object Idle : ReservationActionState()
    object Loading : ReservationActionState()
    data class Success(val reservationId: String) : ReservationActionState()
    data class Error(val message: String) : ReservationActionState()
}

/** State for the My Reservations list. */
data class MyReservationsUiState(
    val reservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ReservationViewModel(
    private val parkingRepository: ParkingRepository = ParkingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _actionState = MutableStateFlow<ReservationActionState>(ReservationActionState.Idle)
    val actionState: StateFlow<ReservationActionState> = _actionState.asStateFlow()

    private val _myReservations = MutableStateFlow(MyReservationsUiState())
    val myReservations: StateFlow<MyReservationsUiState> = _myReservations.asStateFlow()

    init {
        observeMyReservations()
    }

    private fun observeMyReservations() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            parkingRepository.observeUserReservations(userId)
                .catch { e ->
                    _myReservations.value = _myReservations.value.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
                .collect { list ->
                    _myReservations.value = MyReservationsUiState(
                        reservations = list,
                        isLoading = false
                    )
                }
        }
    }

    fun reserveSpot(area: ParkingArea) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _actionState.value = ReservationActionState.Error("You must be logged in")
            return
        }

        viewModelScope.launch {
            _actionState.value = ReservationActionState.Loading
            parkingRepository.reserveSpot(userId, area)
                .onSuccess { reservationId ->
                    _actionState.value = ReservationActionState.Success(reservationId)
                }
                .onFailure { error ->
                    _actionState.value = ReservationActionState.Error(
                        error.message ?: "Reservation failed"
                    )
                }
        }
    }

    fun cancelReservation(reservation: Reservation) {
        viewModelScope.launch {
            parkingRepository.cancelReservation(reservation)
                .onFailure { error ->
                    android.util.Log.e("Reservation", "Cancel failed: ${error.message}", error)
                    _myReservations.value = _myReservations.value.copy(
                        errorMessage = error.message
                    )
                }
        }
    }

    fun clearActionState() {
        _actionState.value = ReservationActionState.Idle
    }
}