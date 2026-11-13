package com.benjakusa.malele.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.benjakusa.malele.data.repository.AuthRepository
import com.benjakusa.malele.ui.screens.auth.LoginScreen
import com.benjakusa.malele.ui.screens.auth.RegisterScreen
import com.benjakusa.malele.ui.screens.create.CreateScreen
import com.benjakusa.malele.ui.screens.explore.ExploreScreen
import com.benjakusa.malele.ui.screens.library.LibraryScreen
import com.benjakusa.malele.ui.screens.profile.ProfileScreen
import com.benjakusa.malele.ui.screens.songdetail.SongDetailScreen
import com.benjakusa.malele.ui.screens.studio.StudioScreen
import com.benjakusa.malele.ui.theme.AfricanRed
import com.benjakusa.malele.ui.theme.LocalThemeState


sealed class AuthScreen(val route: String) {
    data object Login : AuthScreen("auth/login")
    data object Register : AuthScreen("auth/register")
}

@Composable
fun MaleleNavHost(
    authRepository: AuthRepository,
) {
    // Check auth state once at startup. Session-agnostic (we don't watch for changes here).
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        isLoggedIn = authRepository.isLoggedIn()
    }

    // Show nothing while checking (a few ms, imperceptible)
    if (isLoggedIn == null) return

    if (isLoggedIn == true) {
        MainNavGraph(onLogout = { isLoggedIn = false })
    } else {
        AuthNavGraph(onAuthSuccess = { isLoggedIn = true })
    }
}

@Composable
private fun AuthNavGraph(onAuthSuccess: () -> Unit) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AuthScreen.Login.route,
    ) {
        composable(AuthScreen.Login.route) {
            LoginScreen(
                onLoginSuccess = onAuthSuccess,
                onNavigateToRegister = { navController.navigate(AuthScreen.Register.route) },
            )
        }
        composable(AuthScreen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = onAuthSuccess,
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainNavGraph(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val themeState = LocalThemeState.current
    val isDark = themeState.isDark
    val shadowColor = if (isDark) Color.White else Color.Black
    val surfaceColor = MaterialTheme.colorScheme.background

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        ambientColor = shadowColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                        spotColor = shadowColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                    ),
                color = surfaceColor,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = backStackEntry?.destination

                    Screen.tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        if (selected) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = AfricanRed,
                                modifier = Modifier.height(40.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Create.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Create.route) { CreateScreen() }
            composable(Screen.Explore.route) { ExploreScreen() }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onSongClick = { song ->
                        val jobId = song.jobId ?: return@LibraryScreen
                        navController.navigate("song/$jobId?title=${java.net.URLEncoder.encode(song.title, "UTF-8")}&genre=${song.genre ?: ""}&language=${song.language ?: ""}")
                    }
                )
            }
            composable(
                route = "song/{jobId}?title={title}&genre={genre}&language={language}",
                arguments = listOf(
                    androidx.navigation.navArgument("jobId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    androidx.navigation.navArgument("genre") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    androidx.navigation.navArgument("language") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                ),
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId").orEmpty()
                val title = backStackEntry.arguments?.getString("title").orEmpty()
                val genre = backStackEntry.arguments?.getString("genre").orEmpty()
                val language = backStackEntry.arguments?.getString("language").orEmpty()
                SongDetailScreen(
                    jobId = jobId,
                    title = title,
                    genre = genre.ifBlank { null },
                    language = language.ifBlank { null },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Profile.route) { ProfileScreen(onLogout = onLogout) }
            composable(Screen.Studio.route) { StudioScreen() }
        }
    }
}
