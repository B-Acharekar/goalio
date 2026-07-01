package com.goalio.scores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.goalio.scores.ui.theme.GoalioColors

@Composable
fun WorldCupScreen(
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenMatches: () -> Unit
) {
    val context = LocalContext.current
    val metrics = rememberGoalioMetrics()
    var data by remember { mutableStateOf(WorldCupRepository.cached(context)) }
    var selected by remember { mutableStateOf("Groups") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { WorldCupRepository.refresh(context) }
            .onSuccess {
                data = it
                error = null
            }
            .onFailure {
                if (data == null) error = it.message ?: "Could not load World Cup hub."
            }
    }

    GoalioBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(metrics.horizontalPadding, metrics.dp(18), metrics.horizontalPadding, metrics.bottomBarPadding),
            verticalArrangement = Arrangement.spacedBy(metrics.dp(22))
        ) {
            item { WorldCupTopBar(onBack) }
            when {
                data == null && error == null -> item { WorldCupState("Loading World Cup data...") }
                error != null -> item { WorldCupState(error.orEmpty()) }
                data != null -> {
                    val cup = data!!
                    item { WorldCupHero(cup) }
                    item { WorldCupTabs(selected) { selected = it } }
                    when (selected) {
                        "Bracket" -> item { WorldCupBracket(cup.bracket) }
                        "Library" -> item { WorldCupLibrary(cup) }
                        else -> {
                            item { WorldCupGroups(cup.groups) }
                            item { WorldCupBracket(cup.bracket) }
                            item { WorldCupLibrary(cup) }
                        }
                    }
                }
            }
        }
        WorldCupBottomNav(Modifier.align(Alignment.BottomCenter), onOpenHome, onOpenMatches)
    }
}

@Composable
private fun WorldCupTopBar(onBack: () -> Unit) {
    val metrics = rememberGoalioMetrics()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("<", color = Color.White, fontSize = metrics.sp(28), fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onBack))
        Spacer(Modifier.width(metrics.dp(18)))
        Text("WORLD CUP", color = Color.White, fontSize = metrics.sp(24), fontWeight = FontWeight.Black, letterSpacing = 5.sp)
        Spacer(Modifier.weight(1f))
        Text("Search", color = GoalioColors.Accent, fontSize = metrics.sp(12), fontWeight = FontWeight.Black)
        Spacer(Modifier.width(metrics.dp(14)))
        Text("Settings", color = GoalioColors.Accent, fontSize = metrics.sp(12), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WorldCupHero(cup: WorldCupBootstrapInfo) {
    val metrics = rememberGoalioMetrics()
    Surface(shape = RoundedCornerShape(metrics.dp(18)), color = GoalioColors.Surface1, modifier = Modifier.fillMaxWidth().height(metrics.dp(270))) {
        Box(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF183139), Color(0xFF0D0F10), Color(0xFF151515))))) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = .16f), radius = size.width * .28f, center = Offset(size.width * .06f, size.height * .08f))
                drawCircle(Color.White.copy(alpha = .16f), radius = size.width * .28f, center = Offset(size.width * .94f, size.height * .08f))
                drawLine(Color(0xFFFF8500), Offset(size.width * .25f, size.height * .55f), Offset(size.width * .75f, size.height * .55f), 3f, StrokeCap.Round)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(metrics.dp(30))) {
                Surface(color = Color(0xFF7A2C19), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Color(0xFFFF8500))) {
                    Text("ROAD TO 2026", color = Color.White, fontSize = metrics.sp(12), fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = metrics.dp(14), vertical = metrics.dp(7)))
                }
                Spacer(Modifier.height(metrics.dp(18)))
                Text("NORTH AMERICA 2026", color = Color.White, fontSize = metrics.sp(24), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(metrics.dp(18)))
                Row(horizontalArrangement = Arrangement.spacedBy(metrics.dp(38))) {
                    HeroMetric("${cup.tournament.daysToFinal ?: 0}", "DAYS TO FINAL")
                    HeroMetric("${cup.tournament.hostCities}", "HOST CITIES")
                }
                Spacer(Modifier.height(metrics.dp(10)))
                Text(cup.tournament.stage.uppercase(), color = GoalioColors.TextSecondary, fontSize = metrics.sp(12), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun HeroMetric(value: String, label: String) {
    val metrics = rememberGoalioMetrics()
    Column {
        Text(value, color = Color.White, fontSize = metrics.sp(38), fontWeight = FontWeight.Light)
        Text(label, color = GoalioColors.TextSecondary, fontSize = metrics.sp(12), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WorldCupTabs(selected: String, onSelected: (String) -> Unit) {
    val metrics = rememberGoalioMetrics()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.dp(10))) {
        items(listOf("Groups", "Bracket", "Library")) { tab ->
            Surface(
                color = if (selected == tab) GoalioColors.Accent else Color.Transparent,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, GoalioColors.Accent),
                modifier = Modifier.clickable { onSelected(tab) }
            ) {
                Text(tab.uppercase(), color = Color.White, fontSize = metrics.sp(12), fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = metrics.dp(16), vertical = metrics.dp(10)))
            }
        }
    }
}

@Composable
private fun WorldCupGroups(groups: List<WorldCupGroupInfo>) {
    val metrics = rememberGoalioMetrics()
    Column(verticalArrangement = Arrangement.spacedBy(metrics.dp(14))) {
        SectionTitle("Groups")
        groups.forEach { group -> WorldCupGroupTable(group) }
    }
}

@Composable
private fun WorldCupGroupTable(group: WorldCupGroupInfo) {
    val metrics = rememberGoalioMetrics()
    Surface(
        color = Color(0xFF202226),
        shape = RoundedCornerShape(metrics.dp(10)),
        border = BorderStroke(1.dp, Color(0xFF3A3D42)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                "Group ${group.code}", color = Color.White, fontSize = metrics.sp(18), fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = metrics.dp(12), vertical = metrics.dp(12))
            )
            GroupTableHeader()
            group.teams.forEachIndexed { index, team ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF45484E)))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = metrics.dp(8), vertical = metrics.dp(10)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text((team.rank ?: index + 1).toString(), color = GoalioColors.TextPrimary, fontSize = metrics.sp(11), modifier = Modifier.width(metrics.dp(18)))
                    if (!team.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = team.logo,
                            contentDescription = "${team.name} flag",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(metrics.dp(20))
                        )
                    } else {
                        Text(countryFlag(team.name), fontSize = metrics.sp(15), modifier = Modifier.width(metrics.dp(20)))
                    }
                    Spacer(Modifier.width(metrics.dp(7)))
                    Text(team.name, color = Color.White, fontSize = metrics.sp(12), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    GroupStat(team.played)
                    GroupStat(team.wins)
                    GroupStat(team.draws)
                    GroupStat(team.losses)
                    GroupStat(team.goalsFor)
                    GroupStat(team.goalsAgainst)
                    GroupStat(team.goalDifference, signed = true)
                    GroupStat(team.points, bold = true)
                }
            }
        }
    }
}

@Composable
private fun GroupTableHeader() {
    val metrics = rememberGoalioMetrics()
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF272A2F)).padding(horizontal = metrics.dp(8), vertical = metrics.dp(7)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TEAM", color = GoalioColors.TextSecondary, fontSize = metrics.sp(9), modifier = Modifier.width(metrics.dp(45)).weight(1f))
        listOf("MP", "W", "D", "L", "GF", "GA", "GD", "PTS").forEach { label ->
            Text(label, color = GoalioColors.TextSecondary, fontSize = metrics.sp(8), fontWeight = FontWeight.Bold, modifier = Modifier.width(metrics.dp(24)))
        }
    }
}

@Composable
private fun GroupStat(value: Int?, signed: Boolean = false, bold: Boolean = false) {
    val metrics = rememberGoalioMetrics()
    val text = when {
        value == null -> "-"
        signed && value > 0 -> "+$value"
        else -> value.toString()
    }
    Text(text, color = Color.White, fontSize = metrics.sp(10), fontWeight = if (bold) FontWeight.Black else FontWeight.Medium, modifier = Modifier.width(metrics.dp(24)), maxLines = 1)
}

@Composable
private fun WorldCupBracket(rounds: List<WorldCupBracketRoundInfo>) {
    val metrics = rememberGoalioMetrics()
    val visibleRounds = rounds.filter { it.matches.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(metrics.dp(14))) {
        SectionTitle("The Knockout Path")
        if (visibleRounds.isEmpty()) {
            WorldCupState("Bracket data is loading from World Cup feed.")
        } else {
            Surface(color = Color(0xFF050505), shape = RoundedCornerShape(metrics.dp(14)), border = BorderStroke(1.dp, Color(0xFF2B2B2B)), modifier = Modifier.fillMaxWidth()) {
                ConnectedBracket(visibleRounds)
            }
        }
    }
}

@Composable
private fun ConnectedBracket(rounds: List<WorldCupBracketRoundInfo>) {
    val metrics = rememberGoalioMetrics()
    val cardWidth = 188f * metrics.scale
    val cardHeight = 78f * metrics.scale
    val columnGap = 52f * metrics.scale
    val rowGap = 16f * metrics.scale
    val headerHeight = 38f * metrics.scale
    val firstStep = cardHeight + rowGap
    val layouts = remember(rounds, metrics.scale) {
        val result = mutableListOf<Pair<WorldCupBracketRoundInfo, List<Float>>>()
        var previous = rounds.first().matches.indices.map { headerHeight + cardHeight / 2f + it * firstStep }
        result += rounds.first() to previous
        rounds.drop(1).forEach { round ->
            val centers = round.matches.indices.map { index ->
                val top = previous.getOrNull(index * 2)
                val bottom = previous.getOrNull(index * 2 + 1)
                when {
                    top != null && bottom != null -> (top + bottom) / 2f
                    top != null -> top
                    else -> headerHeight + cardHeight / 2f + index * firstStep
                }
            }
            result += round to centers
            previous = centers
        }
        result
    }
    val contentWidth = rounds.size * cardWidth + (rounds.size - 1).coerceAtLeast(0) * columnGap + 28f * metrics.scale
    val lastCenter = layouts.flatMap { it.second }.maxOrNull() ?: 0f
    val contentHeight = kotlin.math.max(340f * metrics.scale, lastCenter + cardHeight / 2f + 20f * metrics.scale)

    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Box(Modifier.width(contentWidth.dp).height(contentHeight.dp).padding(horizontal = metrics.dp(14))) {
            Canvas(Modifier.fillMaxSize()) {
                val connector = Color(0xFFFF8500).copy(alpha = .72f)
                val widthPx = cardWidth.dp.toPx()
                val gapPx = columnGap.dp.toPx()
                layouts.dropLast(1).forEachIndexed { roundIndex, (_, childCenters) ->
                    val parentCenters = layouts[roundIndex + 1].second
                    val startX = roundIndex * (widthPx + gapPx) + widthPx
                    val middleX = startX + gapPx / 2f
                    val endX = (roundIndex + 1) * (widthPx + gapPx)
                    parentCenters.forEachIndexed { parentIndex, parentCenter ->
                        val children = listOfNotNull(childCenters.getOrNull(parentIndex * 2), childCenters.getOrNull(parentIndex * 2 + 1))
                        if (children.isEmpty()) return@forEachIndexed
                        children.forEach { child ->
                            drawLine(connector, Offset(startX, child.dp.toPx()), Offset(middleX, child.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                        }
                        drawLine(connector, Offset(middleX, children.min().dp.toPx()), Offset(middleX, children.max().dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                        drawLine(connector, Offset(middleX, parentCenter.dp.toPx()), Offset(endX, parentCenter.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            layouts.forEachIndexed { roundIndex, (round, centers) ->
                val x = roundIndex * (cardWidth + columnGap)
                Text(
                    round.round.uppercase(), color = GoalioColors.TextSecondary, fontSize = metrics.sp(11), fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(x.dp, 0.dp).width(cardWidth.dp), maxLines = 1
                )
                round.matches.forEachIndexed { matchIndex, match ->
                    BracketMatchBox(
                        match = match,
                        modifier = Modifier.offset(x.dp, (centers[matchIndex] - cardHeight / 2f).dp).width(cardWidth.dp).height(cardHeight.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BracketMatchBox(match: WorldCupBracketMatchInfo, modifier: Modifier = Modifier) {
    val metrics = rememberGoalioMetrics()
    Column(modifier) {
        Text("MATCH", color = GoalioColors.Accent, fontSize = metrics.sp(9), fontWeight = FontWeight.Black)
        Surface(color = Color(0xFF141414), shape = RoundedCornerShape(metrics.dp(7)), border = BorderStroke(1.dp, Color(0xFF454545)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = metrics.dp(9), vertical = metrics.dp(6)), verticalArrangement = Arrangement.spacedBy(metrics.dp(5))) {
                BracketTeamRow(match.homeTeam.orEmpty(), match.homeScore, match.homeTeamLogo)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF303030)))
                BracketTeamRow(match.awayTeam.orEmpty(), match.awayScore, match.awayTeamLogo)
            }
        }
    }
}

@Composable
private fun BracketTeamRow(name: String, score: Int?, logo: String?) {
    val metrics = rememberGoalioMetrics()
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!logo.isNullOrBlank()) {
            AsyncImage(model = logo, contentDescription = "$name flag", contentScale = ContentScale.Fit, modifier = Modifier.size(metrics.dp(17)))
        } else {
            Text(countryFlag(name), fontSize = metrics.sp(13), modifier = Modifier.width(metrics.dp(18)))
        }
        Spacer(Modifier.width(metrics.dp(8)))
        Text(name, color = Color.White, fontSize = metrics.sp(12), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (score != null) Text(score.toString(), color = GoalioColors.TextPrimary, fontSize = metrics.sp(13), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WorldCupMatches(cup: WorldCupBootstrapInfo, onOpenMatches: () -> Unit) {
    val metrics = rememberGoalioMetrics()
    val matches = (cup.liveMatches + cup.todayMatches + cup.upcomingMatches + cup.recentResults).distinctBy { it.matchId }
    Column(verticalArrangement = Arrangement.spacedBy(metrics.dp(12))) {
        SectionTitle("World Cup Matches")
        if (matches.isEmpty()) WorldCupState("No World Cup fixtures available yet.")
        matches.take(8).forEach { match ->
            Surface(color = GoalioColors.Surface2, shape = RoundedCornerShape(metrics.dp(10)), modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMatches)) {
                Row(Modifier.padding(metrics.dp(15)), verticalAlignment = Alignment.CenterVertically) {
                    Text(match.statusDescription ?: match.status ?: "Match", color = GoalioColors.Accent, fontSize = metrics.sp(12), fontWeight = FontWeight.Black, modifier = Modifier.width(metrics.dp(86)))
                    Text("${match.homeTeam?.shortName ?: "TBD"} vs ${match.awayTeam?.shortName ?: "TBD"}", color = Color.White, fontSize = metrics.sp(15), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(worldCupScoreLine(match), color = GoalioColors.TextSecondary, fontSize = metrics.sp(14), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun WorldCupLibrary(cup: WorldCupBootstrapInfo) {
    val metrics = rememberGoalioMetrics()
    Column(verticalArrangement = Arrangement.spacedBy(metrics.dp(14))) {
        SectionTitle("World Cup Library")
        Surface(color = Color(0xFF171717), shape = RoundedCornerShape(metrics.dp(14)), border = BorderStroke(1.dp, Color(0xFF2A2A2A)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(metrics.dp(18))) {
                Text(cup.randomFact.title, color = GoalioColors.Accent, fontSize = metrics.sp(12), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(metrics.dp(8)))
                Text(cup.randomFact.body, color = Color.White, fontSize = metrics.sp(17), fontWeight = FontWeight.Bold)
            }
        }
        cup.library.forEach { item ->
            Surface(color = GoalioColors.Surface2, shape = RoundedCornerShape(metrics.dp(14)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(metrics.dp(18))) {
                    Text(item.category.uppercase(), color = GoalioColors.Accent, fontSize = metrics.sp(11), fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(metrics.dp(8)))
                    Text(item.title, color = Color.White, fontSize = metrics.sp(18), fontWeight = FontWeight.Black)
                    Text(item.body, color = GoalioColors.TextSecondary, fontSize = metrics.sp(14), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(metrics.dp(10)))
                    Text("${item.readMinutes} min read", color = GoalioColors.TextSecondary, fontSize = metrics.sp(12), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val metrics = rememberGoalioMetrics()
    Text(text, color = Color.White, fontSize = metrics.sp(22), fontWeight = FontWeight.Black)
}

@Composable
private fun WorldCupState(text: String) {
    val metrics = rememberGoalioMetrics()
    Surface(color = GoalioColors.Surface1, shape = RoundedCornerShape(metrics.dp(14)), border = BorderStroke(1.dp, GoalioColors.CardBorder), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = GoalioColors.TextSecondary, fontSize = metrics.sp(15), fontWeight = FontWeight.Bold, modifier = Modifier.padding(metrics.dp(18)))
    }
}

@Composable
private fun WorldCupBottomNav(modifier: Modifier = Modifier, onOpenHome: () -> Unit, onOpenMatches: () -> Unit) {
    val metrics = rememberGoalioMetrics()
    Surface(color = Color(0xFF3B3B3B), shape = RoundedCornerShape(metrics.dp(28)), modifier = modifier.fillMaxWidth().padding(horizontal = metrics.dp(8), vertical = metrics.dp(10))) {
        Row(Modifier.padding(metrics.dp(8)), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            WorldCupNavTab("Home", false, onOpenHome)
            WorldCupNavTab("Matches", false, onOpenMatches)
            WorldCupNavTab("World Cup", true) {}
            WorldCupNavTab("Games", false) {}
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.WorldCupNavTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val metrics = rememberGoalioMetrics()
    Surface(color = if (selected) GoalioColors.Accent else Color.Transparent, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(metrics.dp(56)).clickable(onClick = onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = if (selected) Color.White else Color(0xFFBEB8AA), fontSize = metrics.sp(12), fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

private fun worldCupScoreLine(match: ScheduleMatch): String {
    val home = match.homeTeam?.score
    val away = match.awayTeam?.score
    return if (home == null || away == null) "-" else "$home - $away"
}

private val CountryCodes = mapOf(
    "Algeria" to "DZ", "Argentina" to "AR", "Australia" to "AU", "Austria" to "AT",
    "Belgium" to "BE", "Bosnia & Herzegovina" to "BA", "Brazil" to "BR", "Cabo Verde" to "CV",
    "Canada" to "CA", "Colombia" to "CO", "Congo DR" to "CD", "Croatia" to "HR",
    "Czechia" to "CZ", "Ecuador" to "EC", "Egypt" to "EG", "England" to "GB",
    "France" to "FR", "Germany" to "DE", "Ghana" to "GH", "Ivory Coast" to "CI",
    "Japan" to "JP", "Mexico" to "MX", "Morocco" to "MA", "Netherlands" to "NL",
    "Nigeria" to "NG", "Norway" to "NO", "Paraguay" to "PY", "Portugal" to "PT",
    "Senegal" to "SN", "South Africa" to "ZA", "South Korea" to "KR", "Spain" to "ES",
    "Sweden" to "SE", "Switzerland" to "CH", "United States" to "US", "USA" to "US"
)

private fun countryFlag(team: String): String {
    val code = CountryCodes[team] ?: return ""
    return code.map { letter -> String(Character.toChars(0x1F1E6 + (letter - 'A'))) }.joinToString("")
}
