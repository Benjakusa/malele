package com.benjakusa.malele

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.benjakusa.malele.data.local.ThemePreferences
import com.benjakusa.malele.data.repository.AuthRepository
import com.benjakusa.malele.ui.nav.MaleleNavHost
import com.benjakusa.malele.ui.theme.LocalThemeState
import com.benjakusa.malele.ui.theme.MaleleTheme
import com.benjakusa.malele.ui.theme.rememberMaleleThemeState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState = rememberMaleleThemeState()
            androidx.compose.runtime.LaunchedEffect(Unit) {
                themePreferences.isDarkMode.collect { saved ->
                    if (themeState.isDark != saved) themeState.set(saved)
                }
            }
            CompositionLocalProvider(LocalThemeState provides themeState) {
                MaleleTheme(darkTheme = themeState.isDark) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MaleleNavHost(authRepository = authRepository)
                    }
                }
            }
        }
    }
}
