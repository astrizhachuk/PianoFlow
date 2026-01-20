package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrizhachuk.pianoflow.domain.model.Note

private const val TREBLE_CLEF_PATH_DATA = "M16.5,33.5 C21.5,29.5 24.0,26.5 24.0,23.0 C24.0,19.5 21.5,16.5 18.0,16.5 C14.5,16.5 12.0,19.5 12.0,23.0 C12.0,26.5 14.5,29.5 16.5,33.5 M16.5,33.5 C15.5,39.5 15.5,43.5 16.5,48.5 M16.5,48.5 C17.5,52.5 20.5,55.5 24.0,55.5 C27.5,55.5 30.5,52.5 30.5,48.5 C30.5,44.5 27.5,41.5 24.0,41.5 C20.5,41.5 18.5,44.5 18.5,48.5 C18.5,52.5 20.5,55.5 24.0,55.5"
private const val BASS_CLEF_PATH_DATA = "M12.0,30.0 C19.0,25.0 22.0,28.0 22.0,32.0 C22.0,36.0 19.0,39.0 15.0,38.0 C11.0,37.0 10.0,34.0 12.0,30.0 Z"

@Composable
fun PianoStaff(
    notes: List<Note>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val staffHeight = 160.dp
    val noteHeadRadius = 4.dp

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val middleC = 60
            val lineGap = (size.height / 20)
            val staffLineThickness = 1.dp.toPx()
            val clefWidth = 60.dp.toPx()

            // Draw treble staff
            drawStaff(5, Offset(0f, size.height / 2 - 6 * lineGap), lineGap, size.width, staffLineThickness, lineColor)
            // Draw bass staff
            drawStaff(5, Offset(0f, size.height / 2 + 2 * lineGap), lineGap, size.width, staffLineThickness, lineColor)

            // Draw treble clef
            drawClef(TREBLE_CLEF_PATH_DATA, Offset(15.dp.toPx(), size.height / 2 - 5.5f * lineGap), 1.6f, lineColor)
            // Draw bass clef
            drawClef(BASS_CLEF_PATH_DATA, Offset(15.dp.toPx(), size.height / 2 + 2.5f * lineGap), 1.5f, lineColor)
            // Dots for bass clef
            drawCircle(lineColor, radius = 2.dp.toPx(), center = Offset(45.dp.toPx(), size.height / 2 + 2.5f * lineGap))
            drawCircle(lineColor, radius = 2.dp.toPx(), center = Offset(45.dp.toPx(), size.height / 2 + 3.5f * lineGap))

            // Draw notes
            val noteX = clefWidth + 30.dp.toPx()
            notes.forEach { note ->
                val yPos = pitchToY(note.pitch, size.height / 2, lineGap)
                drawNote(noteX, yPos, noteHeadRadius.toPx() * 1.4f, noteHeadRadius.toPx(), lineColor)

                if (note.pitch == middleC) {
                    drawLine(lineColor, start = Offset(noteX - 12.dp.toPx(), yPos), end = Offset(noteX + 12.dp.toPx(), yPos), strokeWidth = staffLineThickness)
                }
            }
        }
    }
}

private fun DrawScope.drawStaff(lineCount: Int, start: Offset, lineGap: Float, width: Float, strokeWidth: Float, color: Color) {
    for (i in 0 until lineCount) {
        val y = start.y + i * lineGap
        drawLine(
            color = color,
            start = Offset(start.x, y),
            end = Offset(width, y),
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawClef(pathData: String, offset: Offset, scale: Float, color: Color) {
    val path = Path().apply {
        pathData.split(" ").forEach { segment ->
            when (segment.first()) {
                'M' -> {
                    val (x, y) = segment.substring(1).split(",").map { it.toFloat() * scale }
                    moveTo(x + offset.x, y + offset.y)
                }
                'C' -> {
                    val coords = segment.substring(1).split(",").map { it.toFloat() * scale }
                    cubicTo(coords[0] + offset.x, coords[1] + offset.y, coords[2] + offset.x, coords[3] + offset.y, coords[4] + offset.x, coords[5] + offset.y)
                }
                'Z' -> close()
            }
        }
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
}


private fun DrawScope.drawNote(x: Float, y: Float, width: Float, height: Float, color: Color) {
    drawOval(
        color = color,
        topLeft = Offset(x - width / 2, y - height / 2),
        size = Size(width, height)
    )
}

private fun pitchToY(pitch: Int, middleY: Float, lineGap: Float): Float {
    val pitchMap = mapOf(
        // Treble Clef
        71 to (middleY - 5.5f * lineGap), // F5
        70 to (middleY - 5f * lineGap),
        69 to (middleY - 5f * lineGap),   // E5
        68 to (middleY - 4.5f * lineGap),
        67 to (middleY - 4.5f * lineGap), // D5
        66 to (middleY - 4f * lineGap),
        65 to (middleY - 4f * lineGap),   // C5
        64 to (middleY - 3.5f * lineGap), // B4
        63 to (middleY - 3f * lineGap),
        62 to (middleY - 3f * lineGap),   // A4
        61 to (middleY - 2.5f * lineGap),
        60 to (middleY - 2.5f * lineGap), // G4 (Middle C) -> Should be middleY, special case
        // Middle C
        60 to middleY,
        // Bass Clef
        59 to (middleY + 1.5f * lineGap), // B3
        58 to (middleY + 2f * lineGap),
        57 to (middleY + 2f * lineGap),   // A3
        56 to (middleY + 2.5f * lineGap),
        55 to (middleY + 2.5f * lineGap), // G3
        54 to (middleY + 3f * lineGap),
        53 to (middleY + 3f * lineGap),   // F3
        52 to (middleY + 3.5f * lineGap), // E3
        51 to (middleY + 4f * lineGap),
        50 to (middleY + 4f * lineGap),   // D3
        49 to (middleY + 4.5f * lineGap),
        48 to (middleY + 4.5f * lineGap), // C3
        47 to (middleY + 5f * lineGap),   // B2
        46 to (middleY + 5.5f * lineGap),
        45 to (middleY + 5.5f * lineGap)  // A2
    )

    // Simplified diatonic mapping for prototype
    val step = lineGap / 2
    return when (pitch) {
        // Treble
        83 -> middleY - 8.5f * step // B6
        81 -> middleY - 7.5f * step // A6
        79 -> middleY - 6.5f * step // G6
        77 -> middleY - 5.5f * step // F6
        76 -> middleY - 5 * step // E5
        74 -> middleY - 4 * step // D5
        72 -> middleY - 3 * step // C5
        71 -> middleY - 2.5f * step // B4
        69 -> middleY - 1.5f * step // A4
        67 -> middleY - 0.5f * step // G4
        65 -> middleY + 0.5f * step // F4
        64 -> middleY + 1 * step   // E4
        62 -> middleY + 2 * step   // D4
        60 -> middleY + 3 * step   // C4 (Middle C)
        // Bass
        59 -> middleY + 4 * step   // B3
        57 -> middleY + 5 * step   // A3
        55 -> middleY + 6 * step   // G3
        53 -> middleY + 7 * step   // F3
        52 -> middleY + 7.5f * step // E3
        50 -> middleY + 8.5f * step // D3
        48 -> middleY + 9.5f * step // C3
        47 -> middleY + 10f * step  // B2
        45 -> middleY + 11f * step  // A2
        43 -> middleY + 12f * step  // G2
        41 -> middleY + 13f * step  // F2
        40 -> middleY + 13.5f * step// E2
        else -> {
            // Fallback for notes not in the simplified map - this is very approximate
            // This is not musically accurate for accidentals, but places the note near its diatonic neighbor.
            val referencePitch = 60 // Middle C
            val referenceY = middleY + 3 * step
            val diatonicSteps = listOf(0, 2, 4, 5, 7, 9, 11) // Major scale intervals
            val octave = (pitch - referencePitch) / 12
            val noteInOctave = (pitch - referencePitch) % 12
            // find closest diatonic step
            val diatonicNote = diatonicSteps.minByOrNull { kotlin.math.abs(it - noteInOctave) } ?: 0
            val noteIndex = diatonicSteps.indexOf(diatonicNote)
            val y = referenceY - (octave * 7 + noteIndex) * step
            y
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PianoStaffPreview_Empty() {
    MaterialTheme {
        PianoStaff(notes = emptyList(), modifier = Modifier.height(200.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PianoStaffPreview_MiddleC() {
    MaterialTheme {
        PianoStaff(notes = listOf(Note(60)), modifier = Modifier.height(200.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PianoStaffPreview_CMajorChord() {
    MaterialTheme {
        PianoStaff(notes = listOf(Note(60), Note(64), Note(67)), modifier = Modifier.height(200.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PianoStaffPreview_ComplexChord() {
    MaterialTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PianoStaff(
                notes = listOf(Note(45), Note(52), Note(55), Note(60), Note(64)),
                modifier = Modifier.height(200.dp).padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            PianoStaff(
                notes = listOf(Note(60), Note(64), Note(67), Note(72)),
                modifier = Modifier.height(200.dp).padding(horizontal = 16.dp)
            )
        }
    }
}
