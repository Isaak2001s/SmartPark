package com.example.smartpark.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartpark.auth.AuthViewModel
import com.example.smartpark.parking.ParkingArea
import com.example.smartpark.parking.ReservationActionState
import com.example.smartpark.parking.ReservationViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private val ATHENS_DEFAULT = LatLng(37.9755, 23.7348)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLoggedOut: () -> Unit,
    onNavigateToReservations: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel(),
    reservationViewModel: ReservationViewModel = viewModel()
) {
    val context = LocalContext.current
    val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val actionState by reservationViewModel.actionState.collectAsStateWithLifecycle()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ATHENS_DEFAULT, 14f)
    }

    // Currently selected parking area (opens bottom sheet when non-null)
    var selectedArea by remember { mutableStateOf<ParkingArea?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) mapViewModel.fetchUserLocation(context)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) mapViewModel.fetchUserLocation(context)
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(mapState.userLocation) {
        mapState.userLocation?.let { loc ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(loc.latitude, loc.longitude), 16f
            )
        }
    }

    // React to reservation action state
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ReservationActionState.Success -> {
                selectedArea = null // close bottom sheet
                snackbarHostState.showSnackbar("Spot reserved successfully! 🎉")
                reservationViewModel.clearActionState()
            }
            is ReservationActionState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                reservationViewModel.clearActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SmartPark") },
                actions = {
                    IconButton(onClick = onNavigateToReservations) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "My Reservations")
                    }
                    TextButton(onClick = {
                        authViewModel.logout()
                        onLoggedOut()
                    }) {
                        Text("Logout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = mapState.userLocation != null
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = true
                )
            ) {
                mapState.parkingAreas.forEach { area ->
                    area.location?.let { geoPoint ->
                        val hue = when (area.occupancyLevel) {
                            "low" -> BitmapDescriptorFactory.HUE_GREEN
                            "medium" -> BitmapDescriptorFactory.HUE_YELLOW
                            "high" -> BitmapDescriptorFactory.HUE_RED
                            else -> BitmapDescriptorFactory.HUE_AZURE
                        }
                        Marker(
                            state = MarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                            title = area.name,
                            snippet = "${area.availableSpots}/${area.totalSpots} spots • €${area.pricePerHour}/hr",
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                selectedArea = area
                                false // returning false lets Google show its default info window too
                            }
                        )
                    }
                }
            }

            if (mapState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // Bottom sheet for selected parking area
    selectedArea?.let { area ->
        ModalBottomSheet(
            onDismissRequest = { selectedArea = null },
            sheetState = sheetState
        ) {
            ParkingAreaDetails(
                area = area,
                isReserving = actionState is ReservationActionState.Loading,
                onReserveClick = { reservationViewModel.reserveSpot(area) }
            )
        }
    }
}

@Composable
private fun ParkingAreaDetails(
    area: ParkingArea,
    isReserving: Boolean,
    onReserveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = area.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = area.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DetailItem(label = "Available", value = "${area.availableSpots}/${area.totalSpots}")
            DetailItem(label = "Price", value = "€${area.pricePerHour}/hr")
            DetailItem(
                label = "Occupancy",
                value = area.occupancyLevel.replaceFirstChar { it.uppercase() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onReserveClick,
            enabled = area.hasAvailability && !isReserving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isReserving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    if (area.hasAvailability) "Reserve Spot (15 min hold)"
                    else "No Spots Available"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}