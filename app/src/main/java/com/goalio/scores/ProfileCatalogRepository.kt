package com.goalio.scores

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ProfileCatalog(
    val teams: List<FavoriteTeam>,
    val players: List<FavoritePlayer>,
    val nextTeamCursor: String? = null,
    val nextPlayerCursor: String? = null,
    val teamError: String? = null,
    val playerError: String? = null
)

object ProfileCatalogRepository {
    private val mutex = Mutex()
    private var cachedCatalog: ProfileCatalog? = null

    fun cached(): ProfileCatalog? = cachedCatalog

    suspend fun preload(context: Context, force: Boolean = false): ProfileCatalog {
        val catalog = mutex.withLock {
            if (!force) cachedCatalog?.let { return@withLock it }

            // Loading teams first also establishes the anonymous Firebase session before
            // the parallel player requests begin.
            var teamError: String? = null
            var playerError: String? = null
            val teamPage = runCatching { GoalioBackendApi.getTeams(limit = 6) }
                .onFailure { teamError = it.catalogMessage("Could not load teams.") }
                .getOrElse { BackendPage(emptyList(), null) }
            val teams = teamPage.items

            val playerPage = runCatching { GoalioBackendApi.getPlayers(limit = 6) }
                .onFailure { playerError = it.catalogMessage("Could not load players.") }
                .getOrElse { BackendPage(emptyList(), null) }
            val players = playerPage.items
                .distinctBy { it.id }
                .map { it.withCompetitionIds(teams) }

            ProfileCatalog(
                teams = teams,
                players = players,
                nextTeamCursor = teamPage.nextCursor,
                nextPlayerCursor = playerPage.nextCursor,
                teamError = teamError ?: if (teams.isEmpty()) "No teams are available right now." else null,
                playerError = playerError ?: if (players.isEmpty()) "No players are available right now." else null
            ).also { cachedCatalog = it }
        }

        warmImages(context, catalog)
        return catalog
    }

    private fun warmImages(context: Context, catalog: ProfileCatalog) {
        runCatching {
            val imageLoader = SingletonImageLoader.get(context)
            val urls = (catalog.teams.take(6).mapNotNull { it.imageUrl } +
                catalog.players.take(6).mapNotNull { it.imageUrl }).distinct()
            urls.forEach { url ->
                imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
        }
    }
}

private fun Throwable.catalogMessage(prefix: String): String =
    if (this is BackendException) "$prefix $message" else "$prefix ${message ?: "Check the backend connection and try again."}"

internal fun FavoritePlayer.withCompetitionIds(teams: List<FavoriteTeam>): FavoritePlayer {
    if (competitionIds.isNotEmpty()) return this
    val inferred = teams.asSequence()
        .filter { team -> this.team.contains(team.name, ignoreCase = true) }
        .flatMap { it.competitionIds.asSequence() }
        .toSet()
    return copy(competitionIds = inferred)
}
