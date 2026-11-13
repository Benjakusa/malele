package com.benjakusa.malele.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.domain.model.VoiceTrack
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.theme.PureRed
import com.benjakusa.malele.ui.theme.WovenYellow

@Composable
fun VoiceRecordPanel(
    isRecording: Boolean,
    amplitude: Float,
    voiceTracks: List<VoiceTrack>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDeleteVoiceTrack: (takeId: String) -> Unit,
    onSetVoiceVolume: (takeId: String, vol: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaleleCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Vocal Recording",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Record vocals directly over your beat.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))

            // Record Button & Live Amplitude Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) PureRed else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Stop Recording" else "Record Voice",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Live Amplitude Level Bar
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Mic Level:", fontSize = 12.sp, color = PureRed)
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(amplitude)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PureRed)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Recorded Vocal Takes
            if (voiceTracks.isNotEmpty()) {
                Text(
                    text = "Vocal Takes (${voiceTracks.size})",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))

                voiceTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.width(90.dp),
                        )
                        Slider(
                            value = track.volume,
                            onValueChange = { vol -> onSetVoiceVolume(track.takeId, vol) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = PureRed,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { onDeleteVoiceTrack(track.takeId) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Take",
                                tint = PureRed,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
