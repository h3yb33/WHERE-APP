package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.LearnedCorridorEntity
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.SavedRouteEntity
import com.example.data.local.TripSurveyEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.OverchargeAmber
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereInfoBg
import com.example.ui.theme.WhereInfoTitle
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereMintBorder
import com.example.ui.theme.WhereMintSurface
import com.example.ui.theme.WhereMutedText
import com.example.ui.theme.WhereSubtleBorder
import com.example.ui.theme.WhereWhite

@Composable
fun AbujaMemoryScreen(
    savedPlaces: List<SavedPlaceEntity>,
    learnedCorridors: List<LearnedCorridorEntity>,
    tripSurveys: List<TripSurveyEntity>,
    savedRoutes: List<SavedRouteEntity>,
    onLoadRoute: (String, String) -> Unit,
    onSelectSavedPlace: (SavedPlaceEntity) -> Unit,
    onDeleteSavedPlace: (Int) -> Unit,
    onDeleteSavedRoute: (Int) -> Unit,
    onTeachAiTip: (String, String, String, Int) -> Unit,
    onClearMemory: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTeachDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WhereWhite),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Summary & Actions Card
        item {
            Surface(
                color = WhereInfoBg,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, WhereMintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = WhereDeepGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Abuja Memory",
                                style = MaterialTheme.typography.titleLarge,
                                color = WhereInfoTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Remembers your saved Abuja places, frequent routes, and trip surveys.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhereMainText.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemoryStatChip("Places", savedPlaces.size.toString(), Modifier.weight(1f))
                        MemoryStatChip("Corridors", learnedCorridors.size.toString(), Modifier.weight(1f))
                        MemoryStatChip("Surveys", tripSurveys.size.toString(), Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showTeachDialog = true },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhereDeepGreen,
                                contentColor = WhereWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("teach_ai_tip_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Teach AI Tip")
                        }

                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = WhereWhite,
                                contentColor = AlertRed
                            ),
                            modifier = Modifier.testTag("clear_memory_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear")
                        }
                    }
                }
            }
        }

        // 2. Saved Places
        item {
            Text(
                text = "Saved Abuja Places",
                style = MaterialTheme.typography.titleLarge,
                color = WhereMainText
            )
        }

        if (savedPlaces.isEmpty()) {
            item {
                EmptyMemoryNotice("No saved places yet. Tap Place on any Abuja route result to remember it for quick suggestions.")
            }
        } else {
            items(savedPlaces, key = { "place_${it.id}" }) { place ->
                Surface(
                    color = WhereWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WhereSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = WhereDeepGreen
                            )
                            Column {
                                Text(
                                    text = place.placeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhereMainText
                                )
                                Text(
                                    text = "Tap Use to set as destination",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhereMutedText
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onSelectSavedPlace(place) }) {
                                Text("Use", color = WhereDeepGreen, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { onDeleteSavedPlace(place.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete saved place",
                                    tint = WhereMutedText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Frequent & Learned Abuja Corridors
        item {
            Text(
                text = "Frequent & Learned Corridors",
                style = MaterialTheme.typography.titleLarge,
                color = WhereMainText
            )
        }

        if (learnedCorridors.isEmpty()) {
            item {
                EmptyMemoryNotice("Corridor memory is empty. Complete a quick post-trip survey or tap Teach AI Tip.")
            }
        } else {
            items(learnedCorridors, key = { "corr_${it.id}" }) { corridor ->
                LearnedCorridorCard(
                    corridor = corridor,
                    onPlanRoute = { onLoadRoute(corridor.originArea, corridor.destinationArea) }
                )
            }
        }

        // 4. Saved Routes
        item {
            Text(
                text = "Saved Abuja Routes",
                style = MaterialTheme.typography.titleLarge,
                color = WhereMainText
            )
        }

        if (savedRoutes.isEmpty()) {
            item {
                EmptyMemoryNotice("No saved routes yet. Tap Route on any calculated trip card.")
            }
        } else {
            items(savedRoutes, key = { "route_${it.id}" }) { route ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = WhereWhite),
                    border = BorderStroke(1.dp, WhereSubtleBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${route.originName} -> ${route.destinationName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WhereMainText
                                )
                                Text(
                                    text = "${String.format("%.1f", route.distanceKm)} km · ~${route.estimatedMinutes} min · ${route.fareSummary}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WhereDeepGreen
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSavedRoute(route.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete saved route",
                                    tint = WhereMutedText
                                )
                            }
                        }
                        Text(
                            text = route.stepsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMutedText
                        )
                        OutlinedButton(
                            onClick = { onLoadRoute(route.originName, route.destinationName) },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, WhereMintBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = WhereMintSurface,
                                contentColor = WhereDeepGreen
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Route")
                        }
                    }
                }
            }
        }

        // 5. Recent Trip Surveys
        if (tripSurveys.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Trip Surveys",
                    style = MaterialTheme.typography.titleLarge,
                    color = WhereMainText
                )
            }
            items(tripSurveys.take(10), key = { "survey_${it.id}" }) { survey ->
                Surface(
                    color = WhereWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WhereSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${survey.originName} -> ${survey.destinationName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = WhereMainText,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₦${survey.farePaidNaira}",
                                style = MaterialTheme.typography.labelLarge,
                                color = WhereDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Alight & trek: ${survey.alightAndTrekPoint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMainText
                        )
                        Text(
                            text = "Okada: ${survey.commercialBikePoint} · Keke: ${survey.kekeTimeTaken}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMutedText
                        )
                        Text(
                            text = "${survey.areaChecked}: Keke=${survey.kekeAllowedStatus}, Bike=${survey.bikeAllowedStatus}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverchargeAmber
                        )
                    }
                }
            }
        }
    }

    if (showTeachDialog) {
        TeachAiDialog(
            onDismiss = { showTeachDialog = false },
            onSaveTip = { o, d, tip, fare ->
                onTeachAiTip(o, d, tip, fare)
                showTeachDialog = false
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear AI Memory?") },
            text = {
                Text("This removes all saved Abuja places, saved routes, learned corridors, and survey logs from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearMemory(false)
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = WhereWhite)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            onClearMemory(true)
                            showClearDialog = false
                        }
                    ) {
                        Text("Reset to Defaults", color = WhereDeepGreen)
                    }
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyMemoryNotice(text: String) {
    Surface(
        color = WhereMintSurface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WhereMintBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = WhereMutedText,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun MemoryStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WhereWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WhereMintBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhereDeepGreen
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = WhereMutedText
            )
        }
    }
}

@Composable
private fun LearnedCorridorCard(
    corridor: LearnedCorridorEntity,
    onPlanRoute: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WhereWhite),
        border = BorderStroke(1.dp, WhereSubtleBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = corridor.corridorTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhereMainText
                    )
                    Text(
                        text = "${corridor.tripCount} trip(s) recorded",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereDeepGreen
                    )
                }
                Surface(
                    color = WhereMintSurface,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, WhereMintBorder)
                ) {
                    Text(
                        text = "Est. ~₦${corridor.verifiedFareNaira}",
                        style = MaterialTheme.typography.labelMedium,
                        color = WhereDeepGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = corridor.routeSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = WhereMainText
            )

            HorizontalDivider(color = WhereSubtleBorder)

            Text(
                text = "Alight & Trek: ${corridor.trekAlightPoint}",
                style = MaterialTheme.typography.bodySmall,
                color = WhereMutedText
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = OverchargeAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Keke: ${corridor.kekeCommercialAllowed} | Bike: ${corridor.bikeCommercialAllowed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OverchargeAmber,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = onPlanRoute,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, WhereMintBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = WhereMintSurface,
                    contentColor = WhereDeepGreen
                )
            ) {
                Text("Plan This Abuja Route")
            }
        }
    }
}

@Composable
private fun TeachAiDialog(
    onDismiss: () -> Unit,
    onSaveTip: (String, String, String, Int) -> Unit
) {
    var origin by remember { mutableStateOf("") }
    var dest by remember { mutableStateOf("") }
    var tip by remember { mutableStateOf("") }
    var fare by remember { mutableStateOf("400") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teach AI an Abuja Route") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("From (Abuja Area)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dest,
                    onValueChange = { dest = it },
                    label = { Text("To (Abuja Area)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tip,
                    onValueChange = { tip = it },
                    label = { Text("Step-by-step movements & restrictions") },
                    placeholder = { Text("e.g. Board Along at Berger, alight at 1st Ave, Keke inside estate") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fare,
                    onValueChange = { raw -> fare = raw.filter { it.isDigit() } },
                    label = { Text("Estimated Fare (₦)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveTip(origin, dest, tip, fare.toIntOrNull() ?: 0) },
                enabled = origin.isNotBlank() && dest.isNotBlank() && tip.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WhereDeepGreen, contentColor = WhereWhite)
            ) {
                Text("Train AI Memory")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
