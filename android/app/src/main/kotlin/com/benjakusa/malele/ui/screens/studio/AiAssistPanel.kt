package com.benjakusa.malele.ui.screens.studio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.theme.PureRed

val AI_BEAT_STYLES = listOf("Benga", "Rhumba", "Soukous", "Highlife", "Afro-dance")

@Composable
fun AiAssistPanel(
    isAiLoading: Boolean,
    onSuggestBeat: (style: String) -> Unit,
    onGenerateMelody: () -> Unit,
    onFinishBeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedStyle by remember { mutableStateOf("Benga") }

    MaleleCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PureRed)
                Text(
                    text = "AI Co-Producer",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Use AI to suggest rhythms, generate vocal melodies, or complete your pattern.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(12.dp))

            // Style Picker
            Text("Select African Style", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AI_BEAT_STYLES.forEach { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { selectedStyle = style },
                        label = { Text(style) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureRed,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onSuggestBeat(selectedStyle) },
                    enabled = !isAiLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PureRed),
                    modifier = Modifier.weight(1f),
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Suggest Beat", color = Color.White, fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onGenerateMelody,
                    enabled = !isAiLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Gen Melody", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onFinishBeat,
                enabled = !isAiLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Finish This Beat Pattern")
            }
        }
    }
}
