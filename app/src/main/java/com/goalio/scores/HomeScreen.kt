package com.goalio.scores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val HomePanel = Color(0xFF1D1D1F)
private val HomeCard = Color(0xFF101112)
private val HomeMuted = Color(0xFFB9B5B6)
private val HomeGold = Color(0xFFE2C98C)
private val GoalRed = Color(0xFFFF351C)

private val HomeLeagues = listOf(
    "fifa.world",
    "eng.1",
    "esp.1",
    "ita.1",
    "ger.1",
    "fra.1",
    "uefa.champions",
    "uefa.europa"
)

@Composable
fun PersonalizedHomeScreen(
    fallbackName: String?,
    fallbackTeams: Set<String>,
    fallbackPlayers: Set<String>
) {
    var matches by remember { mutableStateOf(emptyList<ScheduleMatch>()) }
    var loading by remember { mutableStateOf(true) }
    val today = remember { LocalDate.now().toString() }

    LaunchedEffect(today) {
        loading = true
        matches = coroutineScope {
            HomeLeagues.map { league ->
                async {
                    runCatching { GoalioBackendApi.getSchedule(league, today).matches }
                        .getOrDefault(emptyList())
                }
            }.flatMap { it.await() }
        }.distinctBy { "${it.league}:${it.matchId}" }
        loading = false
    }

    val featured = matches.firstOrNull { it.state == "in" }
        ?: matches.firstOrNull { it.state == "pre" }
        ?: matches.firstOrNull()
    val liveMatches = matches.filter { it.state == "in" }
    val upcoming = matches.filter { it.state == "pre" }.take(4)
    val finished = matches.filter { it.state == "post" }.take(3)

    GoalioBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item { HomeTopBar() }
            item {
                if (featured != null) FeaturedMatchCard(featured)
                else EmptyHeroCard(loading)
            }
            item {
                SectionHeader("Live Action", "View All")
                Spacer(Modifier.height(12.dp))
                if (liveMatches.isEmpty()) {
                    MutedPill("No live matches right now")
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(liveMatches.take(6), key = { "${it.league}:${it.matchId}" }) {
                            LiveActionCard(it)
                        }
                    }
                }
            }
            item {
                Text("UPCOMING TODAY", color = Color(0xFFD9D3D4), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                if (upcoming.isEmpty()) {
                    MutedPill("No upcoming fixtures found for today")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        upcoming.forEach { UpcomingRow(it) }
                    }
                }
            }
            item { WinProbabilityCard(featured) }
            item { MiniHubCard(finished, fallbackTeams) }
            item { FunZone(fallbackPlayers) }
        }
        HomeBottomNav(Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HomeTopBar() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("☰", color = Color.White, fontSize = 28.sp)
        Spacer(Modifier.width(18.dp))
        Text("GOALIO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, modifier = Modifier.weight(1f))
        Text("⌕", color = HomeGold, fontSize = 30.sp)
        Spacer(Modifier.width(16.dp))
        Text("◖", color = HomeGold, fontSize = 27.sp)
        Spacer(Modifier.width(16.dp))
        Text("⚙", color = HomeGold, fontSize = 24.sp)
    }
}

@Composable
private fun FeaturedMatchCard(match: ScheduleMatch) {
    Surface(
        color = HomePanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2B2B2D)),
        modifier = Modifier.fillMaxWidth().height(282.dp)
    ) {
        Box {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF4D8B62).copy(alpha = .30f), Color.Transparent),
                        center = Offset(size.width * .5f, size.height * .65f),
                        radius = size.width * .75f
                    ),
                    radius = size.width * .75f,
                    center = Offset(size.width * .5f, size.height * .65f)
                )
                drawLine(Color.White.copy(alpha = .10f), Offset(0f, size.height * .70f), Offset(size.width, size.height * .44f), 3f)
            }
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${matchMinute(match)} • ${match.statusLabel().uppercase()}", color = GoalRed, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text(match.leagueLabel(), color = Color.White.copy(alpha = .72f), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TeamBadge(match.homeTeam, Modifier.weight(1f))
                    Text(scoreLine(match), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    TeamBadge(match.awayTeam, Modifier.weight(1f))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(.9f).height(58.dp)
                ) {
                    Text("⚽  Match Details", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun EmptyHeroCard(loading: Boolean) {
    Surface(color = HomePanel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(if (loading) "Loading today's matches..." else "No matches found today", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TeamBadge(team: MatchTeamInfo?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(74.dp).clip(CircleShape).background(Color.White.copy(alpha = .92f)), contentAlignment = Alignment.Center) {
            if (!team?.logo.isNullOrBlank()) {
                AsyncImage(team?.logo, contentDescription = team?.name, contentScale = ContentScale.Fit, modifier = Modifier.size(58.dp))
            } else {
                Text(team?.abbreviation ?: "TBD", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(team?.abbreviation ?: team?.shortName ?: "TBD", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text(action, color = Color(0xFFCFC8CA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LiveActionCard(match: ScheduleMatch) {
    Surface(
        color = HomeCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF272729)),
        modifier = Modifier.width(210.dp).height(126.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("${matchMinute(match)} ${match.leagueLabel(short = true)}", color = GoalRed, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("${match.homeTeam?.name ?: "Home"} ${match.homeTeam?.score ?: 0}", color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${match.awayTeam?.name ?: "Away"} ${match.awayTeam?.score ?: 0}", color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun UpcomingRow(match: ScheduleMatch) {
    Surface(color = HomePanel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(64.dp)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatKickoff(match.kickoff), color = Color(0xFFD9D4D5), fontSize = 18.sp)
            Spacer(Modifier.width(18.dp))
            Text(match.shortName ?: match.name ?: "Match", color = Color(0xFFE8E3E4), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("›", color = Color.White, fontSize = 32.sp)
        }
    }
}

@Composable
private fun WinProbabilityCard(match: ScheduleMatch?) {
    Surface(color = HomePanel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF515153)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("⌁ Win Probability", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(18.dp))
            Row {
                Text(match?.homeTeam?.name?.uppercase() ?: "HOME", color = HomeMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(match?.awayTeam?.name?.uppercase() ?: "AWAY", color = HomeMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50))) {
                Box(Modifier.weight(.55f).fillMaxSize().background(Color.White))
                Box(Modifier.weight(.45f).fillMaxSize().background(GoalRed))
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("55%", color = HomeMuted, fontSize = 18.sp)
                Spacer(Modifier.width(20.dp))
                Text("45%", color = GoalRed, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Surface(color = Color(0xFF3F4141), shape = RoundedCornerShape(50)) {
                    Text("View Analysis", color = Color.White.copy(alpha = .78f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniHubCard(finished: List<ScheduleMatch>, fallbackTeams: Set<String>) {
    Surface(color = Color(0xFF101110), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF303030)), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().background(Color(0xFF5C5B5A)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🌎 World Cup Hub", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("View Hub", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            val rows = finished.mapNotNull { it.homeTeam?.name }.take(3).ifEmpty { fallbackTeams.take(3).ifEmpty { listOf("Brazil", "Argentina", "France") } }
            rows.forEachIndexed { index, team ->
                Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", color = Color.White, fontSize = 17.sp, modifier = Modifier.width(42.dp))
                    Text(team, color = Color(0xFFEAE3D3), fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${12 - index * 2}", color = Color.White, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun FunZone(fallbackPlayers: Set<String>) {
    Column {
        Text("Fun Zone", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FunTile("▣", "Daily Trivia", Modifier.weight(1f))
            FunTile("☷", "Guess Player", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = Color(0xFFE6E2D4), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Box {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color(0xFFE6BB58).copy(alpha = .55f), radius = size.width * .22f, center = Offset(size.width * .72f, size.height * .25f))
                    drawCircle(Color(0xFF496D3B).copy(alpha = .35f), radius = size.width * .55f, center = Offset(size.width * .52f, size.height * 1.12f), style = Stroke(width = 24f))
                }
                Column(Modifier.padding(24.dp)) {
                    Text("Roll & Win", color = Color(0xFF1D1D1F), fontSize = 18.sp)
                    Text("Earn points for every goal predicted", color = Color(0xFF1D1D1F), fontSize = 18.sp, lineHeight = 26.sp)
                    if (fallbackPlayers.isNotEmpty()) Text("Try with ${fallbackPlayers.first()}", color = Color(0xFF4B4B4B), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FunTile(icon: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = Color.Black, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF151515)), modifier = modifier.height(104.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, color = HomeGold, fontSize = 27.sp)
            Spacer(Modifier.height(9.dp))
            Text(label, color = Color(0xFFD9D4D5), fontSize = 16.sp)
        }
    }
}

@Composable
private fun MutedPill(text: String) {
    Surface(color = HomePanel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = HomeMuted, fontSize = 15.sp, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun HomeBottomNav(modifier: Modifier = Modifier) {
    Surface(color = Color.Black, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), border = BorderStroke(1.dp, Color(0xFF101010)), modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            BottomTab("⌂", "Home", true)
            BottomTab("⚽", "Matches", false)
            BottomTab("♕", "World Cup", false)
            BottomTab("🎮", "Games", false)
        }
    }
}

@Composable
private fun RowScope.BottomTab(icon: String, label: String, selected: Boolean) {
    val bg = if (selected) Color.White else Color.Transparent
    val fg = if (selected) Color.Black else Color(0xFFC9C4C5)
    Surface(color = bg, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(54.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, color = fg, fontSize = 19.sp)
            Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun ScheduleMatch.statusLabel(): String = statusDescription ?: status ?: when (state) {
    "pre" -> "Scheduled"
    "in" -> "Live"
    "post" -> "Final"
    else -> "Match"
}

private fun ScheduleMatch.leagueLabel(short: Boolean = false): String = when (league) {
    "fifa.world" -> if (short) "WC" else "WORLD CUP QUALIFIERS"
    "eng.1" -> if (short) "ENG" else "PREMIER LEAGUE"
    "esp.1" -> if (short) "ESP" else "LALIGA"
    "ita.1" -> if (short) "ITA" else "SERIE A"
    "ger.1" -> if (short) "GER" else "BUNDESLIGA"
    "fra.1" -> if (short) "FRA" else "LIGUE 1"
    "uefa.champions" -> if (short) "UCL" else "CHAMPIONS LEAGUE"
    "uefa.europa" -> if (short) "UEL" else "EUROPA LEAGUE"
    else -> league.uppercase()
}

private fun scoreLine(match: ScheduleMatch): String {
    val home = match.homeTeam?.score
    val away = match.awayTeam?.score
    return if (home == null || away == null) "v" else "$home - $away"
}

private fun matchMinute(match: ScheduleMatch): String =
    if (match.state == "in") match.status ?: "LIVE" else formatKickoff(match.kickoff)

private fun formatKickoff(value: String?): String {
    if (value.isNullOrBlank()) return "--:--"
    return runCatching {
        OffsetDateTime.parse(value).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(value.take(5))
}
