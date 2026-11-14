package com.benjakusa.malele.ui.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.ui.theme.AfricanRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpSheet(
    viewModel: CreditsViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("100") }
    var localError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearMessages()
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Top up credits",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Enter your M-Pesa number and the amount in KES. " +
                        "You'll get an STK push on your phone to approve.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    localError = null
                    viewModel.clearMessages()
                },
                label = { Text("M-Pesa phone") },
                placeholder = { Text("07XX XXX XXX") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it.filter { c -> c.isDigit() }
                    localError = null
                    viewModel.clearMessages()
                },
                label = { Text("Amount (KES)") },
                placeholder = { Text("100") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(50, 100, 250, 500).forEach { k ->
                    Button(
                        onClick = { amount = k.toString() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AfricanRed.copy(alpha = 0.12f),
                            contentColor = AfricanRed,
                        ),
                    ) {
                        Text(text = "KES $k", fontSize = 12.sp)
                    }
                }
            }

            val err = localError ?: state.errorMessage
            val msg = state.purchaseMessage
            if (err != null) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            } else if (msg != null && state.pendingCheckoutId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AfricanRed,
                    )
                    Text(text = msg, color = AfricanRed, fontSize = 13.sp)
                }
            } else if (msg != null) {
                Text(text = msg, color = AfricanRed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val p = phone.trim()
                    val a = amount.trim().toIntOrNull()
                    when {
                        p.length < 9 -> localError = "Enter a valid M-Pesa number"
                        a == null || a < 10 -> localError = "Minimum top-up is KES 10"
                        a > 250_000 -> localError = "Maximum top-up is KES 250,000"
                        else -> viewModel.purchase(p, a)
                    }
                },
                enabled = !state.isPurchasing && state.pendingCheckoutId == null,
                colors = ButtonDefaults.buttonColors(containerColor = AfricanRed),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(text = "Send STK push", color = Color.White)
                }
            }

            if (state.pendingCheckoutId != null || state.purchaseMessage != null) {
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            viewModel.clearMessages()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Close")
                }
            }
        }
    }
}
