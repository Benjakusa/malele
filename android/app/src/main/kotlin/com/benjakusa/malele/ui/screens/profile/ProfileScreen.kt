package com.benjakusa.malele.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjakusa.malele.BuildConfig
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.components.MaleleTextField
import com.benjakusa.malele.ui.components.WovenRingAvatar
import com.benjakusa.malele.ui.theme.LocalThemeState

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val themeState = LocalThemeState.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
    ) {
        // Avatar + name
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WovenRingAvatar(sizeDp = 120.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.user?.displayName ?: state.user?.email?.substringBefore("@") ?: "You",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.user?.email ?: "Loading...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.user?.role?.uppercase() ?: "",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(24.dp))

        // ── Section: Account ──
        SectionTitle("Account")
        MaleleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Display name", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(state.user?.displayName ?: "(not set)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                    TextButton(onClick = viewModel::openEditNameDialog) {
                        Text("Edit", color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Email", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(state.user?.email ?: "-", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── Section: Appearance ──
        SectionTitle("Appearance")
        MaleleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Dark mode", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (themeState.isDark) "Dark theme active" else "Light theme active",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    }
                    Switch(
                        checked = themeState.isDark,
                        onCheckedChange = { dark ->
                            themeState.set(dark)
                            viewModel.setTheme(dark)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── Section: Actions ──
        SectionTitle("Actions")
        MaleleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                TextButton(
                    onClick = { viewModel.logout(onLogout) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Log out", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── Section: Danger zone ──
        SectionTitle("Danger zone")
        MaleleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                TextButton(
                    onClick = viewModel::openDeleteAccountDialog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── Section: About ──
        SectionTitle("About")
        MaleleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Version", color = MaterialTheme.colorScheme.onSurface)
                    Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("App", color = MaterialTheme.colorScheme.onSurface)
                    Text("Malele", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Status message
        if (state.errorMessage != null) {
            Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }
        if (state.savedMessage != null) {
            Text(state.savedMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(32.dp))

        // ── Dialogs ──
        if (state.showEditNameDialog) {
            EditNameDialog(
                initial = state.user?.displayName ?: "",
                onDismiss = viewModel::dismissEditNameDialog,
                onSave = viewModel::saveDisplayName,
            )
        }
        if (state.showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteAccountDialog,
                title = { Text("Delete account") },
                text = { Text("Account deletion will be available in a future update. Contact support to delete your account for now.") },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissDeleteAccountDialog) {
                        Text("Got it")
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
private fun EditNameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit display name dialog") },
        text = {
            Column(modifier = Modifier.imePadding()) {
                MaleleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Display name",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
