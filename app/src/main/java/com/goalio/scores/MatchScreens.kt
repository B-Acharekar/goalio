package com.goalio.scores

import android.widget.Toast
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.time.format.TextStyle
import java.util.Locale

private data class LeagueFilter(val code: String?, val label: String)

private val LeagueFilters = listOf(
    LeagueFilter(null, "All"),
    LeagueFilter("fifa.world", "World Cup"),
    LeagueFilter("eng.1", "Premier League"),
    LeagueFilter("esp.1", "LaLiga"),
    LeagueFilter("ita.1", "Serie A"),
    LeagueFilter("ger.1", "Bundesliga"),
    LeagueFilter("fra.1", "Ligue 1"),
    LeagueFilter("uefa.champions", "Champions League"),
    LeagueFilter("uefa.europa", "Europa League")
)

@Composable
fun MatchScreen(
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenMatch: (ScheduleMatch) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var selectedDate by rememberSaveable { mutableStateOf(today.toString()) }
    var selectedLeague by rememberSaveable { mutableStateOf<String?>(null) }
    var matches by remember { mutableStateOf(emptyList<ScheduleMatch>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val date = remember(selectedDate) { LocalDate.parse(selectedDate) }

    LaunchedEffect(selectedDate) {
        matches = MatchRepository.cachedFeed(context, selectedDate, selectedDate)
        loading = matches.isEmpty()
        while (true) {
            errorMessage = null
            runCatching { MatchRepository.refreshFeed(context, selectedDate, selectedDate) }
                .onSuccess { result ->
                    matches = result.matches
                    if (result.scoreChanged) {
                        Toast.makeText(context, "Goal update received", Toast.LENGTH_SHORT).show()
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

    val filtered = matches.filter { selectedLeague == null || it.league == selectedLeague }
    GoalioBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item { MatchTopBar(onBack) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items((-3..7).map { today.plusDays(it.toLong()) }) { day ->
                        DateChip(day, selected = day.toString() == selectedDate) {
                            selectedDate = day.toString()
                        }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(LeagueFilters) { league ->
                        LeagueChip(league.label, selected = selectedLeague == league.code) {
                            selectedLeague = league.code
                        }
                    }
                }
            }
            item {
                Text(
                    "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)} • ${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                    color = GoalioColors.TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
            }
            item {
                Crossfade(targetState = Triple(loading, errorMessage, filtered.isEmpty()), label = "matches state") { state ->
                    when {
                        state.first -> MatchStateCard("Loading real match data...")
                        state.second != null -> MatchStateCard(state.second.orEmpty())
                        state.third -> MatchStateCard("No matches found for this date and league.")
                        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            filtered.forEach { match -> MatchListCard(match, onOpenMatch) }
                        }
                    }
                }
            }
        }
        MatchBottomNav(Modifier.align(Alignment.BottomCenter), selected = "Matches", onHome = onOpenHome, onMatches = {})
    }
}

@Composable
fun MatchDetailScreen(
    league: String,
    matchId: String,
    initialMatch: ScheduleMatch?,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenMatches: () -> Unit
) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf(MatchRepository.cachedDetail(context, league, matchId)) }
    var loading by remember { mutableStateOf(detail == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf("Overview") }
    var boostUntil by remember { mutableStateOf(0L) }
    var previousScore by remember { mutableStateOf(detail?.scoreSignature() ?: initialMatch?.scoreSignature().orEmpty()) }

    LaunchedEffect(league, matchId) {
        detail = MatchRepository.cachedDetail(context, league, matchId)
        loading = detail == null
        while (true) {
            errorMessage = null
            runCatching { MatchRepository.refreshDetail(context, league, matchId) }
                .onSuccess { fresh ->
                    val newScore = fresh.scoreSignature()
                    if (previousScore.isNotBlank() && previousScore != newScore) {
                        boostUntil = System.currentTimeMillis() + 8 * 60 * 1000L
                        Toast.makeText(context, "Goal update received", Toast.LENGTH_SHORT).show()
                    }
                    previousScore = newScore
                    detail = fresh
                }
                .onFailure {
                    errorMessage = if (detail == null) {
                        it.message ?: "Could not load match detail. Check the backend connection and try again."
                    } else {
                        null
                    }
                }
            loading = false
            val isLive = detail?.statusState() == "in" || initialMatch?.state == "in"
            val delayMs = when {
                !isLive -> 15 * 60 * 1000L
                System.currentTimeMillis() < boostUntil -> 2 * 60 * 1000L
                else -> 5 * 60 * 1000L
            }
            delay(delayMs)
        }
    }

    val shown = detail
    GoalioBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { MatchTopBar(onBack, compact = true) }
            item {
                when {
                    shown != null -> DetailHeroCard(shown)
                    initialMatch != null -> ScheduleDetailHero(initialMatch)
                    loading -> MatchStateCard("Loading match detail...")
                    else -> MatchStateCard(errorMessage ?: "No detail found for this match.")
                }
            }
            if (shown != null) {
                item { DetailTabs(selectedTab) { selectedTab = it } }
                item {
                    when (selectedTab) {
                        "Timeline" -> TimelineSection(shown.events)
                        "Stats" -> StatsSection(shown)
                        "Players" -> LeadersSection(shown.playerLeaders)
                        else -> OverviewSection(shown)
                    }
                }
            }
        }
        MatchBottomNav(Modifier.align(Alignment.BottomCenter), selected = "Matches", onHome = onOpenHome, onMatches = onOpenMatches)
    }
}

@Composable
private fun MatchTopBar(onBack: () -> Unit, compact: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = GoalioColors.TextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Light, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.width(14.dp))
        Text(if (compact) "Goalio" else "GOALIO", color = GoalioColors.TextPrimary, fontSize = if (compact) 26.sp else 28.sp, fontWeight = FontWeight.Black, letterSpacing = if (compact) 3.sp else 5.sp, modifier = Modifier.weight(1f))
        Text("Search", color = GoalioColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(16.dp))
        Text("Alerts", color = GoalioColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DateChip(date: LocalDate, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) GoalioColors.TextPrimary else GoalioColors.Surface1,
        contentColor = if (selected) GoalioColors.Background else GoalioColors.TextPrimary,
        border = BorderStroke(1.dp, if (selected) GoalioColors.TextPrimary else GoalioColors.CardBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.width(106.dp).height(92.dp).clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(), fontSize = 16.sp, color = if (selected) GoalioColors.Border else GoalioColors.TextSecondary)
            Text(date.format(DateTimeFormatter.ofPattern("dd MMM")), fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(date.year.toString(), fontSize = 13.sp, color = if (selected) GoalioColors.Border else GoalioColors.TextTertiary)
        }
    }
}

@Composable
private fun LeagueChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) GoalioColors.TextPrimary else GoalioColors.Surface2,
        contentColor = if (selected) GoalioColors.Background else GoalioColors.TextPrimary,
        border = BorderStroke(1.dp, if (selected) GoalioColors.TextPrimary else GoalioColors.Border),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp))
    }
}

@Composable
private fun MatchListCard(match: ScheduleMatch, onOpenMatch: (ScheduleMatch) -> Unit) {
    Surface(
        color = GoalioColors.Surface1,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GoalioColors.CardBorder),
        modifier = Modifier.fillMaxWidth().clickable { onOpenMatch(match) }
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${match.leagueLabel()} • ${match.statusLabel()}", color = GoalioColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(match.statusPillText(), color = statusColor(match.state), fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                DetailTeam(match.homeTeam, Modifier.weight(1f))
                Text(scoreLine(match), color = GoalioColors.TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Light)
                DetailTeam(match.awayTeam, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(match.venueText().ifBlank { formatKickoff(match.kickoff) }, color = GoalioColors.TextTertiary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("›", color = GoalioColors.Accent, fontSize = 30.sp)
            }
        }
    }
}

@Composable
private fun DetailHeroCard(detail: MatchDetail) {
    Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(detail.statusPillText(), color = statusColor(detail.statusState()), fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DetailTeam(detail.homeTeam, Modifier.weight(1f))
                Text(scoreLine(detail.homeTeam, detail.awayTeam), color = GoalioColors.TextPrimary, fontSize = 48.sp, fontWeight = FontWeight.Black)
                DetailTeam(detail.awayTeam, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Text(detail.venueText().ifBlank { detail.leagueLabel() }, color = GoalioColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ScheduleDetailHero(match: ScheduleMatch) {
    Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(match.statusPillText(), color = statusColor(match.state), fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DetailTeam(match.homeTeam, Modifier.weight(1f))
                Text(scoreLine(match), color = GoalioColors.TextPrimary, fontSize = 48.sp, fontWeight = FontWeight.Black)
                DetailTeam(match.awayTeam, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailTeam(team: MatchTeamInfo?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(GoalioColors.Surface2), contentAlignment = Alignment.Center) {
            if (!team?.logo.isNullOrBlank()) {
                AsyncImage(team?.logo, contentDescription = team?.name, contentScale = ContentScale.Fit, modifier = Modifier.size(58.dp))
            } else {
                Text(team?.abbreviation ?: "TBD", color = GoalioColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(team?.shortName ?: team?.name ?: "TBD", color = GoalioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailTabs(selected: String, onSelected: (String) -> Unit) {
    val tabs = listOf("Overview", "Timeline", "Stats", "Players")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        tabs.forEach { tab ->
            Column(Modifier.clickable { onSelected(tab) }) {
                Text(tab, color = if (selected == tab) GoalioColors.TextPrimary else GoalioColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(58.dp).height(3.dp).background(if (selected == tab) GoalioColors.TextPrimary else Color.Transparent))
            }
        }
    }
}

@Composable
private fun OverviewSection(detail: MatchDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoGrid(detail)
        if (!detail.summary.isNullOrBlank()) {
            Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Match Summary", color = GoalioColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    Text(detail.summary, color = GoalioColors.TextSecondary, fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
        }
        TimelineSection(detail.events.take(4))
    }
}

@Composable
private fun InfoGrid(detail: MatchDetail) {
    val venue = detail.venueText()
    val kickoff = formatKickoff(detail.kickoff)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (venue.isNotBlank()) InfoTile("Venue", venue)
        if (kickoff != "--:--") InfoTile("Kickoff", kickoff)
        InfoTile("Competition", detail.leagueLabel())
    }
}

@Composable
private fun InfoTile(label: String, value: String) {
    Surface(color = GoalioColors.Surface2, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = GoalioColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(value, color = GoalioColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TimelineSection(events: List<MatchTimelineEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Key Moments", color = GoalioColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        if (events.isEmpty()) {
            MatchStateCard("No timeline events available for this match yet.")
        } else {
            events.forEach { event ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(24.dp).background(GoalioColors.Surface2, CircleShape), contentAlignment = Alignment.Center) {
                        Text("•", color = if (event.type?.contains("goal", true) == true) GoalioColors.Accent else GoalioColors.TextSecondary, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(listOfNotNull(event.minute, event.type).joinToString(" "), color = GoalioColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(event.text ?: event.team ?: "Match event", color = GoalioColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        event.team?.let { Text(it, color = GoalioColors.TextTertiary, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSection(detail: MatchDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Performance Matrix", color = GoalioColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        val stats = detail.teamStats.flatMap { block -> block.stats.map { block.teamId.orEmpty() to it } }
        if (stats.isEmpty()) {
            MatchStateCard("No team statistics available yet.")
        } else {
            stats.take(12).forEach { (teamId, stat) ->
                Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stat.label ?: stat.name ?: teamId, color = GoalioColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Text(stat.value ?: "-", color = GoalioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeadersSection(groups: List<MatchLeaderGroup>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Player Leaders", color = GoalioColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        if (groups.isEmpty()) {
            MatchStateCard("No player leaders available yet.")
        } else {
            groups.forEach { group ->
                Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(group.category ?: "Leaders", color = GoalioColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        group.players.forEach { player ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(player.name ?: "Player", color = GoalioColors.TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(player.mainStat ?: player.stats.firstOrNull()?.value ?: "-", color = GoalioColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchStateCard(text: String) {
    Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = GoalioColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(22.dp))
    }
}

@Composable
private fun MatchBottomNav(modifier: Modifier = Modifier, selected: String, onHome: () -> Unit, onMatches: () -> Unit) {
    Surface(color = GoalioColors.Navigation, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), border = BorderStroke(1.dp, GoalioColors.CardBorder), modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            NavTab("Home", selected == "Home", onHome)
            NavTab("Matches", selected == "Matches", onMatches)
            NavTab("World Cup", false) {}
            NavTab("Games", false) {}
        }
    }
}

@Composable
private fun RowScope.NavTab(label: String, selected: Boolean, onClick: () -> Unit) {
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

private fun ScheduleMatch.statusPillText(): String =
    if (state == "in") "● ${status ?: "LIVE"}" else statusLabel().uppercase()

private fun ScheduleMatch.leagueLabel(): String = leagueLabel(league)

private fun MatchDetail.leagueLabel(): String = leagueLabel(league)

private fun leagueLabel(league: String): String = when (league) {
    "fifa.world" -> "World Cup"
    "eng.1" -> "Premier League"
    "esp.1" -> "LaLiga"
    "ita.1" -> "Serie A"
    "ger.1" -> "Bundesliga"
    "fra.1" -> "Ligue 1"
    "uefa.champions" -> "Champions League"
    "uefa.europa" -> "Europa League"
    else -> league.uppercase()
}

private fun scoreLine(match: ScheduleMatch): String = scoreLine(match.homeTeam, match.awayTeam)

private fun scoreLine(home: MatchTeamInfo?, away: MatchTeamInfo?): String {
    val homeScore = home?.score
    val awayScore = away?.score
    return if (homeScore == null || awayScore == null) "v" else "$homeScore - $awayScore"
}

private fun statusColor(state: String?): Color = when (state) {
    "in" -> GoalioColors.Live
    "pre" -> GoalioColors.Upcoming
    "post" -> GoalioColors.Finished
    else -> GoalioColors.TextTertiary
}

private fun ScheduleMatch.venueText(): String =
    listOfNotNull(venue?.name, venue?.city).joinToString(", ")

private fun MatchDetail.venueText(): String =
    listOfNotNull(venue?.name, venue?.city).joinToString(", ")

private fun MatchDetail.statusPillText(): String =
    if (statusState() == "in") "● ${status ?: "LIVE"}" else (statusDescription ?: status ?: "Match").uppercase()

private fun MatchDetail.statusState(): String? = when {
    status?.contains("live", true) == true -> "in"
    statusDescription?.contains("live", true) == true -> "in"
    statusDescription?.contains("final", true) == true -> "post"
    statusDescription?.contains("scheduled", true) == true -> "pre"
    else -> null
}

private fun ScheduleMatch.scoreSignature(): String =
    "${homeTeam?.score}:${awayTeam?.score}"

private fun MatchDetail.scoreSignature(): String =
    "${homeTeam?.score}:${awayTeam?.score}"

private fun formatKickoff(value: String?): String {
    if (value.isNullOrBlank()) return "--:--"
    return runCatching {
        OffsetDateTime.parse(value).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(value.take(5))
}
