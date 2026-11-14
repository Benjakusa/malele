package com.benjakusa.malele.ui.screens.create

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector
import com.benjakusa.malele.ui.components.ShadowChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjakusa.malele.ui.components.MaleleButton
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.components.ThemeToggleButton
import com.benjakusa.malele.ui.components.WovenProgressBar
import com.benjakusa.malele.ui.theme.AfricanRed
import com.benjakusa.malele.ui.theme.PureRed
import com.benjakusa.malele.ui.theme.WovenYellow
import com.benjakusa.malele.ui.theme.maleleTextFieldColors
import com.benjakusa.malele.ui.credits.CreditPill
import com.benjakusa.malele.ui.credits.CreditsViewModel
import com.benjakusa.malele.ui.credits.TopUpSheet
import com.benjakusa.malele.ui.components.MaleleErrorDialog
import com.benjakusa.malele.ui.components.ErrorDialogType

data class Genre(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

val AFRICAN_GENRES = listOf(
    Genre("benga", "Benga", Icons.Filled.MusicNote),
    Genre("rhumba", "Rhumba", Icons.Filled.Piano),
    Genre("soukous", "Soukous", Icons.Filled.GraphicEq),
    Genre("gospel", "Gospel", Icons.Filled.Church),
    Genre("afro-dance", "Afro-dance", Icons.Filled.Celebration),
    Genre("reggae", "Reggae", Icons.Filled.SelfImprovement),
    Genre("slow-african", "Slow African", Icons.Filled.Nightlight),
    Genre("amapiano", "Amapiano", Icons.Filled.Festival),
)

val LANGUAGES = listOf(
    "en" to "English",
    "sw" to "Swahili",
    "luo" to "Dholuo",
    "luhya" to "Luhya",
    "kikuyu" to "Kikuyu",
    "kamba" to "Kikamba",
    "kisii" to "Ekegusii",
    "meru" to "Kimeru",
    "maasai" to "Maa",
    "somali" to "Somali",
    "taita" to "Kitaita",
    "taveta" to "Kitaveta",
    "mijikenda" to "Mijikenda",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: CreateViewModel = hiltViewModel(),
    creditsViewModel: CreditsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val creditsState by creditsViewModel.state.collectAsState()
    var showTopUp by remember { mutableStateOf(false) }

    if (showTopUp) {
        TopUpSheet(
            viewModel = creditsViewModel,
            onDismiss = { showTopUp = false },
        )
    }

    state.errorCode?.let { code ->
        val (type, title, body) = errorDialogFor(code, state.errorMessage)
        val isCredits = type == ErrorDialogType.CREDITS
        MaleleErrorDialog(
            type = type,
            title = title,
            message = body,
            onDismiss = { viewModel.clearError() },
            primaryActionLabel = if (isCredits) "Top up" else null,
            onPrimaryAction = if (isCredits) {
                {
                    viewModel.clearError()
                    showTopUp = true
                }
            } else null,
        )
    }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Create",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CreditPill(
                        available = creditsState.available,
                        isLoading = creditsState.isLoadingBalance,
                        onClick = { showTopUp = true },
                    )
                    ThemeToggleButton()
                }
            }
            Spacer(Modifier.height(16.dp))

            // Two white buttons side by side: Add audio & Add voice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionButton(
                    text = "Add audio",
                    icon = { Icon(Icons.Filled.Waves, contentDescription = null, tint = AfricanRed) },
                    onClick = { showAudioSheet = true },
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    text = "Add voice",
                    icon = { Icon(Icons.Filled.Mic, contentDescription = null, tint = AfricanRed) },
                    onClick = { showVoiceSheet = true },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))

            // Prompt card
            MaleleCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Describe your song",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.prompt,
                        onValueChange = viewModel::setPrompt,
                        placeholder = { Text("A Benga love song with interlocking guitars and warm vocals") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5, colors = maleleTextFieldColors(),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Genre horizontal scroll
            MaleleCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = AfricanRed,
                        )
                        Text(
                            text = "Genre",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AFRICAN_GENRES.forEach { genre ->
                            val selected = state.genre == genre.id
                            ShadowChip(
                                label = genre.label,
                                icon = genre.icon,
                                selected = selected,
                                onClick = {
                                    viewModel.setGenre(if (selected) null else genre.id)
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Vocals + Language — side by side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MaleleCard(modifier = Modifier.weight(0.38f).fillMaxHeight()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = AfricanRed,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Vocals",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ShadowChip(
                                label = "Vocals",
                                icon = Icons.Filled.Mic,
                                selected = state.vocals,
                                onClick = { viewModel.setVocals(true) },
                            )
                            ShadowChip(
                                label = "Instrumental",
                                icon = Icons.Filled.MusicNote,
                                selected = state.instrumental,
                                onClick = { viewModel.setVocals(false) },
                            )
                        }
                    }
                }
                MaleleCard(modifier = Modifier.weight(0.62f).fillMaxHeight()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Translate,
                                contentDescription = null,
                                tint = AfricanRed,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Language",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LANGUAGES.forEach { (code, label) ->
                                ShadowChip(
                                    label = label,
                                    icon = if (code == "en") Icons.Filled.Public
                                           else Icons.Filled.Translate,
                                    selected = state.language == code,
                                    onClick = { viewModel.setLanguage(code) },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Sections header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sections (${state.sections.size})",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Button(
                    onClick = viewModel::addSection,
                    colors = ButtonDefaults.buttonColors(containerColor = AfricanRed),
                ) {
                    Text("+ Add section", color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))

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
                    Spacer(Modifier.height(16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))


            // Primary Generate button
            MaleleButton(
                text = "Generate",
                onClick = viewModel::submit,
                enabled = state.prompt.isNotBlank(),
                isLoading = state.isLoading,
            )

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
                    isSaving = state.isSaving,
                    savedSongId = state.savedSongId,
                    onSave = { viewModel.saveJob() },
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaleleCard(onClick = onClick, modifier = modifier.height(56.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onBackground,
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
    MaleleCard {
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
                    modifier = Modifier.weight(1f), colors = maleleTextFieldColors(),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRemove) {
                    Text("Remove", color = AfricanRed)
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${section.durationSeconds}s",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 20.sp),
                    color = AfricanRed,
                )
            }
            Slider(
                value = section.durationSeconds.toFloat(),
                onValueChange = { secs -> onChange { it.copy(durationSeconds = secs.toInt()) } },
                valueRange = 5f..60f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AfricanRed,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = section.instruments,
                onValueChange = { v -> onChange { it.copy(instruments = v) } },
                label = { Text("Instruments") },
                placeholder = { Text("heavy kick, congas, interlocking guitars, bass") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(), colors = maleleTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = section.vocals,
                onValueChange = { v -> onChange { it.copy(vocals = v) } },
                label = { Text("Vocals") },
                placeholder = { Text("warm male tenor, group harmonies") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(), colors = maleleTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = section.mood,
                onValueChange = { v -> onChange { it.copy(mood = v) } },
                label = { Text("Mood") },
                placeholder = { Text("anthemic, joyful, building") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = maleleTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = section.negative,
                onValueChange = { v -> onChange { it.copy(negative = v) } },
                label = { Text("What to avoid (optional)") },
                placeholder = { Text("no auto-tune, no synth bass") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = maleleTextFieldColors(),
            )
            Spacer(Modifier.height(16.dp))

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
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (onWriteWithAi != null) {
            Button(
                onClick = onWriteWithAi,
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = AfricanRed),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("✨ Write with AI", color = Color.White)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = lyrics,
        onValueChange = onLyricsChange,
        placeholder = { Text("Taa za jioni zinawaka, moyo wangu unarudi kwako...") },
        textStyle = MaterialTheme.typography.bodyMedium, // Italic serif for songbook page feel
        modifier = Modifier.fillMaxWidth().height(120.dp),
        maxLines = 6, colors = maleleTextFieldColors(),
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
    isSaving: Boolean,
    savedSongId: String?,
    onSave: () -> Unit,
) {
    MaleleCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    val isReady = audioReady || jobState == "completed"
                    Text(
                        text = if (isReady) "Your song is ready" else "Generating song",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.DarkGray,
                        ) {
                            Text(
                                text = "MOCK",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, color = Color.White),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                        // Processing pill or completed badge
                        if (isReady) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = WovenYellow,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "completed",
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, color = Color.Black),
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = AfricanRed,
                            ) {
                                Text(
                                    text = "processing",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, color = Color.White),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
                if (isPolling && !audioReady) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AfricanRed,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Woven progress bar (filled a bit over half or based on state)
            val progress = if (audioReady) 1f else 0.55f
            WovenProgressBar(progress = progress)
            Spacer(Modifier.height(16.dp))

            if (audioReady && audioUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                com.benjakusa.malele.ui.components.AudioPlayer(
                    audioUrl = audioUrl,
                    authToken = authToken,
                )
                Spacer(Modifier.height(12.dp))
                if (savedSongId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = WovenYellow)
                        Text(
                            text = "Saved to library",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = AfricanRed),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Save to library", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


private fun errorDialogFor(
    code: String,
    backendMessage: String?,
): Triple<ErrorDialogType, String, String> {
    return when (code) {
        "CREDITS_INSUFFICIENT" -> Triple(
            ErrorDialogType.CREDITS,
            "Out of Credits",
            "You need credits to generate a song. Top up with M-Pesa to continue — it only takes a moment.",
        )
        "PROVIDER_OUT_OF_CREDITS" -> Triple(
            ErrorDialogType.SERVICE,
            "Service Temporarily Unavailable",
            "Our music generation service is temporarily offline while we replenish credits. Your work is saved. Please try again in a few hours.",
        )
        "LYRICS_GENERATE_FAILED", "PROMPT_ENHANCE_FAILED" -> Triple(
            ErrorDialogType.LYRICS,
            "Lyrics Generation Failed",
            "We couldn't write the lyrics this time. Try again, or write them yourself in the section editor.",
        )
        "NETWORK_ERROR", "TIMEOUT" -> Triple(
            ErrorDialogType.NETWORK,
            "Connection Issue",
            "We couldn't reach the server. Check your internet connection and try again.",
        )
        else -> Triple(
            ErrorDialogType.UNKNOWN,
            "Something Went Wrong",
            backendMessage?.takeIf { it.isNotBlank() } ?: "Please try again.",
        )
    }
}
