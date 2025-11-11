package com.secureops.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.secureops.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated gradient background with subtle color shifts
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientDarkStart.copy(alpha = 0.9f + offset * 0.1f),
                        BackgroundDark,
                        GradientDarkEnd.copy(alpha = 0.9f + offset * 0.1f),
                        Color(0xFF2A1E4E).copy(alpha = offset * 0.3f)
                    )
                )
            ),
        content = content
    )
}

/**
 * Pulsing glow effect for status indicators
 */
@Composable
fun PulsingGlowBadge(
    status: String,
    modifier: Modifier = Modifier,
    color: Color = SuccessGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = modifier) {
        // Glow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(scale)
                .alpha(alpha * 0.3f)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .blur(8.dp)
        )

        // Main badge
        StatusBadge(
            status = status,
            color = color,
            modifier = Modifier.scale(scale)
        )
    }
}

/**
 * Animated particle system with floating purple particles
 */
@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    color: Color = NeonPurple.copy(alpha = 0.3f)
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.5f + 0.2f,
                size = Random.nextFloat() * 4f + 2f,
                angle = Random.nextFloat() * 360f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val currentY = (particle.y + (time * particle.speed * 0.0001f)) % 1f
            val currentX = particle.x + sin(currentY * 10f + particle.angle) * 0.05f

            drawCircle(
                color = color,
                radius = particle.size,
                center = Offset(
                    x = currentX * size.width,
                    y = currentY * size.height
                )
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val angle: Float
)

/**
 * Smooth fade-in animation for screens
 */
@Composable
fun FadeInContent(
    modifier: Modifier = Modifier,
    durationMillis: Int = 600,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
        ) + expandVertically(
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Animated loading indicator with pulsing rings
 */
@Composable
fun AnimatedLoadingRings(
    modifier: Modifier = Modifier,
    color: Color = PrimaryPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )

    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing, delayMillis = 300),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ring 1
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(ring1Scale)
                .alpha(1f - (ring1Scale - 0.6f) / 0.6f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
        )
        // Ring 2
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(ring2Scale)
                .alpha(1f - (ring2Scale - 0.6f) / 0.6f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
        )
        // Ring 3
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(ring3Scale)
                .alpha(1f - (ring3Scale - 0.6f) / 0.6f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
        )
        // Center
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Shimmer effect for loading states
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val offset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        GlassSurfaceDark,
                        PrimaryPurple.copy(alpha = 0.2f),
                        GlassSurfaceDark
                    ),
                    startX = offset * 1000f,
                    endX = offset * 1000f + 500f
                )
            )
    )
}

/**
 * Animated card entrance
 */
@Composable
fun AnimatedCardEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 4 }
        ) + scaleIn(
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            initialScale = 0.9f
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Hover scale effect for interactive elements
 */
@Composable
fun HoverScaleEffect(
    pressed: Boolean,
    content: @Composable (Modifier) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "hover_scale"
    )

    content(Modifier.scale(scale))
}

/**
 * Rotating gradient border effect
 */
@Composable
fun RotatingGradientBorder(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val gradient = Brush.sweepGradient(
                colors = listOf(
                    PrimaryPurple,
                    AccentPink,
                    AccentCyan,
                    PrimaryPurple
                ),
                center = center
            )
            // Draw rotating gradient border effect
            drawCircle(
                brush = gradient,
                radius = size.minDimension / 2f,
                alpha = 0.5f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(20.dp)
                ),
            content = content
        )
    }
}

/**
 * Breathe animation for voice assistant
 */
@Composable
fun BreathingAnimation(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    color: Color = PrimaryPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_alpha"
    )

    if (isActive) {
        Box(
            modifier = modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .alpha(alpha * 0.3f)
                    .clip(CircleShape)
                    .background(color)
                    .blur(20.dp)
            )

            // Middle ring
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .scale(scale * 0.9f)
                    .alpha(alpha * 0.5f)
                    .clip(CircleShape)
                    .background(color)
                    .blur(10.dp)
            )

            // Inner circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
