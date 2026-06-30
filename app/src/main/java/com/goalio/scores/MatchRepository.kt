package com.goalio.scores

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class MatchFeedResult(val matches: List<ScheduleMatch>, val scoreChanged: Boolean)

object MatchRepository {
    val leagues = listOf(
        "fifa.world",
        "eng.1",
        "esp.1",
        "ita.1",
        "ger.1",
        "fra.1",
        "uefa.champions",
        "uefa.europa"
    )

    private const val PREFS = "goalio_match_cache"
    private const val BOOST_UNTIL = "live_boost_until"

    fun cachedFeed(context: Context, from: String, to: String): List<ScheduleMatch> =
        context.cachePrefs().getString(feedKey(from, to), null)
            ?.let { runCatching { JSONArray(it).toScheduleMatches() }.getOrDefault(emptyList()) }
            .orEmpty()

    suspend fun refreshFeed(context: Context, from: String, to: String): MatchFeedResult {
        val before = cachedFeed(context, from, to).scoreSignature()
        val matches = coroutineScope {
            leagues.map { league ->
                async {
                    runCatching { GoalioBackendApi.getScheduleRange(league, from, to).matches }
                        .getOrDefault(emptyList())
                }
            }.flatMap { it.await() }
        }.distinctBy { "${it.league}:${it.matchId}" }
            .sortedWith(compareBy<ScheduleMatch> { stateRank(it.state) }.thenBy { it.kickoff.orEmpty() })
        val changed = before.isNotBlank() && before != matches.scoreSignature()
        withContext(Dispatchers.IO) {
            context.cachePrefs().edit()
                .putString(feedKey(from, to), JSONArray(matches.map { it.toJson() }).toString())
                .apply()
            if (changed) markGoalBoost(context)
        }
        return MatchFeedResult(matches, changed)
    }

    fun cachedDetail(context: Context, league: String, matchId: String): MatchDetail? =
        context.cachePrefs().getString(detailKey(league, matchId), null)
            ?.let { runCatching { JSONObject(it).toMatchDetail() }.getOrNull() }

    suspend fun refreshDetail(context: Context, league: String, matchId: String): MatchDetail {
        val detail = GoalioBackendApi.getMatchDetail(league, matchId)
        withContext(Dispatchers.IO) {
            context.cachePrefs().edit()
                .putString(detailKey(league, matchId), detail.toJson().toString())
                .apply()
        }
        return detail
    }

    fun nextRefreshDelayMillis(context: Context, matches: List<ScheduleMatch>): Long {
        val hasLive = matches.any { it.state == "in" }
        if (!hasLive) return 15 * 60 * 1000L
        return if (System.currentTimeMillis() < context.cachePrefs().getLong(BOOST_UNTIL, 0L)) {
            2 * 60 * 1000L
        } else {
            5 * 60 * 1000L
        }
    }

    private fun markGoalBoost(context: Context) {
        context.cachePrefs().edit()
            .putLong(BOOST_UNTIL, System.currentTimeMillis() + 8 * 60 * 1000L)
            .apply()
    }

    private fun Context.cachePrefs() = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun feedKey(from: String, to: String) = "feed_${from}_$to"

    private fun detailKey(league: String, matchId: String) = "detail_${league}_$matchId"
}

fun stateRank(state: String?): Int = when (state) {
    "in" -> 0
    "pre" -> 1
    "post" -> 2
    else -> 3
}

private fun List<ScheduleMatch>.scoreSignature(): String = filter { it.state == "in" }
    .joinToString("|") { "${it.league}:${it.matchId}:${it.homeTeam?.score}:${it.awayTeam?.score}" }

private fun ScheduleMatch.toJson() = JSONObject().apply {
    put("matchId", matchId)
    put("league", league)
    putNullable("name", name)
    putNullable("shortName", shortName)
    putNullable("status", status)
    putNullable("statusDescription", statusDescription)
    putNullable("state", state)
    putNullable("kickoff", kickoff)
    putNullable("homeTeam", homeTeam?.toJson())
    putNullable("awayTeam", awayTeam?.toJson())
    putNullable("venue", venue?.toJson())
    put("detailApi", detailApi)
}

private fun MatchTeamInfo.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    putNullable("shortName", shortName)
    putNullable("abbreviation", abbreviation)
    putNullable("logo", logo)
    putNullable("score", score)
}

private fun MatchVenueInfo.toJson() = JSONObject().apply {
    putNullable("name", name)
    putNullable("city", city)
}

private fun MatchDetail.toJson() = JSONObject().apply {
    put("matchId", matchId)
    put("league", league)
    putNullable("status", status)
    putNullable("statusDescription", statusDescription)
    putNullable("kickoff", kickoff)
    putNullable("homeTeam", homeTeam?.toJson())
    putNullable("awayTeam", awayTeam?.toJson())
    putNullable("venue", venue?.toJson())
    put("teamStats", JSONArray(teamStats.map { it.toJson() }))
    put("playerLeaders", JSONArray(playerLeaders.map { it.toJson() }))
    put("events", JSONArray(events.map { it.toJson() }))
    putNullable("summary", summary)
}

private fun TeamStatsBlock.toJson() = JSONObject().apply {
    putNullable("teamId", teamId)
    put("stats", JSONArray(stats.map { it.toJson() }))
}

private fun MatchStat.toJson() = JSONObject().apply {
    putNullable("name", name)
    putNullable("label", label)
    putNullable("value", value)
}

private fun MatchLeaderGroup.toJson() = JSONObject().apply {
    putNullable("category", category)
    put("players", JSONArray(players.map { it.toJson() }))
}

private fun MatchLeaderPlayer.toJson() = JSONObject().apply {
    putNullable("id", id)
    putNullable("name", name)
    putNullable("position", position)
    putNullable("jersey", jersey)
    putNullable("espnUrl", espnUrl)
    putNullable("mainStat", mainStat)
    put("stats", JSONArray(stats.map { it.toJson() }))
}

private fun MatchTimelineEvent.toJson() = JSONObject().apply {
    putNullable("minute", minute)
    putNullable("type", type)
    putNullable("text", text)
    putNullable("team", team)
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun JSONArray.toScheduleMatches() = buildList {
    for (index in 0 until length()) getJSONObject(index).run {
        add(ScheduleMatch(
            matchId = getString("matchId"),
            league = getString("league"),
            name = nullableString("name"),
            shortName = nullableString("shortName"),
            status = nullableString("status"),
            statusDescription = nullableString("statusDescription"),
            state = nullableString("state"),
            kickoff = nullableString("kickoff"),
            homeTeam = optJSONObject("homeTeam")?.toMatchTeamInfo(),
            awayTeam = optJSONObject("awayTeam")?.toMatchTeamInfo(),
            venue = optJSONObject("venue")?.toMatchVenueInfo(),
            detailApi = getString("detailApi")
        ))
    }
}

private fun JSONObject.toMatchDetail() = MatchDetail(
    matchId = getString("matchId"),
    league = getString("league"),
    status = nullableString("status"),
    statusDescription = nullableString("statusDescription"),
    kickoff = nullableString("kickoff"),
    homeTeam = optJSONObject("homeTeam")?.toMatchTeamInfo(),
    awayTeam = optJSONObject("awayTeam")?.toMatchTeamInfo(),
    venue = optJSONObject("venue")?.toMatchVenueInfo(),
    teamStats = optJSONArray("teamStats").toTeamStatsBlocks(),
    playerLeaders = optJSONArray("playerLeaders").toLeaderGroups(),
    events = optJSONArray("events").toTimelineEvents(),
    summary = nullableString("summary")
)

private fun JSONObject.toMatchTeamInfo() = MatchTeamInfo(
    id = getString("id"),
    name = getString("name"),
    shortName = nullableString("shortName"),
    abbreviation = nullableString("abbreviation"),
    logo = nullableString("logo"),
    score = if (isNull("score")) null else optInt("score")
)

private fun JSONObject.toMatchVenueInfo() = MatchVenueInfo(
    name = nullableString("name"),
    city = nullableString("city")
)

private fun JSONArray?.toTeamStatsBlocks(): List<TeamStatsBlock> = buildList {
    if (this@toTeamStatsBlocks != null) for (index in 0 until length()) getJSONObject(index).run {
        add(TeamStatsBlock(nullableString("teamId"), optJSONArray("stats").toMatchStats()))
    }
}

private fun JSONArray?.toMatchStats(): List<MatchStat> = buildList {
    if (this@toMatchStats != null) for (index in 0 until length()) getJSONObject(index).run {
        add(MatchStat(nullableString("name"), nullableString("label"), nullableString("value")))
    }
}

private fun JSONArray?.toLeaderGroups(): List<MatchLeaderGroup> = buildList {
    if (this@toLeaderGroups != null) for (index in 0 until length()) getJSONObject(index).run {
        add(MatchLeaderGroup(nullableString("category"), optJSONArray("players").toLeaderPlayers()))
    }
}

private fun JSONArray?.toLeaderPlayers(): List<MatchLeaderPlayer> = buildList {
    if (this@toLeaderPlayers != null) for (index in 0 until length()) getJSONObject(index).run {
        add(MatchLeaderPlayer(
            id = nullableString("id"),
            name = nullableString("name"),
            position = nullableString("position"),
            jersey = nullableString("jersey"),
            espnUrl = nullableString("espnUrl"),
            mainStat = nullableString("mainStat"),
            stats = optJSONArray("stats").toMatchStats()
        ))
    }
}

private fun JSONArray?.toTimelineEvents(): List<MatchTimelineEvent> = buildList {
    if (this@toTimelineEvents != null) for (index in 0 until length()) getJSONObject(index).run {
        add(MatchTimelineEvent(
            minute = nullableString("minute"),
            type = nullableString("type"),
            text = nullableString("text"),
            team = nullableString("team")
        ))
    }
}

private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key).ifBlank { null }
