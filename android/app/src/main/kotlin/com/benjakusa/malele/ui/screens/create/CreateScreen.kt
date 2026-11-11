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
            text = "Describe your idea, pick a genre, and let Malele make it.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))

        // Section: Prompt
        SectionLabel("Describe your song")
        OutlinedTextField(
            value = state.prompt,
            onValueChange = viewModel::setPrompt,
            placeholder = { Text("A Benga love song with interlocking guitars and warm male vocals") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5,
        )
        Spacer(Modifier.height(24.dp))

        // Section: Genre
        SectionLabel("Genre")
        GenreGrid(
            genres = AFRICAN_GENRES,
            selected = state.genre,
            onSelect = viewModel::setGenre,
        )
        Spacer(Modifier.height(24.dp))

        // Section: Vocals
        SectionLabel("Vocals")
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabeledRadio(
                label = "Vocals",
                selected = state.vocals,
                onClick = { viewModel.setVocals(true) },
            )
            Spacer(Modifier.width(16.dp))
            LabeledRadio(
                label = "Instrumental",
                selected = state.instrumental,
                onClick = { viewModel.setVocals(false) },
            )
        }
        Spacer(Modifier.height(24.dp))

        // Section: Language
        SectionLabel("Language")
        Row {
            LANGUAGES.forEach { (code, label) ->
                LabeledRadio(
                    label = label,
                    selected = state.language == code,
                    onClick = { viewModel.setLanguage(code) },
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // Section: Duration + Submit
        DurationSlider(
            seconds = state.durationSeconds,
            onChange = viewModel::setDuration,
        )
        Spacer(Modifier.height(24.dp))

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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun GenreGrid(
    genres: List<Genre>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        genres.chunked(2).forEach { rowGenres ->
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
private fun DurationSlider(
    seconds: Int,
    onChange: (Int) -> Unit,
) {
    SectionLabel("Duration: ${seconds}s")
    androidx.compose.material3.Slider(
        value = seconds.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = 30f..300f,
        steps = 8,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("30s", style = MaterialTheme.typography.labelLarge)
        Text("5m", style = MaterialTheme.typography.labelLarge)
    }
}
