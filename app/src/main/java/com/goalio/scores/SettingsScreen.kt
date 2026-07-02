package com.goalio.scores

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goalio.scores.ui.theme.GoalioColors
import com.google.firebase.auth.FirebaseAuth
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    onBack: () -> Unit, onHome: () -> Unit, onMatches: () -> Unit,
    onWorldCup: () -> Unit, onGames: () -> Unit,
    onEditProfile: () -> Unit = {}, onLanguage: () -> Unit = {}, onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("goalio_settings", Context.MODE_PRIVATE) }
    var profile by remember { mutableStateOf<BackendProfile?>(null) }
    var leaderboard by remember { mutableStateOf(QuizRepository.cachedLeaderboard(context)) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { GoalioBackendApi.getProfile() }.onSuccess { profile = it }.onFailure { error = it.message }
        leaderboard = runCatching { QuizRepository.leaderboard(context) }.getOrDefault(leaderboard)
    }
    val teams = profile?.favoriteTeams?.takeIf { it.isNotEmpty() } ?: prefs.getStringSet("profile_team_names", emptySet()).orEmpty().toList()
    val players = profile?.favoritePlayers?.takeIf { it.isNotEmpty() } ?: prefs.getStringSet("profile_player_names", emptySet()).orEmpty().toList()
    val xp = leaderboard?.me?.xp ?: 0
    val username = profile?.username ?: prefs.getString("profile_username", null) ?: "player"
    val member = profile?.createdAt?.let { runCatching { OffsetDateTime.parse(it).format(DateTimeFormatter.ofPattern("MMM yyyy")) }.getOrNull() }

    GoalioBackground {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().widthIn(max = 640.dp).align(Alignment.TopCenter),
            contentPadding = PaddingValues(22.dp, 14.dp, 22.dp, 130.dp), verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item { SettingsHeader(onBack, onEditProfile) }
            item { ProfileHero(username, member) }
            item { MasteryCard(xp / 100 + 1, xp % 100, xp) }
            item { SettingsSection("PREFERENCES") {
                SettingsRow("◎", "Language", prefs.getString("language", "English (UK)") ?: "English (UK)", onLanguage)
                SettingsRow("♧", "Notifications", trailing = { Switch(checked = notifications, onCheckedChange = { notifications = it; prefs.edit().putBoolean("notifications_enabled", it).apply() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GoalioColors.Tertiary)) })
                SettingsRow("♟", "Favorite Teams", teams.joinToString().ifBlank { "Add teams" }, onEditProfile)
                SettingsRow("♟", "Favorite Players", players.joinToString().ifBlank { "Add players" }, onEditProfile)
            } }
            item { SettingsSection("SUPPORT") {
                SettingsRow("◉", "Privacy Policy", "↗") { context.openUrl("https://goalio.app/privacy") }
                SettingsRow("☆", "Rate App", "›") { context.openUrl("market://details?id=${context.packageName}") }
                SettingsRow("⌯", "Share App", "›") { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${context.packageName}") }, "Share Goalio")) }
                SettingsRow("ⓘ", "Version", BuildConfig.VERSION_NAME, enabled = false)
            } }
            item { Surface(color = Color(0xFF120202), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF4A0808)), modifier = Modifier.fillMaxWidth().clickable {
                FirebaseAuth.getInstance().signOut(); prefs.edit().remove("profile_complete").remove("profile_username").remove("profile_full_name").apply(); onSignOut()
            }) { Text("Sign Out", color = Color(0xFFFF8B82), fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp)) } }
            error?.let { item { Text(it, color = GoalioColors.Live, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) } }
        }
        GoalioBottomBar(Modifier.align(Alignment.BottomCenter), "", onHome, onMatches, onWorldCup, onGames)
    }
}

@Composable private fun SettingsHeader(onBack: () -> Unit, onEdit: () -> Unit) = Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("‹", color = GoalioColors.Secondary, fontSize = 40.sp, modifier = Modifier.clickable(onClick = onBack).padding(end = 16.dp))
    Text("GOALIO", color = GoalioColors.Secondary, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    Text("✎", color = GoalioColors.Tertiary, fontSize = 29.sp, modifier = Modifier.clickable(onClick = onEdit).padding(start = 16.dp))
}

@Composable private fun ProfileHero(username: String, member: String?) = Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Box(Modifier.size(136.dp).background(Color(0xFF4A3000), CircleShape).padding(5.dp).background(GoalioColors.Tertiary, CircleShape).padding(6.dp).background(GoalioColors.Neutral, CircleShape), contentAlignment = Alignment.Center) {
        Text(username.take(2).uppercase(), color = GoalioColors.Secondary, fontSize = 38.sp, fontWeight = FontWeight.Black); Box(Modifier.size(28.dp).background(GoalioColors.Tertiary, CircleShape).align(Alignment.BottomEnd))
    }
    Spacer(Modifier.height(16.dp)); Text(username, color = GoalioColors.Secondary, fontSize = 29.sp, fontWeight = FontWeight.Black)
    Text("Member since ${member ?: "recently"} • Pro Fan", color = GoalioColors.TextSecondary, fontSize = 15.sp)
}

@Composable private fun MasteryCard(level: Int, levelXp: Int, totalXp: Int) = Surface(color = GoalioColors.Neutral, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GoalioColors.Border), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(24.dp)) {
        Row { Column(Modifier.weight(1f)) { Text("TRIVIA\nMASTERY", color = GoalioColors.TextSecondary, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp); Text("Level\n$level", color = GoalioColors.Tertiary, fontSize = 30.sp, fontWeight = FontWeight.Black) }; Text("⚔", color = GoalioColors.Tertiary, fontSize = 30.sp) }
        Spacer(Modifier.height(24.dp)); Row(verticalAlignment = Alignment.Bottom) { Text("$totalXp /\n${(totalXp / 100 + 1) * 100} XP", color = GoalioColors.Secondary, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); Text("LEVEL PROGRESS", color = GoalioColors.TextSecondary, fontSize = 11.sp) }
        Spacer(Modifier.height(10.dp)); Box(Modifier.fillMaxWidth().height(10.dp).background(GoalioColors.Surface3, CircleShape)) { Box(Modifier.fillMaxWidth(levelXp / 100f).fillMaxHeight().background(GoalioColors.Tertiary, CircleShape)) }
    }
}

@Composable private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) = Column {
    Text(title, color = GoalioColors.TextSecondary, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
    Surface(color = GoalioColors.Neutral, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GoalioColors.Border)) { Column(content = content) }
}

@Composable private fun ColumnScope.SettingsRow(icon: String, label: String, value: String = "", onClick: () -> Unit = {}, enabled: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 74.dp).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = if (enabled) GoalioColors.TextSecondary else GoalioColors.Disabled, fontSize = 25.sp, modifier = Modifier.width(44.dp)); Text(label, color = if (enabled) GoalioColors.TextPrimary else GoalioColors.Disabled, fontSize = 17.sp, modifier = Modifier.weight(1f)); if (trailing != null) trailing() else Text(value, color = if (enabled) GoalioColors.TextSecondary else GoalioColors.Disabled, fontSize = 15.sp, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 180.dp))
    }
    Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(GoalioColors.Divider))
}

private fun Context.openUrl(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
