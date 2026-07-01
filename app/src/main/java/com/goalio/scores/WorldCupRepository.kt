package com.goalio.scores

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object WorldCupRepository {
    private const val PREFS = "goalio_world_cup_cache"
    private const val BOOTSTRAP = "bootstrap"

    fun cached(context: Context): WorldCupBootstrapInfo? =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(BOOTSTRAP, null)
            ?.let { runCatching { JSONObject(it).toWorldCupBootstrapInfo() }.getOrNull() }

    suspend fun refresh(context: Context): WorldCupBootstrapInfo {
        val fresh = GoalioBackendApi.getWorldCupBootstrap()
        withContext(Dispatchers.IO) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(BOOTSTRAP, fresh.toJson().toString())
                .apply()
        }
        return fresh
    }
}

private fun WorldCupBootstrapInfo.toJson() = JSONObject().apply {
    put("tournament", JSONObject().apply {
        put("id", tournament.id)
        put("name", tournament.name)
        put("stage", tournament.stage)
        put("hostCities", tournament.hostCities)
        putNullable("daysToFinal", tournament.daysToFinal)
    })
    put("liveMatches", JSONArray(liveMatches.map { it.toWorldCupJson() }))
    put("todayMatches", JSONArray(todayMatches.map { it.toWorldCupJson() }))
    put("upcomingMatches", JSONArray(upcomingMatches.map { it.toWorldCupJson() }))
    put("recentResults", JSONArray(recentResults.map { it.toWorldCupJson() }))
    put("groups", JSONArray(groups.map { group -> JSONObject().apply {
        put("code", group.code)
        put("teams", JSONArray(group.teams.map { it.toJson() }))
    } }))
    put("bracket", JSONArray(bracket.map { round -> JSONObject().apply {
        put("round", round.round)
        put("matches", JSONArray(round.matches.map { it.toJson() }))
    } }))
    put("library", JSONArray(library.map { item -> JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("category", item.category)
        put("body", item.body)
        put("readMinutes", item.readMinutes)
    } }))
    put("randomFact", JSONObject().apply {
        put("title", randomFact.title)
        put("body", randomFact.body)
    })
}

private fun ScheduleMatch.toWorldCupJson() = JSONObject().apply {
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
    putNullable("venue", venue?.let { JSONObject().apply {
        putNullable("name", it.name)
        putNullable("city", it.city)
    } })
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

private fun StandingTeamInfo.toJson() = JSONObject().apply {
    putNullable("rank", rank)
    put("teamId", teamId)
    put("name", name)
    putNullable("abbreviation", abbreviation)
    putNullable("logo", logo)
    putNullable("group", group)
    putNullable("stage", stage)
    putNullable("played", played)
    putNullable("wins", wins)
    putNullable("draws", draws)
    putNullable("losses", losses)
    putNullable("goalsFor", goalsFor)
    putNullable("goalsAgainst", goalsAgainst)
    putNullable("goalDifference", goalDifference)
    putNullable("points", points)
}

private fun WorldCupBracketMatchInfo.toJson() = JSONObject().apply {
    put("eventId", eventId)
    put("round", round)
    putNullable("matchNumber", matchNumber)
    putNullable("status", status)
    putNullable("homeTeam", homeTeam)
    putNullable("awayTeam", awayTeam)
    putNullable("homeTeamLogo", homeTeamLogo)
    putNullable("awayTeamLogo", awayTeamLogo)
    putNullable("homeScore", homeScore)
    putNullable("awayScore", awayScore)
    putNullable("kickoff", kickoff)
}

private fun JSONObject.toWorldCupBootstrapInfo() = WorldCupBootstrapInfo(
    tournament = getJSONObject("tournament").run {
        WorldCupTournamentInfo(
            id = getString("id"),
            name = getString("name"),
            stage = getString("stage"),
            hostCities = optInt("hostCities", 16),
            daysToFinal = nullableInt("daysToFinal")
        )
    },
    liveMatches = optJSONArray("liveMatches").toMatches(),
    todayMatches = optJSONArray("todayMatches").toMatches(),
    upcomingMatches = optJSONArray("upcomingMatches").toMatches(),
    recentResults = optJSONArray("recentResults").toMatches(),
    groups = buildList {
        val source = optJSONArray("groups")
        if (source != null) for (index in 0 until source.length()) source.getJSONObject(index).run {
            add(WorldCupGroupInfo(getString("code"), optJSONArray("teams").toStandingTeams()))
        }
    },
    bracket = buildList {
        val source = optJSONArray("bracket")
        if (source != null) for (index in 0 until source.length()) source.getJSONObject(index).run {
            add(WorldCupBracketRoundInfo(getString("round"), optJSONArray("matches").toBracketMatches()))
        }
    },
    library = buildList {
        val source = optJSONArray("library")
        if (source != null) for (index in 0 until source.length()) source.getJSONObject(index).run {
            add(WorldCupLibraryItemInfo(getString("id"), getString("title"), getString("category"), getString("body"), optInt("readMinutes", 4)))
        }
    },
    randomFact = getJSONObject("randomFact").run { WorldCupFactInfo(getString("title"), getString("body")) }
)

private fun JSONArray?.toMatches(): List<ScheduleMatch> = buildList {
    if (this@toMatches != null) for (index in 0 until length()) getJSONObject(index).run {
        add(ScheduleMatch(
            matchId = getString("matchId"), league = getString("league"),
            name = nullableString("name"), shortName = nullableString("shortName"),
            status = nullableString("status"), statusDescription = nullableString("statusDescription"),
            state = nullableString("state"), kickoff = nullableString("kickoff"),
            homeTeam = optJSONObject("homeTeam")?.toTeam(), awayTeam = optJSONObject("awayTeam")?.toTeam(),
            venue = optJSONObject("venue")?.let { MatchVenueInfo(it.nullableString("name"), it.nullableString("city")) },
            detailApi = getString("detailApi")
        ))
    }
}

private fun JSONObject.toTeam() = MatchTeamInfo(
    id = getString("id"), name = getString("name"), shortName = nullableString("shortName"),
    abbreviation = nullableString("abbreviation"), logo = nullableString("logo"), score = nullableInt("score")
)

private fun JSONArray?.toStandingTeams(): List<StandingTeamInfo> = buildList {
    if (this@toStandingTeams != null) for (index in 0 until length()) getJSONObject(index).run {
        add(StandingTeamInfo(
            rank = nullableInt("rank"), teamId = getString("teamId"), name = getString("name"),
            abbreviation = nullableString("abbreviation"), logo = nullableString("logo"),
            group = nullableString("group"), stage = nullableString("stage"), played = nullableInt("played"),
            wins = nullableInt("wins"), draws = nullableInt("draws"), losses = nullableInt("losses"),
            goalsFor = nullableInt("goalsFor"), goalsAgainst = nullableInt("goalsAgainst"),
            goalDifference = nullableInt("goalDifference"), points = nullableInt("points")
        ))
    }
}

private fun JSONArray?.toBracketMatches(): List<WorldCupBracketMatchInfo> = buildList {
    if (this@toBracketMatches != null) for (index in 0 until length()) getJSONObject(index).run {
        add(WorldCupBracketMatchInfo(
            eventId = getString("eventId"), round = getString("round"), matchNumber = nullableInt("matchNumber"),
            status = nullableString("status"), homeTeam = nullableString("homeTeam"), awayTeam = nullableString("awayTeam"),
            homeTeamLogo = nullableString("homeTeamLogo"), awayTeamLogo = nullableString("awayTeamLogo"),
            homeScore = nullableInt("homeScore"), awayScore = nullableInt("awayScore"), kickoff = nullableString("kickoff")
        ))
    }
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key).ifBlank { null }

private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else optInt(key)
