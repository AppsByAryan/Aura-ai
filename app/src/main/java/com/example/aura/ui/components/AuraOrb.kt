package com.example.aura.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.data.AuraState
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraElectricViolet
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraWarning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuraOrb(
    state: AuraState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_orb_anim")

    // Slow breath for idle, rapid for thinking/executing
    val breathDuration = when (state) {
        AuraState.IDLE -> 3000
        AuraState.LISTENING -> 1200
        AuraState.THINKING -> 800
        AuraState.WAITING -> 1500
        AuraState.EXECUTING -> 600
        AuraState.SUCCESS -> 1000
        AuraState.ERROR -> 700
    }

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(breathDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraState.EXECUTING -> 2000
                    AuraState.THINKING -> 3500
                    AuraState.LISTENING -> 5000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Primary and accent colors based on state
    val targetPrimaryColor = when (state) {
        AuraState.IDLE -> AuraCyan
        AuraState.LISTENING -> Color(0xFF00F5FF)
        AuraState.THINKING -> AuraElectricViolet
        AuraState.WAITING -> AuraWarning
        AuraState.EXECUTING -> Color(0xFF2979FF)
        AuraState.SUCCESS -> AuraSuccess
        AuraState.ERROR -> AuraError
    }

    val targetSecondaryColor = when (state) {
        AuraState.IDLE -> Color(0xFF005B66)
        AuraState.LISTENING -> AuraCyan
        AuraState.THINKING -> Color(0xFF00E5FF)
        AuraState.WAITING -> Color(0xFFFF6D00)
        AuraState.EXECUTING -> AuraElectricViolet
        AuraState.SUCCESS -> Color(0xFF00B0FF)
        AuraState.ERROR -> Color(0xFFFF1744)
    }

    val primaryColor by animateColorAsState(targetValue = targetPrimaryColor, label = "orb_primary")
    val secondaryColor by animateColorAsState(targetValue = targetSecondaryColor, label = "orb_secondary")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .testTag("aura_orb")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.32f

                // Outer diffuse atmospheric glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f * breathScale),
                            primaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.8f * breathScale
                    ),
                    radius = baseRadius * 1.8f * breathScale,
                    center = center
                )

                // Expanding audio listening wave if in LISTENING state
                if (state == AuraState.LISTENING) {
                    drawCircle(
                        color = primaryColor.copy(alpha = (1f - wavePulse) * 0.6f),
                        radius = baseRadius * (1.1f + wavePulse * 0.6f),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Outer Orbit Ring with Notches
                rotate(degrees = rotationAngle, pivot = center) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.5f),
                        radius = baseRadius * 1.25f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 4 Telemetry Nodes on outer ring
                    val nodeRadius = baseRadius * 1.25f
                    for (i in 0 until 4) {
                        val angle = (i * 90.0) * (PI / 180.0)
                        val nx = center.x + nodeRadius * cos(angle).toFloat()
                        val ny = center.y + nodeRadius * sin(angle).toFloat()
                        drawCircle(
                            color = primaryColor,
                            radius = 3.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                    }
                }

                // Counter-Rotating Inner Tech Arcs
                rotate(degrees = -rotationAngle * 1.5f, pivot = center) {
                    drawArc(
                        color = secondaryColor.copy(alpha = 0.8f),
                        startAngle = 45f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius * 0.95f, center.y - baseRadius * 0.95f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 1.9f, baseRadius * 1.9f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = secondaryColor.copy(alpha = 0.8f),
                        startAngle = 225f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius * 0.95f, center.y - baseRadius * 0.95f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 1.9f, baseRadius * 1.9f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Solid Core Shield with Radial Gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            primaryColor,
                            secondaryColor
                        ),
                        center = center,
                        radius = baseRadius * 0.75f * breathScale
                    ),
                    radius = baseRadius * 0.75f * breathScale,
                    center = center
                )

                // Hot White Singularity Center
                drawCircle(
                    color = Color.White,
                    radius = baseRadius * 0.28f * breathScale,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // State Badge
        Surface(
            shape = CircleShape,
            color = Color(0x33000000),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = primaryColor
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.name,
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
