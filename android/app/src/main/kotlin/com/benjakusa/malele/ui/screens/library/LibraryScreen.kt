package com.benjakusa.malele.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjakusa.malele.data.network.dto.SongDto
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.components.ThemeToggleButton
import com.benjakusa.malele.ui.components.ThinWovenStrip
import com.benjakusa.malele.ui.components.VinylRecord
import com.benjakusa.malele.ui.theme.PureRed

@Composable
fun LibraryScreen(
    onSongClick: (SongDto) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ThinWovenStrip()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = viewModel::refresh) {
                    Text("Refresh")
                }
                ThemeToggleButton()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.songs.size} song${if (state.songs.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.songs.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(110.dp)
                                    .rotate(-10f),
                                shape = RoundedCornerShape(16.dp),
                                color = PureRed,
                            ) {}
                            Surface(
                                modifier = Modifier
                                    .size(110.dp)
                                    .rotate(6f)
                                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    VinylRecord(modifier = Modifier.size(70.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "Your songs will live here",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Every song you generate is saved to your library, ready to replay, edit and share.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.songs, key = { it.id }) { song ->
                            SongCard(song, onClick = { onSongClick(song) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongCard(song: SongDto, onClick: () -> Unit = {}) {
    MaleleCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (song.genre != null) {
                    Text(
                        text = "🎵 ${song.genre}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
                if (song.language != null) {
                    Text(
                        text = "🌍 ${song.language}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
