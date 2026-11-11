package com.benjakusa.malele.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Create : Screen("create", "Create", Icons.Filled.Add)
    data object Explore : Screen("explore", "Explore", Icons.Filled.Search)
    data object Library : Screen("library", "Library", Icons.Filled.LibraryMusic)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)

    companion object {
        val tabs = listOf(Create, Explore, Library, Profile)
    }
}
