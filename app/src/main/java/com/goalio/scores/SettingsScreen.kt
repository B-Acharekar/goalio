package com.goalio.scores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goalio.scores.ui.theme.GoalioColors

@Composable
fun SettingsScreen(onBack: () -> Unit, onHome: () -> Unit, onMatches: () -> Unit, onWorldCup: () -> Unit, onGames: () -> Unit) {
    var profile by remember { mutableStateOf<BackendProfile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching { GoalioBackendApi.getProfile() }.onSuccess { profile = it }.onFailure { error = it.message } }
    GoalioBackground {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentPadding = PaddingValues(22.dp, 18.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { GoalioTopBar("SETTINGS", onBack = onBack, onSettings = {}) }
            item { Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder)) { Column(Modifier.fillMaxWidth().padding(22.dp)) { Text(profile?.let { "@${it.username}" } ?: "YOUR PROFILE", color = GoalioColors.TextPrimary, fontWeight = FontWeight.Black); profile?.name?.let { Text(it, color = GoalioColors.TextSecondary, modifier = Modifier.padding(top = 5.dp)) }; error?.let { Text(it, color = GoalioColors.Live) } } } }
            profile?.favoriteTeams?.takeIf { it.isNotEmpty() }?.let { teams -> item { PreferenceCard("FAVORITE TEAMS", teams) } }
            profile?.favoritePlayers?.takeIf { it.isNotEmpty() }?.let { players -> item { PreferenceCard("FAVORITE PLAYERS", players) } }
            item { PreferenceCard("PREFERENCES", listOf("Language: English", "Notifications: Enabled")) }
        }
        GoalioBottomBar(Modifier.align(androidx.compose.ui.Alignment.BottomCenter), "", onHome, onMatches, onWorldCup, onGames)
    }
}

@Composable private fun PreferenceCard(title: String, values: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, color = GoalioColors.TextSecondary, fontWeight = FontWeight.Black); Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { values.forEach { Text(it, color = GoalioColors.TextPrimary, fontWeight = FontWeight.Bold) } } } }
}
