package com.benjakusa.malele

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.benjakusa.malele.data.repository.AuthRepository
import com.benjakusa.malele.ui.nav.MaleleNavHost
import com.benjakusa.malele.ui.theme.MaleleTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaleleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MaleleNavHost(authRepository = authRepository)
                }
            }
        }
    }
}
