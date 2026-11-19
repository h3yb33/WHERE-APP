package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RouteBundle
import com.example.model.RoutePlan
import com.example.model.TransitLeg
import com.example.ui.theme.BadgeCheapestBg
import com.example.ui.theme.BadgeFastestBg
import com.example.ui.theme.BadgeFastestText
import com.example.ui.theme.OverchargeAmber
import com.example.ui.theme.OverchargeSoftBg
import com.example.ui.theme.TimelineBlue
import com.example.ui.theme.TimelineBlueSoft
import com.example.ui.theme.TimelinePurple
import com.example.ui.theme.TimelinePurpleSoft
import com.example.ui.theme.TimelineSlate
import com.example.ui.theme.TimelineSlateSoft
import com.example.ui.theme.TimelineTeal
import com.example.ui.theme.TimelineTealSoft
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereMintBorder
import com.example.ui.theme.WhereMintSurface
import com.example.ui.theme.WhereMutedText
import com.example.ui.theme.WhereWhite
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WaysToGetThereSection(
    bundle: RouteBundle,
    selectedIndex: Int,
    onSelectOption: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Ways to get there",
            style = MaterialTheme.typography.titleMedium,
            color = WhereMainText,
            fontWeight = FontWeight.Bold
        )

        bundle.options.forEachIndexed { index, plan ->
            RouteOptionSummaryCard(
                plan = plan,
                isSelected = index == selectedIndex,
                onClick = { onSelectOption(index) },
                modifier = Modifier.testTag("route_option_${plan.optionTag.lowercase().replace(" ", "_")}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteOptionSummaryCard(
    plan: RoutePlan,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeText) = when (plan.optionTag.lowercase()) {
        "fastest" -> BadgeFastestBg to BadgeFastestText
        "direct drop" -> TimelineTealSoft to TimelineTeal
        else -> BadgeCheapestBg to WhereDeepGreen
    }

    val cardBg = if (isSelected) WhereWhite else WhereMintSurface.copy(alpha = 0.45f)
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, WhereDeepGreen)
    } else {
        BorderStroke(1.dp, WhereMintBorder)
    }

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(18.dp),
        border = borderStroke,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Badge ("Cheapest" / "Fastest" / "Direct drop") + Duration ("1 h 33 min")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = plan.optionTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = formatDurationLabel(plan.totalMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = WhereMainText,
                    fontWeight = FontWeight.Bold
                )
            }

            // Middle Row: Mode sequence chips separated by '>'
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                plan.legs.forEachIndexed { idx, leg ->
                    ModeChainChip(leg = leg)
                    if (idx < plan.legs.lastIndex) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = WhereMutedText,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            // Bottom Row: Payments icon + Fare range + Changes / Walk summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = WhereDeepGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatPlanFareRange(plan),
                    style = MaterialTheme.typography.titleSmall,
                    color = WhereMainText,
                    fontWeight = FontWeight.Bold
                )

                val metaText = buildString {
                    if (plan.isDoorToDoor) {
                        append("Door to door")
                    } else if (plan.changesCount > 0) {
                        append(if (plan.changesCount == 1) "1 change" else "${plan.changesCount} changes")
                    }
                    if (plan.walkDistanceKm >= 0.1) {
                        if (isNotEmpty()) append("  ·  ")
                        append("${String.format("%.1f", plan.walkDistanceKm)} km walk")
                    }
                }
                if (metaText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = WhereMutedText
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChainChip(leg: TransitLeg) {
    val (bgColor, fgColor, icon, label) = when (leg.mode.uppercase()) {
        "BUS", "BRT" -> QuadModeStyle(TimelineBlueSoft, TimelineBlue, Icons.Default.DirectionsBus, "Bus")
        "TAXI" -> QuadModeStyle(TimelineBlueSoft, TimelineBlue, Icons.Default.LocalTaxi, "Along")
        "KEKE" -> QuadModeStyle(TimelinePurpleSoft, TimelinePurple, Icons.Default.ElectricRickshaw, "Keke")
        "OKADA", "BIKE" -> QuadModeStyle(OverchargeSoftBg, OverchargeAmber, Icons.Default.TwoWheeler, "Okada")
        "DROP" -> QuadModeStyle(TimelineTealSoft, TimelineTeal, Icons.Default.LocalTaxi, "Drop")
        else -> QuadModeStyle(
            TimelineSlateSoft,
            TimelineSlate,
            Icons.AutoMirrored.Filled.DirectionsWalk,
            "${leg.durationMinutes} min"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fgColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun formatDurationLabel(totalMinutes: Int): String {
    if (totalMinutes < 60) return "$totalMinutes min"
    val hrs = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (mins == 0) "$hrs h" else "$hrs h $mins min"
}

fun formatNaira(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "₦${formatter.format(amount)}"
}

fun formatPlanFareRange(plan: RoutePlan): String {
    if (plan.trekRecommended || (plan.totalFareMinNaira == 0 && plan.totalFareMaxNaira == 0)) {
        return "₦0"
    }
    return if (plan.totalFareMinNaira == plan.totalFareMaxNaira) {
        formatNaira(plan.totalFareMinNaira)
    } else {
        "${formatNaira(plan.totalFareMinNaira)}–${formatNaira(plan.totalFareMaxNaira)}"
    }
}

private data class QuadModeStyle(
    val bg: Color,
    val fg: Color,
    val icon: ImageVector,
    val label: String
)
