package com.example.smartpark.home

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpark.parking.ParkingArea
import com.example.smartpark.parking.ParkingRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MapUiState(
    val userLocation: Location? = null,
    val parkingAreas: List<ParkingArea> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MapViewModel(
    private val parkingRepository: ParkingRepository = ParkingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        observeParkingAreas()
    }

    private fun observeParkingAreas() {
        viewModelScope.launch {
            parkingRepository.observeParkingAreas()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to load parking: ${e.message}"
                    )
                }
                .collect { areas ->
                    _uiState.value = _uiState.value.copy(
                        parkingAreas = areas,
                        errorMessage = null
                    )
                }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchUserLocation(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
                _uiState.value = _uiState.value.copy(
                    userLocation = location,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Could not get location"
                )
            }
        }
    }
}