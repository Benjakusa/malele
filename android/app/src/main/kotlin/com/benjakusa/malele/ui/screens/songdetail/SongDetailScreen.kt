package com.benjakusa.malele.ui.screens.songdetail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjakusa.malele.ui.components.AudioPlayer
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.components.ThemeToggleButton

@Composable
fun SongDetailScreen(
    jobId: String,
    title: String,
    genre: String?,
    language: String?,
    onBack: () -> Unit,
    viewModel: SongDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(jobId) {
        viewModel.ensureLocalCopy(jobId, title)
    }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
                ThemeToggleButton()
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (genre != null) {
                    Text(
                        text = "🎵 $genre",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
                if (language != null) {
                    Text(
                        text = "🌍 $language",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            MaleleCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    AudioPlayer(
                        audioUrl = viewModel.audioSource(jobId),
                        authToken = viewModel.authToken(),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.prepareShare(jobId, title) { file ->
                            val intent = viewModel.shareIntent(context, file)
                            context.startActivity(Intent.createChooser(intent, "Share song"))
                        }
                    },
                    enabled = !state.isDownloading,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Share")
                    }
                }

                Button(
                    onClick = { viewModel.saveToDevice(context, jobId, title) },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save to device")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to library")
            }
        }
    }
}
