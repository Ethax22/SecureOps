package com.secureops.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureops.app.ui.components.FloatingParticles
import com.secureops.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Animated splash screen with purple gradient
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var animationPhase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Phase 1: Logo fade in
        delay(300)
        animationPhase = 1

        // Phase 2: Text fade in
        delay(500)
        animationPhase = 2

        // Phase 3: Complete
        delay(1200)
        onSplashComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_gradient")

    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A1E4E).copy(alpha = 0.8f + gradientOffset * 0.2f),
                        GradientDarkStart.copy(alpha = 0.9f + gradientOffset * 0.1f),
                        BackgroundDark,
                        GradientDarkEnd.copy(alpha = 0.9f + gradientOffset * 0.1f),
                        Color(0xFF1A0E3E).copy(alpha = 0.8f + gradientOffset * 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating particles
        FloatingParticles(
            particleCount = 30,
            color = NeonPurple.copy(alpha = 0.4f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with pulsing animation
            AnimatedLogo(visible = animationPhase >= 1)

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            AnimatedText(visible = animationPhase >= 2)
        }
    }
}

@Composable
private fun AnimatedLogo(visible: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_glow"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale * pulse)
                .alpha(alpha * glowAlpha * 0.4f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple,
                            AccentPink,
                            Color.Transparent
                        )
                    )
                )
                .blur(30.dp)
        )

        // Middle glow
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale * pulse)
                .alpha(alpha * glowAlpha * 0.6f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple,
                            AccentViolet,
                            Color.Transparent
                        )
                    )
                )
                .blur(20.dp)
        )

        // Logo background
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GradientPurpleStart,
                            GradientPurpleEnd
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun AnimatedText(visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "text_alpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "text_offset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(alpha)
            .offset(y = offsetY.dp)
    ) {
        Text(
            text = "SecureOps",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "CI/CD Security Platform",
            style = MaterialTheme.typography.bodyLarge,
            color = NeonPurple.copy(alpha = 0.8f)
        )
    }
}
