package com.cmf.tvremote.presentation.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BacklightSpringSlider(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val scope = rememberCoroutineScope()
    
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val handleOffsetX = remember { Animatable(0f) }
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    val deadZone = 18f
    val maxTravel = (trackWidthPx / 2f) - 32f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF22252B))
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(trackWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        repeatJob?.cancel()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newTarget = (handleOffsetX.value + dragAmount).coerceIn(-maxTravel, maxTravel)
                            handleOffsetX.snapTo(newTarget)

                            // Haptic Boundary Thud
                            if (abs(newTarget) >= maxTravel - 2f) {
                                triggerBoundaryHaptic(vibrator)
                            }

                            // Dynamic Rate-Scaled Command Repeater
                            val offset = handleOffsetX.value
                            if (abs(offset) > deadZone) {
                                val directionPositive = offset > 0
                                val ratio = ((abs(offset) - deadZone) / (maxTravel - deadZone)).coerceIn(0.1f, 1f)
                                val intervalMs = (400 - (280 * ratio)).toLong() // 400ms down to 120ms tick rate

                                if (repeatJob?.isActive != true) {
                                    repeatJob = scope.launch {
                                        while (isActive) {
                                            if (directionPositive) onIncrement() else onDecrement()
                                            triggerTickHaptic(vibrator)
                                            delay(intervalMs)
                                        }
                                    }
                                }
                            } else {
                                repeatJob?.cancel()
                            }
                        }
                    },
                    onDragEnd = {
                        repeatJob?.cancel()
                        scope.launch {
                            handleOffsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        repeatJob?.cancel()
                        scope.launch {
                            handleOffsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Minus Anchor
        Text(
            text = "—",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        )

        // Center Baseline Marker
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )

        // Plus Anchor
        Text(
            text = "+",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        )

        // Spring-loaded thumb handle
        Box(
            modifier = Modifier
                .offset { IntOffset(handleOffsetX.value.roundToInt(), 0) }
                .size(width = 54.dp, height = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFE2E4EB))
        )
    }
}

private fun triggerTickHaptic(vibrator: Vibrator?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(10)
    }
}

private fun triggerBoundaryHaptic(vibrator: Vibrator?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(35)
    }
}
