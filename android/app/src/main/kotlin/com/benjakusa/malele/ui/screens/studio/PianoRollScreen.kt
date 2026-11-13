package com.benjakusa.malele.ui.screens.studio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.domain.model.Note
import com.benjakusa.malele.domain.model.Scale
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.theme.PureRed

val INSTRUMENTS = listOf(
    "guitar_bass", "guitar_solo", "guitar_rhythm1", "guitar_rhythm2", "guitar_rhythm3",
    "piano", "epiano", "organ", "synth_pad",
    "violin", "harp", "flute", "trumpet",
    "bass_pluck", "bass_sub",
    "synth_lead", "brass_stab",
    "strings",
)

@Composable
fun PianoRollScreen(
    notes: List<Note>,
    selectedInstrument: String,
    onSelectInstrument: (String) -> Unit,
    onToggleNote: (pitch: Int, step: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedScale by remember { mutableStateOf(Scale.BENGA_PENTATONIC) }

    MaleleCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Piano Roll",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Scale: ${selectedScale.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = PureRed,
                )
            }
            Spacer(Modifier.height(8.dp))

            // Instrument Selector
            Text("Instrument", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                INSTRUMENTS.forEach { inst ->
                    FilterChip(
                        selected = selectedInstrument == inst,
                        onClick = { onSelectInstrument(inst) },
                        label = { Text(inst.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureRed,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scale Selector
            Text("Scale Lock", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Scale.ALL_SCALES.forEach { scale ->
                    FilterChip(
                        selected = selectedScale == scale,
                        onClick = { selectedScale = scale },
                        label = { Text(scale.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureRed,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Interactive Piano Roll Grid
            PianoRollCanvas(
                notes = notes.filter { it.instrumentType == selectedInstrument },
                minPitch = 48, // C3
                maxPitch = 72, // C5
                stepCount = 16,
                onToggleNote = onToggleNote,
            )
        }
    }
}
