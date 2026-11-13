package com.benjakusa.malele.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.domain.model.DrumPattern
import com.benjakusa.malele.domain.model.MixerState
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.theme.PureRed
import com.benjakusa.malele.ui.theme.WovenYellow

@Composable
fun MixerPanel(
    mixerState: MixerState,
    trackIds: List<String>,
    onSetPan: (track: Int, pan: Float) -> Unit,
    onToggleMute: (track: Int) -> Unit,
    onToggleSolo: (track: Int) -> Unit,
    onSetMasterVolume: (vol: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaleleCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mixer & Pan",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))

            // Per-track controls
            for (t in trackIds.indices) {
                val isMuted = mixerState.mutes[t]
                val isSolo = mixerState.solos[t]
                val pan = mixerState.pans[t]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = DrumPattern.displayName(trackIds[t]),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.width(60.dp),
                    )

                    // Mute Button
                    Button(
                        onClick = { onToggleMute(t) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMuted) PureRed else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .width(42.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "M",
                            color = if (isMuted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(6.dp))

                    // Solo Button
                    Button(
                        onClick = { onToggleSolo(t) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSolo) WovenYellow else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .width(42.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "S",
                            color = if (isSolo) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))

                    // Pan Slider (-1 L to 1 R)
                    Text("L", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Slider(
                        value = pan,
                        onValueChange = { p -> onSetPan(t, p) },
                        valueRange = -1.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = PureRed,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text("R", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Master Volume Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Master Vol",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(90.dp),
                )
                Slider(
                    value = mixerState.masterVolume,
                    onValueChange = onSetMasterVolume,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = PureRed,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(mixerState.masterVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(40.dp),
                )
            }
        }
    }
}
