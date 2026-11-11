package com.benjakusa.malele.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Create a song",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Describe your idea, add sections, and control every instrument.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))

        // Prompt
        SectionHeader("Describe your song")
        OutlinedTextField(
            value = state.prompt,
            onValueChange = viewModel::setPrompt,
            placeholder = { Text("A Benga love song with interlocking guitars and warm vocals") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5,
        )
        Spacer(Modifier.height(24.dp))

        // Genre
        SectionHeader("Genre")
        GenrePicker(
            selected = state.genre,
            onSelect = viewModel::setGenre,
        )
        Spacer(Modifier.height(24.dp))

        // Vocals + Language row
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                SectionHeader("Vocals")
                Row {
                    LabeledRadio(
                        label = "Vocals",
                        selected = state.vocals,
                        onClick = { viewModel.setVocals(true) },
                    )
                }
                Row {
                    LabeledRadio(
                        label = "Instrumental",
                        selected = state.instrumental,
                        onClick = { viewModel.setVocals(false) },
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                SectionHeader("Language")
                LANGUAGES.forEach { (code, label) ->
                    Row {
                        LabeledRadio(
                            label = label,
                            selected = state.language == code,
                            onClick = { viewModel.setLanguage(code) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Sections
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader("Sections (${state.sections.size})")
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
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(24.dp))

        // Error + submit
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
        }

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
            Text(
                text = "Job submitted: ${state.submittedJob!!.id.take(8)}...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "State: ${state.submittedJob!!.state}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GenrePicker(
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AFRICAN_GENRES.chunked(2).forEach { rowGenres ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowGenres.forEach { genre ->
                    androidx.compose.material3.FilterChip(
                        selected = selected == genre.id,
                        onClick = {
                            onSelect(if (selected == genre.id) null else genre.id)
                        },
                        label = { Text(genre.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowGenres.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LabeledRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SectionEditor(
    section: SectionInput,
    onChange: ((SectionInput) -> SectionInput) -> Unit,
    onRemove: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: section name + remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = section.name,
                    onValueChange = { newName ->
                        onChange { it.copy(name = newName) }
                    },
                    label = { Text("Section name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
            Spacer(Modifier.height(8.dp))

            // Duration
            Text(
                text = "Duration: ${section.durationSeconds}s",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            androidx.compose.material3.Slider(
                value = section.durationSeconds.toFloat(),
                onValueChange = { secs ->
                    onChange { it.copy(durationSeconds = secs.toInt()) }
                },
                valueRange = 5f..60f,
                steps = 10,
                modifier = Modifier.fillMaxWidth(),
            )

            // Instruments
            OutlinedTextField(
                value = section.instruments,
                onValueChange = { v -> onChange { it.copy(instruments = v) } },
                label = { Text("Instruments") },
                placeholder = { Text("heavy kick, congas, interlocking guitars, bass") },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            // Vocals
            OutlinedTextField(
                value = section.vocals,
                onValueChange = { v -> onChange { it.copy(vocals = v) } },
                label = { Text("Vocals") },
                placeholder = { Text("warm male tenor, group harmonies") },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            // Mood
            OutlinedTextField(
                value = section.mood,
                onValueChange = { v -> onChange { it.copy(mood = v) } },
                label = { Text("Mood") },
                placeholder = { Text("anthemic, joyful, building") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            // Negative
            OutlinedTextField(
                value = section.negative,
                onValueChange = { v -> onChange { it.copy(negative = v) } },
                label = { Text("What to avoid (optional)") },
                placeholder = { Text("no auto-tune, no synth bass") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            // Lyrics placeholder (editable textarea + AI button in next paste)
            LyricsField(
                lyrics = section.lyrics,
                onLyricsChange = { v -> onChange { it.copy(lyrics = v) } },
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
