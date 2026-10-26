package com.cmf.tvremote

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmf.tvremote.domain.model.RemoteCommand

/**
 * Continuous Curvature Squircle Shape (Apple Superellipse Approximation)
 */
class SquircleShape(val cornerRadius: Dp, private val smoothness: Float = 0.65f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { cornerRadius.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        val c = r * smoothness
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            cubicTo(w - c, 0f, w, c, w, r)
            lineTo(w, h - r)
            cubicTo(w, h - c, w - c, h, w - r, h)
            lineTo(r, h)
            cubicTo(c, h, 0f, h - c, 0f, h - r)
            lineTo(0f, r)
            cubicTo(0f, c, c, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * High-fidelity iOS Glassmorphism Modifier
 */
fun Modifier.iosGlassmorphicSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    tintAlpha: Float = 0.12f,
    borderAlpha: Float = 0.20f,
    elevation: Dp = 16.dp,
    blurRadius: Dp = 25.dp
): Modifier = composed {
    val base = this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = Color.Black.copy(alpha = 0.45f),
            ambientColor = Color.Black.copy(alpha = 0.25f)
        )
        .clip(shape)

    val blurred = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        base.blur(blurRadius)
    } else {
        base
    }

    blurred
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = tintAlpha + 0.05f),
                    Color.White.copy(alpha = tintAlpha)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha + 0.15f),
                    Color.White.copy(alpha = borderAlpha * 0.4f)
                )
            ),
            shape = shape
        )
}

/**
 * Squircle D-Pad Dial (Matches attached design)
 */
@Composable
fun SquircleDPad(
    onCommand: (RemoteCommand) -> Unit,
    radius: Dp,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val outerShape = SquircleShape(cornerRadius = radius + 6.dp)
    val innerShape = SquircleShape(cornerRadius = (radius * 0.65f))

    Box(
        modifier = modifier
            .size(236.dp)
            .iosGlassmorphicSurface(
                shape = outerShape,
                tintAlpha = 0.09f,
                borderAlpha = 0.25f,
                elevation = 20.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        // UP
        DirectionalChevron(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.34f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.DPAD_UP)
            }
        )

        // DOWN
        DirectionalChevron(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.34f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.DPAD_DOWN)
            }
        )

        // LEFT
        DirectionalChevron(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Left",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.34f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.DPAD_LEFT)
            }
        )

        // RIGHT
        DirectionalChevron(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Right",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.34f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.DPAD_RIGHT)
            }
        )

        // Center Recessed OK Button
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1.0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "ok_press"
        )

        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .iosGlassmorphicSurface(
                    shape = innerShape,
                    tintAlpha = 0.22f,
                    borderAlpha = 0.35f,
                    elevation = 8.dp
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            tryAwaitRelease()
                            isPressed = false
                            onCommand(RemoteCommand.SELECT)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DirectionalChevron(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(34.dp)
        )
    }
}

/**
 * Vertical Tactile Capsule Rocker with Indicator Pill (Matches attached design)
 */
@Composable
fun GlassCapsuleRocker(
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val capsuleShape = RoundedCornerShape(32.dp)

    Column(
        modifier = modifier
            .width(66.dp)
            .height(138.dp)
            .iosGlassmorphicSurface(
                shape = capsuleShape,
                tintAlpha = 0.14f,
                borderAlpha = 0.20f,
                elevation = 12.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Plus action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlus()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light
            )
        }

        // Center Pill Thumb
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(14.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.75f))
        )

        // Minus action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMinus()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "—",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
