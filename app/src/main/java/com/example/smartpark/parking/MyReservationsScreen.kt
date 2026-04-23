package com.example.smartpark.parking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.layout.width
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(
    onBack: () -> Unit,
    viewModel: ReservationViewModel = viewModel()
) {
    val state by viewModel.myReservations.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reservations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.reservations.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🅿️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No reservations yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Tap a parking marker on the map to reserve a spot",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Partition reservations into active and past
                        val active = state.reservations.filter { it.status == "active" }
                        val past = state.reservations.filter { it.status != "active" }

                        // === ACTIVE SECTION ===
                        if (active.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Active",
                                    count = active.size
                                )
                            }
                            items(active) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    onCancel = { viewModel.cancelReservation(reservation) }
                                )
                            }
                        }

                        // === PAST SECTION ===
                        if (past.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionHeader(
                                    title = "History",
                                    count = past.size
                                )
                            }
                            items(past) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    onCancel = { viewModel.cancelReservation(reservation) }
                                )

                            }
                        }
                    }
                        }
                    }
                }
            }
        }
@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Composable
private fun ReservationCard(
    reservation: Reservation,
    onCancel: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val (statusColor, statusText) = when (reservation.status) {
        "active" -> MaterialTheme.colorScheme.primary to "ACTIVE"
        "cancelled" -> Color.Gray to "CANCELLED"
        "completed" -> Color(0xFF4CAF50) to "COMPLETED"
        else -> Color.Gray to reservation.status.uppercase()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.parkingAreaName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            reservation.reservedAt?.toDate()?.let { date ->
                Text(
                    text = "Reserved: ${dateFormat.format(date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            reservation.expiresAt?.toDate()?.let { date ->
                Text(
                    text = "Expires: ${dateFormat.format(date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "€${reservation.pricePerHour}/hr",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (reservation.status == "active") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Reservation")
                }
            }
        }
    }
}