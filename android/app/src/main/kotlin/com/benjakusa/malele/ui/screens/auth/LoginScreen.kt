package com.benjakusa.malele.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjakusa.malele.ui.components.DiagonalImageHero
import com.benjakusa.malele.ui.components.MaleleButton
import com.benjakusa.malele.ui.components.MaleleTextField
import com.benjakusa.malele.ui.components.ThemeToggleButton

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (state.user != null) {
        onLoginSuccess()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        val screenHeight = maxHeight
        val heroHeight = screenHeight * 0.43f

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
            ) {
                DiagonalImageHero(height = heroHeight)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = "Malele",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Songs with an African heartbeat.",
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Welcome back",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(20.dp))

                    MaleleTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = "Email",
                        placeholder = "you@example.com",
                    )
                    Spacer(Modifier.height(16.dp))

                    MaleleTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = "Password",
                        placeholder = "••••••••",
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    Spacer(Modifier.height(16.dp))

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    MaleleButton(
                        text = "Log in",
                        onClick = { viewModel.login(email, password) },
                        isLoading = state.isLoading,
                    )

                    Spacer(Modifier.height(16.dp))

                    TextButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = "Don't have an account? Register",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }

            ThemeToggleButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                tint = Color.White,
            )
        }
    }
}
