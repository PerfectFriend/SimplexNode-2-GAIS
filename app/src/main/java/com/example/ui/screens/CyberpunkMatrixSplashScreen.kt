package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * Animated Splash Screen with WOW Effect.
 * Aesthetics: Cyberpunk + Van Gogh Swirling Starry Night + Matrix Digital Rain + Gaudi Organic Architecture.
 * Depicts the Paranoid Cypherpunk Hacker in action with high-tech HUD & live boot sequence.
 */
@Composable
fun CyberpunkMatrixSplashScreen(
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // Infinite transitions for ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "cyberpunk_infinite")

    // Van Gogh swirl rotation
    val swirlRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "swirl_rotation"
    )

    // Breathing pulse for neon aura
    val neonPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_pulse"
    )

    // Parallax subtle scale for the master illustration
    val parallaxScale by infiniteTransition.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "parallax_scale"
    )

    // Scanline vertical offset
    val scanlineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_y"
    )

    // Glitch trigger state
    var glitchActive by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2000..4500).random().toLong())
            glitchActive = true
            delay(120)
            glitchActive = false
        }
    }

    // Interactive Boot Sequence States
    val bootLogs = remember {
        listOf(
            "INITIALIZING ZERO-KNOWLEDGE CYBERSPACE CORE v4.9...",
            "AUTHENTICATING HARDWARE-BACKED MASTER VAULT KEY...",
            "ROUTING 3-HOP TOR ONION TUNNEL (SOCKS5 :9050)... [ACTIVE]",
            "DECOUPLING UNIDIRECTIONAL SMP MESSAGE QUEUES...",
            "ENFORCING ANDROID FLAG_SECURE ANTI-FORENSIC SHIELD...",
            "GAUDI-MATRIX CRYPTOGRAPHIC ENCLAVE ENGAGED."
        )
    }

    var visibleLogCount by remember { mutableIntStateOf(0) }
    var bootProgress by remember { mutableFloatStateOf(0f) }
    var isReadyToEnter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (i in bootLogs.indices) {
            delay(500)
            visibleLogCount = i + 1
            bootProgress = (i + 1).toFloat() / bootLogs.size
            try {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
        delay(400)
        isReadyToEnter = true

        // Automatically dismiss after a comfortable WOW immersion window (4.5s)
        delay(3500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03070D))
            .clickable {
                try {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (_: Exception) {}
                onDismiss()
            }
            .testTag("cyberpunk_splash_screen")
    ) {
        // LAYER 1: Master Artwork Background with Van Gogh & Cyberpunk aesthetics
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(parallaxScale)
                .alpha(if (glitchActive) 0.82f else 0.94f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_paranoid_hacker_splash),
                contentDescription = "Cyberpunk Van Gogh Matrix Gaudi Cryptohacker",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark atmospheric gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x66000B14),
                                Color(0xDD02070F),
                                Color(0xFF010408)
                            )
                        )
                    )
            )

            // Vignette top & bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xCC03070E),
                            0.2f to Color.Transparent,
                            0.75f to Color(0xAA02060C),
                            1.0f to Color(0xFA010408)
                        )
                    )
            )
        }

        // LAYER 2: Van Gogh Swirling Starry Night Vortex & Gaudi Fractal Arches Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.65f * neonPulse)
        ) {
            val center = Offset(size.width * 0.5f, size.height * 0.42f)
            val maxDim = max(size.width, size.height)

            // Rotating Van Gogh brushstroke spirals
            rotate(swirlRotation, pivot = center) {
                val spirals = 6
                for (s in 0 until spirals) {
                    val baseAngle = (s * 360f / spirals) * (PI / 180f)
                    val points = 50
                    val path = Path()

                    for (p in 0..points) {
                        val t = p.toFloat() / points
                        val r = t * maxDim * 0.48f
                        val angle = baseAngle + t * 4f
                        val x = center.x + r * cos(angle).toFloat()
                        val y = center.y + r * sin(angle).toFloat()

                        if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    // Van Gogh vibrant electric palette: Cyan, Neon Green, Cobalt, Gaudí Golden Sparks
                    val spiralColor = when (s % 4) {
                        0 -> Color(0xFF00FF66)
                        1 -> Color(0xFF00E5FF)
                        2 -> Color(0xFF2979FF)
                        else -> Color(0xFFFFD600)
                    }

                    drawPath(
                        path = path,
                        color = spiralColor.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 18f), 0f)
                        )
                    )
                }
            }

            // Gaudí organic catenary arches radiating from the center
            val archCount = 8
            for (a in 0 until archCount) {
                val angleDeg = (a * 360f / archCount) + (swirlRotation * 0.3f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val radius = size.width * 0.38f * neonPulse

                val archEnd = Offset(
                    (center.x + radius * cos(angleRad)).toFloat(),
                    (center.y + radius * sin(angleRad)).toFloat()
                )

                drawLine(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00FFB2), Color(0x4400E5FF), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    start = center,
                    end = archEnd,
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 14f), 0f)
                )
            }
        }

        // LAYER 3: The Matrix Digital Rain Stream Overlay
        MatrixRainCanvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.55f)
        )

        // LAYER 4: Cyberpunk Scanline & Glitch Raster
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw horizontal scanline beam
            val beamY = scanlineY % size.height
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x3300FF99), Color.Transparent),
                    startY = beamY - 40f,
                    endY = beamY + 40f
                ),
                start = Offset(0f, beamY),
                end = Offset(size.width, beamY),
                strokeWidth = 20f
            )

            // Subtle raster scanlines
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color(0x15000000),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += 4f
            }
        }

        // LAYER 5: Foreground Cypherpunk HUD, Typography & Interactive Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD: System ID & Security Badges
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PARANOID_X :: PROTOCOL v4.9",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            fontSize = 10.sp,
                            color = Color(0xFF00FF66)
                        )
                    }

                    // Security Tier Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3300E5FF))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FLAG_SECURE ON",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle with artistic heritage
                Text(
                    text = "AESTHETICS: CYBERPUNK • VAN GOGH • MATRIX • GAUDÍ",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF88A0B8).copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }

            // MIDDLE / LOWER: Animated Terminal Boot Sequence
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD9040810))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF00FF66), Color(0xFF00E5FF).copy(alpha = 0.4f), Color(0xFF2979FF))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CRYPTO ENCLAVE INITIALIZATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FF66)
                        )
                    }

                    Text(
                        text = "${(bootProgress * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Glowing Progress Indicator
                LinearProgressIndicator(
                    progress = { bootProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00FF66),
                    trackColor = Color(0xFF132230)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Terminal lines
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bootLogs.take(visibleLogCount).forEach { log ->
                        Text(
                            text = "> $log",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (log.contains("ACTIVE") || log.contains("ENGAGED")) Color(0xFF00FF66) else Color(0xFFB0C4DE)
                        )
                    }
                }
            }

            // BOTTOM: Call-to-Action WOW Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Button with Glowing Pulsating Border
                Button(
                    onClick = {
                        try {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF66),
                        contentColor = Color(0xFF02090F)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("enter_node_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReadyToEnter) "ACCESS SECURE NODE" else "INITIALIZING VAULT...",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "[ TAP ANYWHERE TO ENTER ]",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF6B8299),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Custom Canvas for The Matrix digital rain effect with Cypherpunk, Greek & Hex glyphs.
 * Optimized with hardware-accelerated native drawing to prevent CPU overload and frame drops.
 */
@Composable
private fun MatrixRainCanvas(modifier: Modifier = Modifier) {
    val characters = remember {
        listOf(
            "0", "1", "X", "Z", "F", "9", "A", "C",
            "λ", "φ", "Ω", "Ψ", "Δ", "Σ", "∇", "⊕",
            "0x", "7F", "E2", "B4", "FF", "9A"
        )
    }

    val columnCount = 14
    val drops = remember {
        Array(columnCount) {
            MatrixDrop(
                y = Random.nextFloat() * 1200f,
                speed = Random.nextFloat() * 7f + 5f,
                length = Random.nextInt(6, 12),
                charSeed = Random.nextInt(0, 100)
            )
        }
    }

    // Animation frame clock for smooth cascading matrix rain
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(33) // ~30 fps
            frameTick++
            for (drop in drops) {
                drop.y += drop.speed
                if (drop.y > 1600f) {
                    drop.y = -Random.nextFloat() * 250f
                    drop.speed = Random.nextFloat() * 7f + 5f
                }
            }
        }
    }

    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 26f
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    Canvas(modifier = modifier) {
        val colWidth = size.width / columnCount

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            for (i in 0 until columnCount) {
                val drop = drops[i]
                val x = i * colWidth + (colWidth * 0.2f)

                for (j in 0 until drop.length) {
                    val charY = drop.y - (j * 22f)
                    if (charY in 0f..size.height) {
                        val charIndex = (drop.charSeed + j + (frameTick / 4).toInt()) % characters.size
                        val charText = characters[charIndex]

                        val colorInt = when (j) {
                            0 -> 0xFFFFFFFF.toInt() // Bright white glowing head
                            1 -> 0xFF80FFB2.toInt() // Glowing cyan-green
                            2 -> 0xFF00FF66.toInt() // Classic matrix green
                            else -> {
                                val alpha = ((1f - (j.toFloat() / drop.length)).coerceIn(0.12f, 0.75f) * 255).toInt()
                                (alpha shl 24) or 0x008F39
                            }
                        }
                        paint.color = colorInt
                        paint.isFakeBoldText = (j == 0)
                        nativeCanvas.drawText(charText, x, charY, paint)
                    }
                }
            }
        }
    }
}

private class MatrixDrop(
    var y: Float,
    var speed: Float,
    var length: Int,
    var charSeed: Int
)
