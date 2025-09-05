package com.elad.zmanim

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

/* Preferred: Lottie JSON from assets
 * File path: app/src/main/assets/lottie/candle_light.json
 */
@Composable
fun CandleLottieJson(
    modifier: Modifier = Modifier,
    assetPath: String = "lottie/candle_light.json"
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    if (composition == null) {
        AnimatedCandles(modifier = modifier) // fallback if JSON missing
    } else {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier
        )
    }
}

/** Convenience wrapper to use inside CandleHeaderBox. */
@Composable
fun CandleHeaderCandle(
    modifier: Modifier = Modifier,
    assetPath: String = "lottie/candle_light.json"
) {
    CandleLottieJson(modifier = modifier, assetPath = assetPath)
}

/** Two side-by-side candles for the header (keeps Lottie/fallback behavior). */
@Composable
fun TwoCandles(
    modifier: Modifier = Modifier,
    leftAsset: String = "lottie/candle_1.json",
    rightAsset: String = "lottie/candle_2.json"
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CandleHeaderCandle(
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer { scaleX = 1f },   // 🔒 never mirrored
            assetPath = leftAsset
        )
        CandleHeaderCandle(
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { scaleX = 1f },   // 🔒 never mirrored
            assetPath = rightAsset
        )
    }
}

/* ── Original canvas-based animated candles (fallback) ─────────────────────── */

@Composable
fun AnimatedCandles(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "flame")
    val a1 by infinite.animateFloat(
        0.6f, 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "a1"
    )
    val a2 by infinite.animateFloat(
        1.0f, 0.6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "a2"
    )
    val bob1 by infinite.animateFloat(
        0f, 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "b1"
    )
    val bob2 by infinite.animateFloat(
        1.2f, 0f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "b2"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val candleW = w * 0.22f
        val candleH = h * 0.52f
        val gap = w * 0.08f
        val leftX = w / 2 - candleW - gap / 2
        val rightX = w / 2 + gap / 2
        val baseY = h * 0.82f

        fun body(x: Float, height: Float, color: Color) {
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, baseY - height),
                size = androidx.compose.ui.geometry.Size(candleW, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(candleW * 0.2f, candleW * 0.2f)
            )
        }
        body(leftX, candleH, Color(0xFFFFF8E1))
        body(rightX, candleH * 0.85f, Color(0xFFFFFDE7))

        translate(leftX + candleW / 2, baseY - candleH - 5f + bob1) { drawFlame(a1) }
        translate(rightX + candleW / 2, baseY - candleH * 0.85f - 5f + bob2) { drawFlame(a2) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlame(alpha: Float) {
    drawCircle(color = Color(0xFFFFECB3), radius = 10f, alpha = 0.5f * alpha)
    drawCircle(color = Color(0xFFFFC107), radius = 5f, alpha = alpha)
}

/* ── Countdown utilities (unchanged) ──────────────────────────────────────── */

@Composable
fun countdownLine(tz: ZoneId, target: ZonedDateTime?): String {
    val now = remember { mutableStateOf(ZonedDateTime.now(tz)) }
    LaunchedEffect(target?.toInstant(), tz) {
        while (true) {
            now.value = ZonedDateTime.now(tz)
            delay(1000)
        }
    }
    if (target == null) return "—"
    val remaining = Duration.between(now.value, target)
    if (remaining.isNegative) return "שבת שלום!"

    var secs = remaining.seconds
    val days = (secs / 86_400).toInt(); secs %= 86_400
    val hours = (secs / 3600).toInt(); secs %= 3600
    val minutes = (secs / 60).toInt()
    val seconds = (secs % 60).toInt()

    val prefix = if (days > 0) "$days ימים " else ""
    val base = "%02d:%02d:%02d".format(hours, minutes, seconds)

    val unitLabel = when {
        hours > 0 -> "שעות"
        minutes > 0 -> "דקות"
        else -> "שניות"
    }

    return "$prefix$base $unitLabel"
}

fun isNegative(tz: ZoneId, target: ZonedDateTime): Boolean {
    return Duration.between(ZonedDateTime.now(tz), target).isNegative
}
