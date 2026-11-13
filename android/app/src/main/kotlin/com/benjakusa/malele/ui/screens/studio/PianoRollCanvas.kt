package com.benjakusa.malele.ui.screens.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.benjakusa.malele.domain.model.Note
import com.benjakusa.malele.ui.theme.PureRed
import com.benjakusa.malele.ui.theme.WovenYellow

@Composable
fun PianoRollCanvas(
    notes: List<Note>,
    minPitch: Int = 48, // C3
    maxPitch: Int = 72, // C5
    stepCount: Int = 16,
    onToggleNote: (pitch: Int, step: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pitchCount = maxPitch - minPitch + 1

    Box(
        modifier = modifier
            .padding(4.dp)
            .height(280.dp)
            .pointerInput(notes) {
                detectTapGestures { offset ->
                    val cellWidth = size.width / stepCount.toFloat()
                    val cellHeight = size.height / pitchCount.toFloat()

                    val step = (offset.x / cellWidth).toInt().coerceIn(0, stepCount - 1)
                    val pitchRow = (offset.y / cellHeight).toInt().coerceIn(0, pitchCount - 1)
                    val pitch = maxPitch - pitchRow

                    onToggleNote(pitch, step)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / stepCount.toFloat()
            val cellHeight = size.height / pitchCount.toFloat()

            // Draw horizontal pitch rows
            for (p in 0 until pitchCount) {
                val y = p * cellHeight
                val currentPitch = maxPitch - p
                val isBlackKey = Note.midiToName(currentPitch).contains("#")
                val rowColor = if (isBlackKey) Color(0xFF1E1E1E) else Color(0xFF282828)

                drawRect(
                    color = rowColor,
                    topLeft = Offset(0f, y),
                    size = Size(size.width, cellHeight)
                )
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            // Draw vertical step grid lines
            for (s in 0..stepCount) {
                val x = s * cellWidth
                val isBarLine = s % 4 == 0
                val lineColor = if (isBarLine) WovenYellow.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.3f)
                val strokeWidth = if (isBarLine) 2f else 1f

                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
            }

            // Draw notes
            notes.forEach { note ->
                if (note.pitch in minPitch..maxPitch) {
                    val pitchRow = maxPitch - note.pitch
                    val x = note.startStep * cellWidth
                    val y = pitchRow * cellHeight
                    val width = note.lengthSteps * cellWidth
                    val height = cellHeight - 2f

                    drawRoundRect(
                        color = PureRed,
                        topLeft = Offset(x + 1f, y + 1f),
                        size = Size(width - 2f, height),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = Offset(x + 1f, y + 1f),
                        size = Size(width - 2f, height),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }
    }
}
