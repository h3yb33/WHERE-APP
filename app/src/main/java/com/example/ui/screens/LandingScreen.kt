package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LandingBgDark
import com.example.ui.theme.LandingBgTopRight
import com.example.ui.theme.LandingBodyText
import com.example.ui.theme.LandingGlassBorder
import com.example.ui.theme.LandingGlassCard
import com.example.ui.theme.LandingLimePrimary
import com.example.ui.theme.LandingLimeText
import com.example.ui.theme.LandingMintAccent
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereWhite

@Composable
fun LandingScreen(
    onFindMyRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(LandingBgDark, LandingBgDark, LandingBgTopRight),
                    start = Offset(0f, Float.POSITIVE_INFINITY),
                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                )
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val isWide = maxWidth >= 760.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Top Bar: Logo + Abuja beta badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(WhereDeepGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "WHERE Logo",
                                tint = LandingLimePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "WHERE",
                            color = WhereWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }

                    Surface(
                        color = LandingGlassCard,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, LandingGlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(LandingLimePrimary)
                            )
                            Text(
                                text = "Abuja beta",
                                style = MaterialTheme.typography.labelMedium,
                                color = WhereWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LandingLeftHeroContent(
                            onFindMyRoute = onFindMyRoute,
                            modifier = Modifier.weight(1.15f)
                        )
                        DecorativeAbujaMapCard(
                            modifier = Modifier
                                .weight(0.95f)
                                .height(420.dp)
                        )
                    }
                } else {
                    LandingLeftHeroContent(
                        onFindMyRoute = onFindMyRoute,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DecorativeAbujaMapCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandingLeftHeroContent(
    onFindMyRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Know your route.",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = WhereWhite
            )
            Text(
                text = "Pay the fair price.",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = LandingMintAccent
            )
        }

        Text(
            text = "Built for newcomers and everyday commuters in Abuja: clear transit steps and honest fare ranges.",
            style = MaterialTheme.typography.bodyLarge,
            color = LandingBodyText,
            modifier = Modifier.widthIn(max = 520.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onFindMyRoute,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LandingLimePrimary,
                    contentColor = LandingLimeText
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("landing_find_route_btn")
            ) {
                Text(
                    text = "Find my route →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LandingLimeText
                )
            }

            Text(
                text = "Free prototype · No sign-up needed",
                style = MaterialTheme.typography.bodySmall,
                color = LandingBodyText.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Three Dark Glass Feature Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LandingGlassFeatureCard(
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    title = "Trek under 1.5 km",
                    description = "Short hop? Walk it and keep your naira.",
                    modifier = Modifier.weight(1f)
                )
                LandingGlassFeatureCard(
                    icon = Icons.Outlined.Payments,
                    title = "Fair fare per leg",
                    description = "Know the real price before you board.",
                    modifier = Modifier.weight(1f)
                )
            }
            LandingGlassFeatureCard(
                icon = Icons.Outlined.AutoAwesome,
                title = "AI Guide",
                description = "Ask anything about getting around Abuja.",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LandingGlassFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LandingGlassCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LandingGlassBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LandingMintAccent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhereWhite
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LandingBodyText
            )
        }
    }
}

@Composable
private fun DecorativeAbujaMapCard(modifier: Modifier = Modifier) {
    Surface(
        color = LandingGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, LandingGlassBorder),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = Color(0xFF154E35)
                val majorRoadColor = Color(0xFF1E6B49)
                val stepX = size.width / 6f
                val stepY = size.height / 5f

                for (i in 1..5) {
                    drawLine(
                        color = gridColor,
                        start = Offset(stepX * i, 0f),
                        end = Offset(stepX * i, size.height),
                        strokeWidth = 2f
                    )
                }
                for (j in 1..4) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, stepY * j),
                        end = Offset(size.width, stepY * j),
                        strokeWidth = 2f
                    )
                }

                // Subtle Abuja arterial curves
                drawLine(
                    color = majorRoadColor,
                    start = Offset(0f, size.height * 0.72f),
                    end = Offset(size.width, size.height * 0.25f),
                    strokeWidth = 6f
                )

                // Dashed Route Curve
                val startPt = Offset(size.width * 0.18f, size.height * 0.72f)
                val midPt = Offset(size.width * 0.48f, size.height * 0.48f)
                val endPt = Offset(size.width * 0.80f, size.height * 0.26f)

                val routePath = Path().apply {
                    moveTo(startPt.x, startPt.y)
                    quadraticTo(midPt.x, size.height * 0.65f, midPt.x, midPt.y)
                    quadraticTo(midPt.x, size.height * 0.28f, endPt.x, endPt.y)
                }
                drawPath(
                    path = routePath,
                    color = LandingMintAccent,
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f), 0f)
                    )
                )

                // Green start dot
                drawCircle(color = WhereWhite, radius = 16f, center = startPt)
                drawCircle(color = LandingLimePrimary, radius = 10f, center = startPt)

                // Destination pin circle
                drawCircle(color = WhereWhite, radius = 18f, center = endPt)
                drawCircle(color = WhereDeepGreen, radius = 12f, center = endPt)
            }

            // Floating white route chips
            Surface(
                color = WhereWhite,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
            ) {
                Text(
                    text = "Trek it · 1.2 km · ₦0",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = WhereMainText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Surface(
                color = WhereWhite,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
            ) {
                Text(
                    text = "Along · ₦300–₦500",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = WhereDeepGreen,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
