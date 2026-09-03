package com.joi.app.ui.raffle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The pointer sits at 12 o'clock. Compose's arc angles start at 3 o'clock and grow clockwise,
 * so that's 270° in the same frame — the one constant the landing maths depends on. */
const val POINTER_ANGLE_DEGREES = 270f

/** Past this many slices the labels are thinner than they are readable, so the wheel drops them
 * and becomes a band of colour. The drawn number is announced in the result card either way. */
private const val MAX_LABELLED_SLICES = 24

/**
 * Where the wheel must come to rest for [index] to sit under the pointer, given how far it has
 * already turned. Always resolves forward and adds whole turns, so every spin visibly travels
 * several rotations rather than twitching to a neighbouring slice.
 */
fun targetRotationFor(index: Int, sliceCount: Int, currentRotation: Float, fullTurns: Int = 5): Float {
    if (sliceCount <= 0) return currentRotation
    val slice = 360f / sliceCount
    val sliceCentre = index * slice + slice / 2f
    // Where the wheel has to be, ignoring how many turns it took to get there.
    val resting = ((POINTER_ANGLE_DEGREES - sliceCentre) % 360f + 360f) % 360f
    val turnsSoFar = Math.floor((currentRotation / 360f).toDouble()).toFloat()
    var target = turnsSoFar * 360f + resting
    // Landing on or behind where we already are would look like no spin at all.
    while (target <= currentRotation) target += 360f
    return target + fullTurns * 360f
}

/** Slice colours, cycled. Taken from the theme's container roles so the wheel reads correctly in
 * both light and dark without a second palette to maintain. */
@Composable
private fun sliceColors(): List<Pair<Color, Color>> = listOf(
    MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
    MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
    MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant,
)

/**
 * The wheel: one slice per number in play, rotated by [rotation] degrees, with a fixed pointer at
 * the top. Drawing is pure — the caller owns the animation and decides where it stops.
 */
@Composable
fun WheelOfNumbers(
    numbers: List<Int>,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    val colors = sliceColors()
    val rimColor = MaterialTheme.colorScheme.outline
    val pointerColor = MaterialTheme.colorScheme.primary
    val hubColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation },
        ) {
            drawWheel(numbers, colors, rimColor, hubColor)
        }
        // Drawn outside the rotating layer so it stays put while the wheel turns under it.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPointer(pointerColor)
        }
    }
}

private fun DrawScope.drawWheel(
    numbers: List<Int>,
    colors: List<Pair<Color, Color>>,
    rimColor: Color,
    hubColor: Color,
) {
    if (numbers.isEmpty()) return
    val diameter = min(size.width, size.height)
    val radius = diameter / 2f
    val centre = Offset(size.width / 2f, size.height / 2f)
    val topLeft = Offset(centre.x - radius, centre.y - radius)
    val arcSize = Size(diameter, diameter)
    val slice = 360f / numbers.size

    numbers.forEachIndexed { index, number ->
        val (background, foreground) = colors[index % colors.size]
        drawArc(
            color = background,
            startAngle = index * slice,
            // A hairline overlap; without it the anti-aliased seams show the background through.
            sweepAngle = slice + 0.5f,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
        )
        if (numbers.size <= MAX_LABELLED_SLICES) {
            drawSliceLabel(number, index * slice + slice / 2f, centre, radius, foreground, numbers.size)
        }
    }

    // Rim and hub, drawn last so they sit over every seam.
    drawCircle(color = rimColor, radius = radius, center = centre, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
    drawCircle(color = hubColor, radius = radius * 0.16f, center = centre)
    drawCircle(
        color = rimColor,
        radius = radius * 0.16f,
        center = centre,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
    )
}

private fun DrawScope.drawSliceLabel(
    number: Int,
    angleDegrees: Float,
    centre: Offset,
    radius: Float,
    color: Color,
    sliceCount: Int,
) {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val distance = radius * 0.68f
    val x = centre.x + distance * cos(radians).toFloat()
    val y = centre.y + distance * sin(radians).toFloat()

    // Shrink the text as slices get thinner, with a floor so it never becomes a speck.
    val textSize = (radius * 0.16f).coerceAtMost(radius * 2.2f / sliceCount).coerceAtLeast(18f)
    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        this.textSize = textSize
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.save()
    // Turn the label to face outward along its own slice, so it reads along the radius.
    drawContext.canvas.nativeCanvas.rotate(angleDegrees + 90f, x, y)
    // Nudge down by half the text height so the glyphs sit centred on the point, not above it.
    drawContext.canvas.nativeCanvas.drawText(number.toString(), x, y + textSize / 3f, paint)
    drawContext.canvas.nativeCanvas.restore()
}

private fun DrawScope.drawPointer(color: Color) {
    val centreX = size.width / 2f
    val radius = min(size.width, size.height) / 2f
    val top = size.height / 2f - radius
    val width = radius * 0.11f
    val depth = radius * 0.16f

    // A downward triangle biting into the rim, so it's unambiguous which slice it names.
    val path = Path().apply {
        moveTo(centreX - width, top - depth * 0.25f)
        lineTo(centreX + width, top - depth * 0.25f)
        lineTo(centreX, top + depth)
        close()
    }
    drawPath(path, color)
}
