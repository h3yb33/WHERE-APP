package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActiveTripSession
import com.example.model.RoutePlan
import com.example.model.TransitLeg
import com.example.ui.theme.OverchargeAmber
import com.example.ui.theme.OverchargeSoftBg
import com.example.ui.theme.RushHourBannerBg
import com.example.ui.theme.RushHourBannerBorder
import com.example.ui.theme.RushHourBannerText
import com.example.ui.theme.TimelineArriveGreen
import com.example.ui.theme.TimelineBlue
import com.example.ui.theme.TimelinePurple
import com.example.ui.theme.TimelineSlate
import com.example.ui.theme.TimelineTeal
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
                                    text = leg.headline.ifBlank { "Step ${leg.stepNumber} · ${leg.mode}" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WhereDeepGreen
                                )
                                Text(
                                    text = leg.subHeadline.ifBlank { leg.instruction },
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
        border = BorderStroke(1.dp, WhereSubtleBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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

            // "Step by step" Header + Total duration & price summary + Close icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step by step",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WhereMainText
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatDurationLabel(plan.totalMinutes)} · ${formatPlanFareRange(plan)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WhereMutedText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onClearResult,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close route results",
                            tint = WhereMutedText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Warm Amber Rush Hour / Fare Agreement Notice Box
            if (plan.rushHourNotice.isNotBlank()) {
                Surface(
                    color = RushHourBannerBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RushHourBannerBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = OverchargeAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = plan.rushHourNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = RushHourBannerText,
                            fontWeight = FontWeight.Medium
                        )
                    }
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

            // Connected Step-by-Step Vertical Timeline
            Column(modifier = Modifier.fillMaxWidth()) {
                plan.legs.forEach { leg ->
                    TimelineStepRow(leg = leg)
                }
                TimelineArrivalRow(destinationName = plan.destinationName)
            }

            HorizontalDivider(color = WhereSubtleBorder)

            Text(
                text = plan.fareEstimateDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = WhereMutedText
            )

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineStepRow(leg: TransitLeg) {
    val isWalk = leg.mode.equals("TREK", ignoreCase = true) || leg.mode.equals("WALK", ignoreCase = true)
    val (nodeColor, icon) = when (leg.mode.uppercase()) {
        "BUS", "BRT" -> TimelineBlue to Icons.Default.DirectionsBus
        "TAXI" -> TimelineBlue to Icons.Default.LocalTaxi
        "KEKE" -> TimelinePurple to Icons.Default.ElectricRickshaw
        "OKADA", "BIKE" -> OverchargeAmber to Icons.Default.TwoWheeler
        "DROP" -> TimelineTeal to Icons.Default.LocalTaxi
        else -> TimelineSlate to Icons.AutoMirrored.Filled.DirectionsWalk
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Left Timeline Column: Node Circle + Vertical Line
        Column(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(nodeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = leg.mode,
                    tint = WhereWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            Canvas(
                modifier = Modifier
                    .width(3.dp)
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                drawLine(
                    color = nodeColor.copy(alpha = if (isWalk) 0.55f else 0.85f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 2.5.dp.toPx(),
                    pathEffect = if (isWalk) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Step Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = leg.headline.ifBlank { "Step ${leg.stepNumber} · ${leg.mode}" },
                style = MaterialTheme.typography.titleSmall,
                color = WhereMainText,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = leg.subHeadline.ifBlank { leg.instruction },
                style = MaterialTheme.typography.bodySmall,
                color = WhereMutedText
            )

            if (!isWalk) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stopPart = if (leg.stopCount > 0) "${leg.stopCount} stops  ·  " else ""
                    Text(
                        text = "$stopPart${leg.distanceText}  ·",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereMutedText
                    )
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = WhereMutedText,
                        modifier = Modifier.size(13.dp)
                    )
                    val waitPart = if (leg.waitMinutes > 0) ", about ${leg.waitMinutes} min wait" else ""
                    Text(
                        text = "${leg.durationMinutes} min ride$waitPart",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereMutedText
                    )
                }

                // Mint FAIR FARE / FIXED FARE Card
                Surface(
                    color = WhereInfoBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WhereMintBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = leg.fareLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = WhereMutedText,
                            fontWeight = FontWeight.SemiBold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val fareText = if (leg.isFixedFare || leg.fareMinNaira == leg.fareMaxNaira) {
                                formatNaira(leg.fareMinNaira)
                            } else {
                                "${formatNaira(leg.fareMinNaira)}–${formatNaira(leg.fareMaxNaira)}"
                            }
                            Text(
                                text = fareText,
                                style = MaterialTheme.typography.titleMedium,
                                color = WhereMainText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 10.dp)
                            )

                            if (!leg.isFixedFare && leg.overchargeAboveNaira > 0) {
                                Surface(
                                    color = OverchargeSoftBg,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = OverchargeAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Above ${formatNaira(leg.overchargeAboveNaira)} is too much",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RushHourBannerText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (leg.fareHelperNote.isNotBlank()) {
                            Text(
                                text = leg.fareHelperNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = WhereDeepGreen
                            )
                        }
                        if (leg.roadNamesSummary.isNotBlank()) {
                            Text(
                                text = "Via OpenStreetMap: ${leg.roadNamesSummary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhereMutedText
                            )
                        }
                    }
                }
            }

            if (leg.changeAtNote.isNotBlank()) {
                Text(
                    text = leg.changeAtNote,
                    style = MaterialTheme.typography.labelMedium,
                    color = WhereDeepGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineArrivalRow(destinationName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(TimelineArriveGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Arrive",
                tint = WhereWhite,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Arrive at $destinationName",
            style = MaterialTheme.typography.titleSmall,
            color = WhereMainText,
            fontWeight = FontWeight.Bold
        )
    }
}
