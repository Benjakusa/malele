package com.benjakusa.malele.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjakusa.malele.ui.components.AudioPlayer

data class Genre(val id: String, val label: String)

val AFRICAN_GENRES = listOf(
    Genre("benga", "Benga"),
    Genre("rhumba", "Rhumba"),
    Genre("soukous", "Soukous"),
    Genre("gospel", "Gospel"),
    Genre("afro-dance", "Afro-dance"),
    Genre("reggae", "Reggae"),
    Genre("slow-african", "Slow African"),
    Genre("amapiano", "Amapiano"),
)

val LANGUAGES = listOf(
    "en" to "English",
    "sw" to "Swahili",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAudioSheet by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }

    // Placeholder sheets — coming soon
    if (showAudioSheet) {
        ComingSoonSheet(
            title = "Add reference audio",
            body = "Upload or record audio — the AI will use the style and energy of the audio to shape your song. Coming soon.",
            onDismiss = { showAudioSheet = false },
        )
    }
    if (showVoiceSheet) {
        ComingSoonSheet(
            title = "Choose or create a voice",
            body = "Pick a preset voice, or enroll your own by recording 30 seconds of your voice reading a script. Coming soon.",
            onDismiss = { showVoiceSheet = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // TOP: 2 buttons side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassButton(
                text = "+ Audio",
                onClick = { showAudioSheet = true },
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = "+ Voice",
                onClick = { showVoiceSheet = true },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))

        // Prompt (glass card)
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Describe your song",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = viewModel::setPrompt,
                    placeholder = { Text("A Benga love song with interlocking guitars and warm vocals") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Genre (horizontal scroll)
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Genre",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AFRICAN_GENRES.forEach { genre ->
                        FilterChip(
                            selected = state.genre == genre.id,
                            onClick = {
                                viewModel.setGenre(if (state.genre == genre.id) null else genre.id)
                            },
                            label = { Text(genre.label) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Vocals (horizontal scroll)
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Vocals",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.vocals,
                        onClick = { viewModel.setVocals(true) },
                        label = { Text("Vocals") },
                    )
                    FilterChip(
                        selected = state.instrumental,
                        onClick = { viewModel.setVocals(false) },
                        label = { Text("Instrumental") },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Language (horizontal scroll)
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Language",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LANGUAGES.forEach { (code, label) ->
                        FilterChip(
                            selected = state.language == code,
                            onClick = { viewModel.setLanguage(code) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Sections header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sections (${state.sections.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(onClick = viewModel::addSection) {
                Text("+ Add section")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.sections.isEmpty()) {
            Text(
                text = "No sections yet. Add sections to describe instruments, vocals, and lyrics per part.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        } else {
            state.sections.forEach { section ->
                SectionEditor(
                    section = section,
                    onChange = { update -> viewModel.updateSection(section.id, update) },
                    onRemove = { viewModel.removeSection(section.id) },
                    onWriteWithAi = { viewModel.writeLyricsForSection(section.id) },
                    isWritingLyrics = state.lyricsLoadingSectionId == section.id,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(24.dp))

        // Error
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
        }

        // Submit
        Button(
            onClick = viewModel::submit,
            enabled = !state.isLoading && state.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Generate", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (state.submittedJob != null) {
            Spacer(Modifier.height(16.dp))
            JobStatusCard(
                jobId = state.submittedJob!!.id,
                provider = state.jobProvider,
                jobState = state.jobState,
                isPolling = state.isPolling,
                audioReady = state.audioReady,
                audioUrl = state.audioUrl,
                authToken = state.authToken,
            )
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.6f),
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
    ) {
        content()
    }
}

@Composable
private fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.6f),
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ComingSoonSheet(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
    )
}

@Composable
private fun SectionEditor(
    section: SectionInput,
    onChange: ((SectionInput) -> SectionInput) -> Unit,
    onRemove: () -> Unit,
    onWriteWithAi: () -> Unit,
    isWritingLyrics: Boolean,
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = section.name,
                    onValueChange = { newName -> onChange { it.copy(name = newName) } },
                    label = { Text("Section name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRemove) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Duration: ${section.durationSeconds}s",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = section.durationSeconds.toFloat(),
                onValueChange = { secs -> onChange { it.copy(durationSeconds = secs.toInt()) } },
                valueRange = 5f..60f,
                steps = 10,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = section.instruments,
                onValueChange = { v -> onChange { it.copy(instruments = v) } },
                label = { Text("Instruments") },
                placeholder = { Text("heavy kick, congas, interlocking guitars, bass") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = section.vocals,
                onValueChange = { v -> onChange { it.copy(vocals = v) } },
                label = { Text("Vocals") },
                placeholder = { Text("warm male tenor, group harmonies") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = section.mood,
                onValueChange = { v -> onChange { it.copy(mood = v) } },
                label = { Text("Mood") },
                placeholder = { Text("anthemic, joyful, building") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = section.negative,
                onValueChange = { v -> onChange { it.copy(negative = v) } },
                label = { Text("What to avoid (optional)") },
                placeholder = { Text("no auto-tune, no synth bass") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            LyricsField(
                lyrics = section.lyrics,
                onLyricsChange = { v -> onChange { it.copy(lyrics = v) } },
                onWriteWithAi = onWriteWithAi,
                isGenerating = isWritingLyrics,
            )
        }
    }
}

@Composable
private fun LyricsField(
    lyrics: String,
    onLyricsChange: (String) -> Unit,
    onWriteWithAi: (() -> Unit)? = null,
    isGenerating: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Lyrics",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onWriteWithAi != null) {
            OutlinedButton(
                onClick = onWriteWithAi,
                enabled = !isGenerating,
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("✨ Write with AI")
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = lyrics,
        onValueChange = onLyricsChange,
        placeholder = { Text("Write lyrics for this section...") },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        maxLines = 6,
    )
}

@Composable
private fun JobStatusCard(
    jobId: String,
    provider: String,
    jobState: String,
    isPolling: Boolean,
    audioReady: Boolean,
    audioUrl: String,
    authToken: String,
) {
    val stateColor = when (jobState) {
        "completed" -> MaterialTheme.colorScheme.secondary
        "failed", "cancelled" -> MaterialTheme.colorScheme.error
        "processing", "downloading" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    }

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Generating song",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Job ${jobId.take(8)}... · ${provider.ifBlank { "mock" }}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
                if (isPolling) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "State: ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Text(
                    text = jobState,
                    style = MaterialTheme.typography.titleLarge,
                    color = stateColor,
                )
            }

            if (audioReady && audioUrl.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "🎵 Your song is ready",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                AudioPlayer(
                    audioUrl = audioUrl,
                    authToken = authToken,
                )
            }
        }
    }
}
