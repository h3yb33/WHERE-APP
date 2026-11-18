package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveTripSession
import com.example.model.RoutePlan
import com.example.model.TransitLeg
import com.example.ui.theme.OverchargeAmber
import com.example.ui.theme.OverchargeSoftBg
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
fun ActiveTripTrackerCard(
    session: ActiveTripSession,
    onToggleStep: (Int) -> Unit,
    onRequestEndTrip: () -> Unit
) {
    val totalSteps = session.plan.legs.size
    val doneSteps = session.completedStepIndices.size

    Surface(
        color = WhereMintSurface,
        border = BorderStroke(1.5.dp, WhereDeepGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_trip_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE ABUJA TRIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${session.plan.originName} -> ${session.plan.destinationName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = WhereMainText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = WhereDeepGreen,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "$doneSteps/$totalSteps steps",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereWhite,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                session.plan.legs.forEachIndexed { index, leg ->
                    val completed = index in session.completedStepIndices
                    Surface(
                        color = WhereWhite.copy(alpha = if (completed) 0.78f else 1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WhereMintBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleStep(index) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (completed) "Step completed" else "Mark step done",
                                tint = WhereDeepGreen
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Step ${leg.stepNumber} · ${leg.mode}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WhereDeepGreen
                                )
                                Text(
                                    text = leg.instruction,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhereMainText
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onRequestEndTrip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhereDeepGreen,
                    contentColor = WhereWhite
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("end_trip_button")
            ) {
                Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("End Trip & Quick Survey", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun RouteResultCard(
    plan: RoutePlan,
    isTripActiveForPlan: Boolean,
    onStartTrip: () -> Unit,
    onSavePlace: () -> Unit,
    onSaveRoute: () -> Unit,
    onEndActiveTrip: () -> Unit,
    onClearResult: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WhereWhite),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = if (plan.trekRecommended) 2.dp else 1.dp,
            color = if (plan.trekRecommended) WhereDeepGreen else WhereSubtleBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (plan.trekRecommended) {
                Surface(
                    color = WhereMintSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WhereMintBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trek_option_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WhereWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "Trek it",
                                tint = WhereDeepGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trek it · ₦0",
                                style = MaterialTheme.typography.titleMedium,
                                color = WhereInfoTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Under 1.5 km (${String.format("%.2f", plan.distanceKm)} km) · ~${plan.totalMinutes} min walk",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhereDeepGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${plan.originName} -> ${plan.destinationName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhereMainText
                    )
                    Text(
                        text = "${String.format("%.1f", plan.distanceKm)} km · ~${plan.totalMinutes} mins · ${plan.legs.size} leg(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = WhereMutedText
                    )
                }
                IconButton(
                    onClick = onClearResult,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close route results",
                        tint = WhereMutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                color = WhereInfoBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, WhereMintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL FAIR FARE (ESTIMATE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhereInfoTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (plan.trekRecommended) "₦0" else "₦${plan.totalFareMinNaira}–₦${plan.totalFareMaxNaira}",
                            style = MaterialTheme.typography.titleLarge,
                            color = WhereDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!plan.trekRecommended && plan.totalOverchargeAboveNaira > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = OverchargeAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "You are being overcharged above ₦${plan.totalOverchargeAboveNaira}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OverchargeAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = plan.fareEstimateDisclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereMutedText
                    )
                }
            }

            if (plan.memoryInsightUsed.isNotBlank()) {
                Surface(
                    color = WhereMintSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = WhereDeepGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI Memory: ${plan.memoryInsightUsed}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereInfoTitle,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = WhereSubtleBorder)

            Text(
                text = "Step-by-step legs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhereMainText
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                plan.legs.forEachIndexed { index, leg ->
                    TransitLegRow(stepNumber = index + 1, leg = leg)
                }
            }

            Surface(
                color = OverchargeSoftBg.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OverchargeAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = plan.antiInflationTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = WhereMainText
                    )
                }
            }

            SearchGroundingBar(
                webSources = plan.webSources,
                searchEntryPointHtml = plan.searchEntryPointHtml
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isTripActiveForPlan) {
                    Button(
                        onClick = onEndActiveTrip,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhereDeepGreen, contentColor = WhereWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("result_end_trip_button")
                    ) {
                        Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Trip & Survey")
                    }
                } else {
                    Button(
                        onClick = onStartTrip,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhereDeepGreen, contentColor = WhereWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_trip_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Trip")
                    }
                }

                OutlinedButton(
                    onClick = onSavePlace,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, WhereMintBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = WhereMintSurface, contentColor = WhereDeepGreen),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.StarOutline, contentDescription = "Save place", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Place")
                }

                OutlinedButton(
                    onClick = onSaveRoute,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, WhereMintBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = WhereMintSurface, contentColor = WhereDeepGreen),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("save_route_button")
                ) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "Save route", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Route")
                }
            }
        }
    }
}

@Composable
private fun TransitLegRow(
    stepNumber: Int,
    leg: TransitLeg
) {
    val icon = when (leg.mode.uppercase()) {
        "WALK", "TREK" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "KEKE" -> Icons.Default.ElectricRickshaw
        "BIKE", "OKADA" -> Icons.Default.TwoWheeler
        "BUS", "BRT" -> Icons.Default.DirectionsBus
        else -> Icons.Default.LocalTaxi
    }

    val fareLabel = if (leg.fareMinNaira == 0 && leg.fareMaxNaira == 0) {
        "₦0 (Trek it)"
    } else {
        "Est. ₦${leg.fareMinNaira}–₦${leg.fareMaxNaira}"
    }

    Surface(
        color = WhereWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WhereSubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(WhereMintSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = leg.mode,
                    tint = WhereDeepGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STEP $stepNumber · ${leg.mode}",
                        style = MaterialTheme.typography.labelLarge,
                        color = WhereDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = WhereMintSurface,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, WhereMintBorder)
                    ) {
                        Text(
                            text = fareLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhereDeepGreen,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                if (leg.boardingPoint.isNotBlank() || leg.alightingPoint.isNotBlank()) {
                    val board = leg.boardingPoint.ifBlank { "Start" }
                    val alight = leg.alightingPoint.ifBlank { "End" }
                    Text(
                        text = "$board  ->  $alight",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WhereMainText
                    )
                }

                Text(
                    text = leg.instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WhereMainText
                )

                if (leg.overchargeAboveNaira > 0) {
                    Surface(
                        color = OverchargeSoftBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Overcharged above ₦${leg.overchargeAboveNaira}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OverchargeAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (leg.areaRuleNote.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = WhereMutedText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = leg.areaRuleNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMutedText
                        )
                    }
                }

                if (leg.roadNamesSummary.isNotBlank()) {
                    Text(
                        text = "Via OpenStreetMap: ${leg.roadNamesSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereDeepGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "${leg.distanceText} · ~${leg.durationMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhereMutedText
                )
            }
        }
    }
}
