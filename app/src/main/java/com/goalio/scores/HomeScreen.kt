package com.goalio.scores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.goalio.scores.ui.theme.GoalioColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
    fallbackPlayers: Set<String>,
    onOpenMatches: () -> Unit,
    onOpenMatch: (ScheduleMatch) -> Unit
) {
    val context = LocalContext.current
    var matches by remember { mutableStateOf(emptyList<ScheduleMatch>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val today = remember { LocalDate.now() }
    val fromDate = remember(today) { today.minusDays(1).toString() }
    val toDate = remember(today) { today.plusDays(7).toString() }

    LaunchedEffect(fromDate, toDate) {
        matches = MatchRepository.cachedFeed(context, fromDate, toDate)
        loading = matches.isEmpty()
        while (true) {
            errorMessage = null
            runCatching { MatchRepository.refreshFeed(context, fromDate, toDate) }
                .onSuccess { result ->
                    matches = result.matches
                    if (result.scoreChanged) {
                        android.widget.Toast.makeText(context, "Live score updated", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure {
                    errorMessage = if (matches.isEmpty()) {
                        it.message ?: "Could not load matches. Check the backend connection and try again."
                    } else {
                        null
                    }
                }
            loading = false
            delay(MatchRepository.nextRefreshDelayMillis(context, matches))
        }
    }

    val liveMatches = matches.filter { it.state == "in" }
    val upcoming = matches.filter { it.state == "pre" }.take(12)
    val finished = matches.filter { it.state == "post" }.take(8)
    val featured = liveMatches.firstOrNull() ?: upcoming.firstOrNull() ?: finished.firstOrNull()

    GoalioBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item { HomeTopBar() }
            item {
                when {
                    loading -> HomeStateCard("Loading real match data...")
                    errorMessage != null -> HomeStateCard(errorMessage.orEmpty(), action = "Retry")
                    featured != null -> FeaturedMatchCard(featured, onOpenMatch)
                    else -> HomeStateCard("No matches found from $fromDate to $toDate.")
                }
            }
            if (!loading && errorMessage == null && matches.isNotEmpty()) {
                item {
                    SectionHeader("Live Action", "${liveMatches.size} live", onOpenMatches)
                    Spacer(Modifier.height(12.dp))
                    if (liveMatches.isEmpty()) {
                        MutedPill("No live matches right now")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(liveMatches.take(6), key = { "${it.league}:${it.matchId}" }) {
                                MatchMiniCard(it, onOpenMatch)
                            }
                        }
                    }
                }
                item {
                    SectionHeader("Upcoming", "${upcoming.size} next", onOpenMatches)
                    Spacer(Modifier.height(12.dp))
                    if (upcoming.isEmpty()) {
                        MutedPill("No upcoming fixtures in this window")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            upcoming.take(6).forEach { ScheduleRow(it, onOpenMatch) }
                        }
                    }
                }
                item {
                    SectionHeader("Finished", "${finished.size} recent", onOpenMatches)
                    Spacer(Modifier.height(12.dp))
                    if (finished.isEmpty()) {
                        MutedPill("No finished matches in this window")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            finished.take(4).forEach { ScheduleRow(it, onOpenMatch) }
                        }
                    }
                }
            }
        }
        HomeBottomNav(Modifier.align(Alignment.BottomCenter), onOpenMatches)
    }
}

@Composable
private fun HomeTopBar() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("GOALIO", color = GoalioColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, modifier = Modifier.weight(1f))
        Text("Search", color = GoalioColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(14.dp))
        Text("Alerts", color = GoalioColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeaturedMatchCard(match: ScheduleMatch, onOpenMatch: (ScheduleMatch) -> Unit) {
    Surface(
        color = GoalioColors.Surface1,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GoalioColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MatchStatusPill(match)
                Spacer(Modifier.weight(1f))
                Text(match.leagueLabel(), color = GoalioColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TeamBadge(match.homeTeam, Modifier.weight(1f))
                Text(scoreLine(match), color = GoalioColors.TextPrimary, fontSize = 44.sp, fontWeight = FontWeight.Black)
                TeamBadge(match.awayTeam, Modifier.weight(1f))
            }
            match.venue?.let { venue ->
                val venueText = listOfNotNull(venue.name, venue.city).joinToString(", ")
                if (venueText.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(venueText, color = GoalioColors.TextTertiary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { onOpenMatch(match) },
                colors = ButtonDefaults.buttonColors(containerColor = GoalioColors.Accent, contentColor = GoalioColors.TextPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Match Details", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun TeamBadge(team: MatchTeamInfo?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(GoalioColors.Surface2), contentAlignment = Alignment.Center) {
            if (!team?.logo.isNullOrBlank()) {
                AsyncImage(team?.logo, contentDescription = team?.name, contentScale = ContentScale.Fit, modifier = Modifier.size(54.dp))
            } else {
                Text(team?.abbreviation ?: "TBD", color = GoalioColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            team?.abbreviation ?: team?.shortName ?: team?.name ?: "TBD",
            color = GoalioColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = GoalioColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text(action, color = GoalioColors.TextTertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable
private fun MatchMiniCard(match: ScheduleMatch, onOpenMatch: (ScheduleMatch) -> Unit) {
    Surface(
        color = GoalioColors.Surface1,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GoalioColors.CardBorder),
        modifier = Modifier.width(230.dp).height(136.dp).clickable { onOpenMatch(match) }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MatchStatusPill(match)
                Spacer(Modifier.weight(1f))
                Text(match.leagueLabel(short = true), color = GoalioColors.TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            TeamScoreLine(match.homeTeam)
            TeamScoreLine(match.awayTeam)
        }
    }
}

@Composable
private fun ScheduleRow(match: ScheduleMatch, onOpenMatch: (ScheduleMatch) -> Unit) {
    Surface(
        color = GoalioColors.Surface1,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoalioColors.CardBorder),
        modifier = Modifier.fillMaxWidth().clickable { onOpenMatch(match) }
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(78.dp)) {
                Text(formatKickoff(match.kickoff), color = GoalioColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(match.leagueLabel(short = true), color = GoalioColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f)) {
                TeamScoreLine(match.homeTeam)
                Spacer(Modifier.height(6.dp))
                TeamScoreLine(match.awayTeam)
            }
            Text(match.statusLabel(), color = statusColor(match.state), fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun TeamScoreLine(team: MatchTeamInfo?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            team?.name ?: "TBD",
            color = GoalioColors.TextSecondary,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        team?.score?.let {
            Text(it.toString(), color = GoalioColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MatchStatusPill(match: ScheduleMatch) {
    val color = statusColor(match.state)
    Surface(color = GoalioColors.Surface2, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, color)) {
        Text(
            match.statusLabel().uppercase(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HomeStateCard(text: String, action: String? = null) {
    Surface(
        color = GoalioColors.Surface1,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GoalioColors.CardBorder),
        modifier = Modifier.fillMaxWidth().height(220.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text, color = GoalioColors.TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (action != null) {
                Spacer(Modifier.height(14.dp))
                Text(action, color = GoalioColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MutedPill(text: String) {
    Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = GoalioColors.TextTertiary, fontSize = 15.sp, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun HomeBottomNav(modifier: Modifier = Modifier, onOpenMatches: () -> Unit) {
    Surface(
        color = GoalioColors.Navigation,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, GoalioColors.CardBorder),
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            BottomTab("Home", true) {}
            BottomTab("Matches", false, onOpenMatches)
            BottomTab("World Cup", false)
            BottomTab("Games", false)
        }
    }
}

@Composable
private fun RowScope.BottomTab(label: String, selected: Boolean, onClick: () -> Unit = {}) {
    val fg = if (selected) GoalioColors.TextPrimary else GoalioColors.InactiveIcon
    Surface(color = Color.Transparent, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp).clickable(onClick = onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(24.dp).height(3.dp).background(if (selected) GoalioColors.Accent else Color.Transparent, RoundedCornerShape(50)))
        }
    }
}

private fun ScheduleMatch.statusLabel(): String = when (state) {
    "pre" -> statusDescription ?: status ?: "Upcoming"
    "in" -> statusDescription ?: status ?: "Live"
    "post" -> statusDescription ?: status ?: "Finished"
    else -> statusDescription ?: status ?: "Match"
}

private fun ScheduleMatch.leagueLabel(short: Boolean = false): String = when (league) {
    "fifa.world" -> if (short) "WC" else "WORLD CUP"
    "eng.1" -> if (short) "EPL" else "PREMIER LEAGUE"
    "esp.1" -> if (short) "LALIGA" else "LALIGA"
    "ita.1" -> if (short) "SERIE A" else "SERIE A"
    "ger.1" -> if (short) "BUND" else "BUNDESLIGA"
    "fra.1" -> if (short) "L1" else "LIGUE 1"
    "uefa.champions" -> if (short) "UCL" else "CHAMPIONS LEAGUE"
    "uefa.europa" -> if (short) "UEL" else "EUROPA LEAGUE"
    else -> league.uppercase()
}

private fun scoreLine(match: ScheduleMatch): String {
    val home = match.homeTeam?.score
    val away = match.awayTeam?.score
    return if (home == null || away == null) "v" else "$home - $away"
}

private fun formatKickoff(value: String?): String {
    if (value.isNullOrBlank()) return "--:--"
    return runCatching {
        OffsetDateTime.parse(value).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(value.take(5))
}

private fun statusColor(state: String?): Color = when (state) {
    "in" -> GoalioColors.Live
    "pre" -> GoalioColors.Upcoming
    "post" -> GoalioColors.Finished
    else -> GoalioColors.TextTertiary
}
