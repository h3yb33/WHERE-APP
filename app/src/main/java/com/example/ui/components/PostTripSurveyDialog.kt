package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PostTripSurveyInput
import com.example.model.RoutePlan
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereMutedText
import com.example.ui.theme.WhereSubtleBorder
import com.example.ui.theme.WhereWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostTripSurveyDialog(
    plan: RoutePlan,
    onDismissWithoutEnding: () -> Unit,
    onSubmitAndEndTrip: (PostTripSurveyInput) -> Unit
) {
    var alightAndTrekPoint by remember {
        mutableStateOf(
            plan.legs.firstOrNull { it.mode == "TREK" }?.boardingPoint
                ?: plan.legs.lastOrNull()?.alightingPoint
                ?: ""
        )
    }
    var commercialBikePoint by remember {
        mutableStateOf(
            plan.legs.firstOrNull { it.mode == "OKADA" }?.boardingPoint
                ?: "Did not use commercial bike"
        )
    }
    var kekeTimeTaken by remember {
        mutableStateOf(
            if (plan.legs.any { it.mode == "KEKE" }) "Morning (6–11am)" else "Did not take Keke"
        )
    }
    var areaChecked by remember { mutableStateOf(plan.destinationName) }
    var kekeAllowedStatus by remember { mutableStateOf("Inner streets only") }
    var bikeAllowedStatus by remember { mutableStateOf("Banned in this area") }
    var farePaidText by remember {
        mutableStateOf(
            if (plan.totalFareMaxNaira > 0) plan.totalFareMaxNaira.toString() else "0"
        )
    }

    val kekeTimeOptions = listOf(
        "Morning (6–11am)",
        "Midday (11am–4pm)",
        "Evening Rush (4–8pm)",
        "Night (8pm+)",
        "Did not take Keke"
    )
    val kekeRuleOptions = listOf("Allowed", "Inner streets only", "Banned")
    val bikeRuleOptions = listOf("Allowed", "Outer roads only", "Banned")

    Dialog(
        onDismissRequest = onDismissWithoutEnding,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = WhereWhite,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = WhereDeepGreen
                    )
                    Column {
                        Text(
                            text = "Quick Post-Trip Survey",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WhereMainText
                        )
                        Text(
                            text = "Helps train WHERE AI Memory for Abuja commuters",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMutedText
                        )
                    }
                }

                HorizontalDivider(color = WhereSubtleBorder)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "1. At what point did you alight and start trekking?",
                        style = MaterialTheme.typography.labelLarge,
                        color = WhereMainText
                    )
                    OutlinedTextField(
                        value = alightAndTrekPoint,
                        onValueChange = { alightAndTrekPoint = it },
                        placeholder = { Text("e.g. Eagle Square junction") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("survey_alight_trek_input")
                    )
                    FilterChip(
                        selected = alightAndTrekPoint.equals("No trekking / Straight drop", ignoreCase = true),
                        onClick = { alightAndTrekPoint = "No trekking / Straight drop" },
                        label = { Text("No trekking / Straight drop") }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. At what point did you use a commercial bike (Okada)?",
                        style = MaterialTheme.typography.labelLarge,
                        color = WhereMainText
                    )
                    OutlinedTextField(
                        value = commercialBikePoint,
                        onValueChange = { commercialBikePoint = it },
                        placeholder = { Text("e.g. Inside Lugbe estate to FHA Gate") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("survey_bike_point_input")
                    )
                    FilterChip(
                        selected = commercialBikePoint.equals("Did not use commercial bike", ignoreCase = true),
                        onClick = { commercialBikePoint = "Did not use commercial bike" },
                        label = { Text("Did not use commercial bike") }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "3. At what time did you take Keke (tricycle)?",
                        style = MaterialTheme.typography.labelLarge,
                        color = WhereMainText
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        kekeTimeOptions.forEach { option ->
                            FilterChip(
                                selected = kekeTimeTaken == option,
                                onClick = { kekeTimeTaken = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "4. Is commercial bike or Keke allowed in that Abuja area?",
                        style = MaterialTheme.typography.labelLarge,
                        color = WhereMainText
                    )
                    OutlinedTextField(
                        value = areaChecked,
                        onValueChange = { areaChecked = it },
                        label = { Text("Abuja Area / District") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Keke rule:",
                        style = MaterialTheme.typography.labelMedium,
                        color = WhereMutedText
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        kekeRuleOptions.forEach { opt ->
                            FilterChip(
                                selected = kekeAllowedStatus == opt,
                                onClick = { kekeAllowedStatus = opt },
                                label = { Text(opt) }
                            )
                        }
                    }
                    Text(
                        text = "Commercial Bike (Okada) rule:",
                        style = MaterialTheme.typography.labelMedium,
                        color = WhereMutedText
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bikeRuleOptions.forEach { opt ->
                            FilterChip(
                                selected = bikeAllowedStatus == opt,
                                onClick = { bikeAllowedStatus = opt },
                                label = { Text(opt) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "5. What price did you pay for the fare (₦)?",
                        style = MaterialTheme.typography.labelLarge,
                        color = WhereMainText
                    )
                    OutlinedTextField(
                        value = farePaidText,
                        onValueChange = { raw -> farePaidText = raw.filter { it.isDigit() } },
                        prefix = { Text("₦ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("survey_fare_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissWithoutEnding) {
                        Text("Back to Trip")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSubmitAndEndTrip(
                                PostTripSurveyInput(
                                    originName = plan.originName,
                                    destinationName = plan.destinationName,
                                    alightAndTrekPoint = alightAndTrekPoint.ifBlank { "Direct drop" },
                                    commercialBikePoint = commercialBikePoint.ifBlank { "Did not use commercial bike" },
                                    kekeTimeTaken = kekeTimeTaken,
                                    kekeAllowedStatus = kekeAllowedStatus,
                                    bikeAllowedStatus = bikeAllowedStatus,
                                    areaChecked = areaChecked.ifBlank { plan.destinationName },
                                    farePaidNaira = farePaidText.toIntOrNull() ?: 0
                                )
                            )
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhereDeepGreen, contentColor = WhereWhite),
                        modifier = Modifier.testTag("submit_survey_finish_trip_button")
                    ) {
                        Text("Submit & End Trip")
                    }
                }
            }
        }
    }
}
