package com.elad.zmanim

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.animation.core.*
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun AnimatedCandles(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "flame")
    val a1 by infinite.animateFloat(0.6f, 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "a1")
    val a2 by infinite.animateFloat(1.0f, 0.6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "a2")
    val bob1 by infinite.animateFloat(0f, 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "b1")
    val bob2 by infinite.animateFloat(1.2f, 0f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "b2")

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val candleW = w * 0.22f
        val candleH = h * 0.52f
        val gap = w * 0.08f
        val leftX = w/2 - candleW - gap/2
        val rightX = w/2 + gap/2
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
    drawCircle(color = Color(0xFFFFC107), radius = 5f,  alpha = alpha)
}

@Composable
fun countdownLine(tz: ZoneId, target: ZonedDateTime?): String {
    val now = remember { mutableStateOf(ZonedDateTime.now(tz)) }
    LaunchedEffect(target?.toInstant(), tz) {
        while (true) { now.value = ZonedDateTime.now(tz); delay(1000) }
    }
    if (target == null) return "—"
    val remaining = Duration.between(now.value, target)
    if (remaining.isNegative) return "שבת שלום!"

    var secs = remaining.seconds
    val days = (secs / 86_400).toInt(); secs %= 86_400
    val hours = (secs / 3600).toInt();   secs %= 3600
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
