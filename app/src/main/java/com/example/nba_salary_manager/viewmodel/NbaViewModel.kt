package com.example.nba_salary_manager.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nba_salary_manager.data.api.EspnPlayerStatsRepository
import com.example.nba_salary_manager.data.api.EspnTeamStandingsRepository
import com.example.nba_salary_manager.data.api.RetrofitClient
import com.example.nba_salary_manager.data.model.Game
import com.example.nba_salary_manager.data.model.Player
import com.example.nba_salary_manager.data.model.PlayerPositionFilter
import com.example.nba_salary_manager.data.model.PlayerSeasonStats
import com.example.nba_salary_manager.data.model.PlayerSortOption
import com.example.nba_salary_manager.data.model.PlayerStatsUiState
import com.example.nba_salary_manager.data.model.ReplacementCandidate
import com.example.nba_salary_manager.data.model.RosterPlayerTemplate
import com.example.nba_salary_manager.data.model.RosterTemplate
import com.example.nba_salary_manager.data.model.RosterTemplateCategory
import com.example.nba_salary_manager.data.model.RosterTemplateRepository
import com.example.nba_salary_manager.data.model.Team
import com.example.nba_salary_manager.data.model.TeamStandingSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NbaViewModel(private val appContext: Context) : ViewModel() {

    private val rosterTemplatesCatalog = RosterTemplateRepository.templates
    private val cachePreferences = appContext.getSharedPreferences("nba_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Teams state ──
    var teams by mutableStateOf<List<Team>>(emptyList())
        private set
    var teamsLoading by mutableStateOf(false)
        private set
    var teamsError by mutableStateOf<String?>(null)
        private set
    var teamStatsLoading by mutableStateOf(false)
        private set
    var teamStatsError by mutableStateOf<String?>(null)
        private set
    var teamStandings by mutableStateOf<List<TeamStandingSummary>>(emptyList())
        private set

    // ── Players state ──
    var players by mutableStateOf<List<Player>>(emptyList())
        private set
    var playersLoading by mutableStateOf(false)
        private set
    var playersError by mutableStateOf<String?>(null)
        private set
    var playerSearchQuery by mutableStateOf("")
        private set
    var playersNextCursor by mutableStateOf<Int?>(null)
        private set
    var playersHasMore by mutableStateOf(true)
        private set
    var playerStatsStates by mutableStateOf<Map<Int, PlayerStatsUiState>>(emptyMap())
        private set
    var selectedPlayerPositionFilter by mutableStateOf(PlayerPositionFilter.ALL)
        private set
    var selectedPlayerSortOption by mutableStateOf(PlayerSortOption.NAME)
        private set
    var seasonLeaderboardStats by mutableStateOf<Map<Int, PlayerSeasonStats>>(emptyMap())
        private set
    private var seasonLeaderboardPlayers by mutableStateOf<List<Player>>(emptyList())
    private var seasonLeaderboardSeason by mutableStateOf<Int?>(null)

    // ── Games state ──
    var games by mutableStateOf<List<Game>>(emptyList())
        private set
    var gamesLoading by mutableStateOf(false)
        private set
    var gamesError by mutableStateOf<String?>(null)
        private set
    var gamesNextCursor by mutableStateOf<Int?>(null)
        private set
    var gamesHasMore by mutableStateOf(true)
        private set
    var selectedGameYear by mutableStateOf<Int?>(null)
        private set
    var selectedGameMonth by mutableStateOf<Int?>(null)
        private set

    var selectedRosterCategory by mutableStateOf(RosterTemplateCategory.CURRENT)
        private set
    var activeRosterTemplateId by mutableStateOf(rosterTemplatesCatalog.firstOrNull()?.id)
        private set
    var editableRosterName by mutableStateOf(rosterTemplatesCatalog.firstOrNull()?.teamName.orEmpty())
        private set
    var editableRosterPlayStyle by mutableStateOf(rosterTemplatesCatalog.firstOrNull()?.playStyle.orEmpty())
        private set
    var editableRosterPlayers by mutableStateOf(
        rosterTemplatesCatalog.firstOrNull()?.players ?: emptyList()
    )
        private set
    var replacementCandidates by mutableStateOf<List<ReplacementCandidate>>(emptyList())
        private set
    var replacementCandidatesLoading by mutableStateOf(false)
        private set
    var replacementCandidatesError by mutableStateOf<String?>(null)
        private set
    var replacementTargetStats by mutableStateOf<PlayerSeasonStats?>(null)
        private set
    var replacementTargetPosition by mutableStateOf("")
        private set

    private val api = RetrofitClient.api
    private val playersCacheDurationMs = 1000L * 60L * 30L

    val rosterTemplates: List<RosterTemplate>
        get() = rosterTemplatesCatalog

    val filteredRosterTemplates: List<RosterTemplate>
        get() = rosterTemplatesCatalog.filter { it.category == selectedRosterCategory }

    val activeRosterTemplate: RosterTemplate?
        get() = rosterTemplatesCatalog.firstOrNull { it.id == activeRosterTemplateId }

    val availableGameYears: List<Int>
        get() = (Calendar.getInstance().get(Calendar.YEAR) downTo 1970).toList()

    val availableGameMonths: List<Int>
        get() = (1..12).toList()

    val visiblePlayers: List<Player>
        get() = playerSourceForCurrentMode()
            .filter { player -> selectedPlayerPositionFilter.matches(player.position) }
            .sortedWith(playerComparator(selectedPlayerSortOption))

    val canLoadMorePlayers: Boolean
        get() = !shouldUseSeasonLeaderboard() && playersHasMore

    fun updateRosterCategory(category: RosterTemplateCategory) {
        selectedRosterCategory = category
    }

    fun useRosterTemplate(templateId: String) {
        val template = rosterTemplatesCatalog.firstOrNull { it.id == templateId } ?: return
        activeRosterTemplateId = template.id
        selectedRosterCategory = template.category
        editableRosterName = template.teamName
        editableRosterPlayStyle = template.playStyle
        editableRosterPlayers = template.players.map { it.copy() }
        clearReplacementCandidates()
    }

    fun updateEditableRosterName(name: String) {
        editableRosterName = name
    }

    fun updateEditableRosterPlayStyle(style: String) {
        editableRosterPlayStyle = style
    }

    fun updateEditableRosterPlayer(
        index: Int,
        transform: (RosterPlayerTemplate) -> RosterPlayerTemplate
    ) {
        if (index !in editableRosterPlayers.indices) return
        val updatedPlayers = editableRosterPlayers.toMutableList()
        updatedPlayers[index] = transform(updatedPlayers[index])
        editableRosterPlayers = updatedPlayers
    }

    fun addEditableRosterPlayer() {
        editableRosterPlayers = editableRosterPlayers + RosterPlayerTemplate(
            name = "",
            position = "",
            role = "Rotacion",
            salaryTier = "",
            note = ""
        )
    }

    fun removeEditableRosterPlayer(index: Int) {
        if (index !in editableRosterPlayers.indices) return
        editableRosterPlayers = editableRosterPlayers.filterIndexed { currentIndex, _ ->
            currentIndex != index
        }
    }

    fun clearReplacementCandidates() {
        replacementCandidates = emptyList()
        replacementCandidatesLoading = false
        replacementCandidatesError = null
        replacementTargetStats = null
        replacementTargetPosition = ""
    }

    fun loadReplacementCandidates(target: RosterPlayerTemplate) {
        viewModelScope.launch {
            replacementCandidatesLoading = true
            replacementCandidatesError = null
            replacementCandidates = emptyList()
            replacementTargetPosition = target.position

            try {
                val targetPlayer = rosterTemplateToPlayer(target)
                replacementTargetStats = targetPlayer?.let { EspnPlayerStatsRepository.getPlayerSeasonStats(it) }
                val pool = fetchReplacementPool(target)
                replacementCandidates = rankReplacementCandidates(
                    target = target,
                    targetStats = replacementTargetStats,
                    pool = pool
                )

                if (replacementCandidates.isEmpty()) {
                    replacementCandidatesError = "No se encontraron candidatos similares para ${target.position}."
                }
            } catch (e: Exception) {
                replacementCandidatesError = e.message ?: "Error buscando reemplazos"
            }

            replacementCandidatesLoading = false
        }
    }

    fun replaceEditableRosterPlayer(index: Int, player: Player) {
        updateEditableRosterPlayer(index) { current ->
            current.copy(
                name = "${player.firstName} ${player.lastName}",
                nbaPlayerId = player.id,
                position = current.position
            )
        }
    }

    fun loadEditableRosterStats(forceRefresh: Boolean = false) {
        editableRosterPlayers.forEach { player ->
            loadRosterPlayerStats(player, forceRefresh = forceRefresh)
        }
    }

    fun loadRosterPlayerStats(player: RosterPlayerTemplate, forceRefresh: Boolean = false) {
        val rosterPlayer = rosterTemplateToPlayer(player) ?: return
        loadPlayerStats(rosterPlayer, forceRefresh = forceRefresh)
    }

    fun rosterPlayerStatsState(player: RosterPlayerTemplate): PlayerStatsUiState {
        val rosterPlayer = rosterTemplateToPlayer(player) ?: return PlayerStatsUiState()
        return playerStatsState(rosterPlayer.id)
    }

    // ── Teams ──

    fun loadTeams() {
        if (teamsLoading) return

        viewModelScope.launch {
            teamsLoading = true
            teamsError = null
            teamStatsLoading = false
            teamStatsError = null
            try {
                val response = api.getTeams()
                val loadedTeams = response.data
                    .filter { it.abbreviation in currentNbaTeamAbbreviations }
                    .distinctBy { it.id }
                    .sortedBy { it.fullName }
                teams = loadedTeams
                teamStandings = emptyList()
            } catch (e: Exception) {
                teamsError = apiErrorMessage(e, "Error desconocido al cargar equipos")
                teamStandings = emptyList()
            }
            teamsLoading = false
        }
    }

    fun loadTeamStandingsIfNeeded() {
        if (teamStatsLoading || teamStandings.isNotEmpty() || teams.isEmpty()) return

        viewModelScope.launch {
            teamStatsLoading = true
            teamStatsError = null
            try {
                teamStandings = EspnTeamStandingsRepository.getTeamStandings(
                    seasonYear = currentEspnSeason(),
                    teams = teams
                )
            } catch (e: Exception) {
                teamStatsError = apiErrorMessage(e, "Error desconocido al cargar estadisticas de equipos")
                teamStandings = emptyList()
            }
            teamStatsLoading = false
        }
    }

    // ── Players ──

    fun updatePlayerSearch(query: String) {
        playerSearchQuery = query
    }

    fun updatePlayerPositionFilter(filter: PlayerPositionFilter) {
        selectedPlayerPositionFilter = filter
        prefetchSortedPlayerStats()
    }

    fun updatePlayerSortOption(sortOption: PlayerSortOption) {
        selectedPlayerSortOption = sortOption
        prefetchSortedPlayerStats()
    }

    fun playerSummaryForSorting(playerId: Int): PlayerSeasonStats? {
        return seasonLeaderboardStats[playerId] ?: playerStatsStates[playerId]?.summary
    }

    fun searchPlayers() {
        if (shouldUseSeasonLeaderboard()) {
            loadSeasonLeaderboard(forceRefresh = true)
            return
        }
        if (playersLoading) return
        val query = playerSearchQuery.trim()
        val cachedPage = readCachedPlayersPage(query)
        if (cachedPage != null && isCacheFresh(cachedPage)) {
            players = cachedPage.players.sortedByDescending { it.id }
            playersNextCursor = cachedPage.nextCursor
            playersHasMore = cachedPage.nextCursor != null
            playersError = null
            prefetchSortedPlayerStats()
            return
        }

        viewModelScope.launch {
            playersLoading = true
            playersError = null
            playersNextCursor = null
            playersHasMore = true
            try {
                val response = api.getPlayers(
                    search = query.ifBlank { null },
                    perPage = 15
                )
                players = response.data.sortedByDescending { it.id }
                playersNextCursor = response.meta?.nextCursor
                playersHasMore = response.meta?.nextCursor != null
                cachePlayersPage(
                    query = query,
                    players = players,
                    nextCursor = playersNextCursor
                )
                prefetchSortedPlayerStats()
            } catch (e: Exception) {
                if (cachedPage != null) {
                    players = cachedPage.players.sortedByDescending { it.id }
                    playersNextCursor = cachedPage.nextCursor
                    playersHasMore = cachedPage.nextCursor != null
                    playersError = null
                    prefetchSortedPlayerStats()
                } else {
                    playersError = apiErrorMessage(e, "Error desconocido al cargar jugadores")
                }
            }
            playersLoading = false
        }
    }

    fun loadPlayers() {
        searchPlayers()
    }

    fun loadMorePlayers() {
        if (shouldUseSeasonLeaderboard()) return
        val cursor = playersNextCursor ?: return
        if (playersLoading) return
        viewModelScope.launch {
            playersLoading = true
            try {
                val response = api.getPlayers(
                    search = playerSearchQuery.ifBlank { null },
                    perPage = 15,
                    cursor = cursor
                )
                players = (players + response.data).sortedByDescending { it.id }
                playersNextCursor = response.meta?.nextCursor
                playersHasMore = response.meta?.nextCursor != null
                cachePlayersPage(
                    query = playerSearchQuery.trim(),
                    players = players,
                    nextCursor = playersNextCursor
                )
                prefetchSortedPlayerStats()
            } catch (e: Exception) {
                playersError = e.message ?: "Error cargando más jugadores"
            }
            playersLoading = false
        }
    }

    // ── Games ──

    fun updateGameYearFilter(year: Int?) {
        selectedGameYear = year
        loadGames()
    }

    fun updateGameMonthFilter(month: Int?) {
        if (month == null) {
            selectedGameMonth = null
            selectedGameYear = null
        } else {
            selectedGameMonth = month
            if (selectedGameYear == null) {
                selectedGameYear = Calendar.getInstance().get(Calendar.YEAR)
            }
        }
        loadGames()
    }

    fun clearGameFilters() {
        selectedGameYear = null
        selectedGameMonth = null
        loadGames()
    }

    fun loadGames() {
        if (gamesLoading) return

        viewModelScope.launch {
            gamesLoading = true
            gamesError = null
            gamesNextCursor = null
            gamesHasMore = true
            games = emptyList()
            try {
                val (startDate, endDate) = getGamesDateRange()

                val response = api.getGames(
                    perPage = 50,
                    startDate = startDate,
                    endDate = endDate
                )
                games = response.data.sortedBy { it.date }
                gamesNextCursor = response.meta?.nextCursor
                gamesHasMore = response.meta?.nextCursor != null
            } catch (e: Exception) {
                gamesError = apiErrorMessage(e, "Error desconocido al cargar partidos")
            }
            gamesLoading = false
        }
    }

    fun loadMoreGames() {
        val cursor = gamesNextCursor ?: return
        if (gamesLoading) return
        viewModelScope.launch {
            gamesLoading = true
            try {
                val (startDate, endDate) = getGamesDateRange()

                val response = api.getGames(
                    perPage = 50,
                    cursor = cursor,
                    startDate = startDate,
                    endDate = endDate
                )
                games = (games + response.data).sortedBy { it.date }
                gamesNextCursor = response.meta?.nextCursor
                gamesHasMore = response.meta?.nextCursor != null
            } catch (e: Exception) {
                gamesError = e.message ?: "Error cargando más partidos"
            }
            gamesLoading = false
        }
    }

    fun playerStatsState(playerId: Int): PlayerStatsUiState {
        return playerStatsStates[playerId] ?: PlayerStatsUiState()
    }

    fun loadPlayerStats(player: Player, forceRefresh: Boolean = false) {
        val playerId = player.id
        val currentState = playerStatsState(playerId)
        if (currentState.isLoading) return
        if (currentState.hasLoaded && !forceRefresh) return

        viewModelScope.launch {
            playerStatsStates = playerStatsStates + (
                playerId to currentState.copy(
                    isLoading = true,
                    error = null
                )
            )

            try {
                val summary = EspnPlayerStatsRepository.getPlayerSeasonStats(player)
                playerStatsStates = playerStatsStates + (
                    playerId to PlayerStatsUiState(
                        isLoading = false,
                        hasLoaded = true,
                        summary = summary,
                        error = null
                    )
                )
            } catch (e: Exception) {
                playerStatsStates = playerStatsStates + (
                    playerId to currentState.copy(
                        isLoading = false,
                        error = playerStatsErrorMessage(e)
                    )
                )
            }
        }
    }

    private suspend fun fetchReplacementPool(target: RosterPlayerTemplate): List<Player> {
        val collected = linkedMapOf<Int, Player>()
        var cursor: Int? = null
        var page = 0

        while (page < 8 && collected.size < 36) {
            val response = api.getPlayers(
                perPage = 100,
                cursor = cursor
            )

            response.data
                .filter { candidate ->
                    candidate.id != target.nbaPlayerId &&
                        candidate.team != null &&
                        isCompatibleReplacement(target.position, candidate.position.orEmpty())
                }
                .forEach { candidate ->
                    collected.putIfAbsent(candidate.id, candidate)
                }

            cursor = response.meta?.nextCursor
            if (cursor == null) break
            page++
        }

        return collected.values.toList()
    }

    private suspend fun rankReplacementCandidates(
        target: RosterPlayerTemplate,
        targetStats: PlayerSeasonStats?,
        pool: List<Player>
    ): List<ReplacementCandidate> = coroutineScope {
        pool.take(18).map { candidate ->
            async {
                val stats = runCatching {
                    EspnPlayerStatsRepository.getPlayerSeasonStats(candidate)
                }.getOrNull()

                ReplacementCandidate(
                    player = candidate,
                    stats = stats,
                    similarityScore = computeSimilarityScore(
                        slotPosition = target.position,
                        targetStats = targetStats,
                        candidateStats = stats
                    ),
                    similarityLabel = buildSimilarityLabel(
                        slotPosition = target.position,
                        targetStats = targetStats,
                        candidateStats = stats
                    )
                )
            }
        }.awaitAll()
            .sortedBy { it.similarityScore }
            .take(8)
    }

    private fun computeSimilarityScore(
        slotPosition: String,
        targetStats: PlayerSeasonStats?,
        candidateStats: PlayerSeasonStats?
    ): Double {
        if (targetStats == null || candidateStats == null) return 9999.0

        val position = primaryPosition(slotPosition)
        val pointsWeight = when (position) {
            "PG", "SG", "SF" -> 3.2
            else -> 2.4
        }
        val reboundsWeight = if (position == "C" || position == "PF") 3.1 else 1.8
        val assistsWeight = if (position == "PG") 3.3 else 1.9
        val blocksWeight = if (position == "C" || position == "PF") 2.7 else 0.9
        val triplesWeight = if (position == "PG" || position == "SG" || position == "SF") 2.1 else 1.1

        var score = 0.0
        score += abs(targetStats.pointsPerGame - candidateStats.pointsPerGame) * pointsWeight
        score += abs(targetStats.reboundsPerGame - candidateStats.reboundsPerGame) * reboundsWeight
        score += abs(targetStats.assistsPerGame - candidateStats.assistsPerGame) * assistsWeight
        score += abs(targetStats.blocksPerGame - candidateStats.blocksPerGame) * blocksWeight
        score += abs(targetStats.threePointsMade.toDouble() - candidateStats.threePointsMade.toDouble()) / 20.0 * triplesWeight
        score += abs((targetStats.threePointPercentage ?: 0.0) - (candidateStats.threePointPercentage ?: 0.0)) * 0.18
        score += abs((targetStats.minutesPerGame ?: 0.0) - (candidateStats.minutesPerGame ?: 0.0)) * 0.6
        return score
    }

    private fun buildSimilarityLabel(
        slotPosition: String,
        targetStats: PlayerSeasonStats?,
        candidateStats: PlayerSeasonStats?
    ): String {
        if (targetStats == null || candidateStats == null) {
            return "Misma posicion, sin comparativa completa"
        }

        val score = computeSimilarityScore(slotPosition, targetStats, candidateStats)
        return when {
            score < 18 -> "Perfil muy parecido"
            score < 30 -> "Encaje similar"
            else -> "Alternativa de misma posicion"
        }
    }

    private fun isCompatibleReplacement(slotPosition: String, candidatePosition: String): Boolean {
        val slot = primaryPosition(slotPosition)
        val tokens = normalizePositionTokens(candidatePosition)
        return when (slot) {
            "PG" -> tokens.any { it == "PG" || it == "G" }
            "SG" -> tokens.any { it == "SG" || it == "G" }
            "SF" -> tokens.any { it == "SF" || it == "F" }
            "PF" -> tokens.any { it == "PF" || it == "F" }
            "C" -> "C" in tokens
            else -> false
        }
    }

    private fun primaryPosition(position: String): String {
        return normalizePositionTokens(position).firstOrNull()
            ?: position.uppercase(Locale.US).trim().ifBlank { "G" }
    }

    private fun normalizePositionTokens(position: String): List<String> {
        return position
            .uppercase(Locale.US)
            .replace("/", "-")
            .split("-", " ")
            .mapNotNull { token ->
                when (token.trim()) {
                    "PG", "SG", "SF", "PF", "C", "G", "F" -> token.trim()
                    else -> null
                }
            }
    }

    private fun rosterTemplateToPlayer(player: RosterPlayerTemplate): Player? {
        val parts = player.name.trim().split(" ").filter { it.isNotBlank() }
        val firstName = parts.firstOrNull() ?: return null
        val lastName = parts.drop(1).joinToString(" ").ifBlank { "-" }

        return Player(
            id = player.nbaPlayerId ?: -abs(player.name.hashCode()),
            firstName = firstName,
            lastName = lastName,
            position = player.position,
            height = null,
            weight = null,
            jerseyNumber = null,
            college = null,
            country = null,
            draftYear = null,
            draftRound = null,
            draftNumber = null,
            team = null
        )
    }

    private fun getGamesDateRange(): Pair<String?, String?> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        val startCalendar = today.clone() as Calendar
        val endCalendar = today.clone() as Calendar

        when {
            selectedGameYear != null && selectedGameMonth != null -> {
                startCalendar.set(Calendar.YEAR, selectedGameYear!!)
                startCalendar.set(Calendar.MONTH, selectedGameMonth!! - 1)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)

                endCalendar.set(Calendar.YEAR, selectedGameYear!!)
                endCalendar.set(Calendar.MONTH, selectedGameMonth!! - 1)
                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            }

            selectedGameYear != null -> {
                startCalendar.set(Calendar.YEAR, selectedGameYear!!)
                startCalendar.set(Calendar.MONTH, Calendar.JANUARY)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)

                endCalendar.set(Calendar.YEAR, selectedGameYear!!)
                endCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
                endCalendar.set(Calendar.DAY_OF_MONTH, 31)
            }

            selectedGameMonth != null -> {
                startCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                startCalendar.set(Calendar.MONTH, selectedGameMonth!! - 1)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)

                endCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                endCalendar.set(Calendar.MONTH, selectedGameMonth!! - 1)
                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            }

            else -> {
                startCalendar.add(Calendar.MONTH, -1)
                endCalendar.add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        return formatter.format(startCalendar.time) to formatter.format(endCalendar.time)
    }

    private fun playerStatsErrorMessage(error: Exception): String {
        return error.message ?: "Error cargando estadisticas"
    }

    private fun prefetchSortedPlayerStats() {
        if (!selectedPlayerSortOption.requiresStats) return
        if (shouldUseSeasonLeaderboard()) {
            loadSeasonLeaderboard()
            return
        }
        visiblePlayers.forEach { player ->
            loadPlayerStats(player)
        }
    }

    private fun shouldUseSeasonLeaderboard(): Boolean {
        return selectedPlayerSortOption.requiresStats && playerSearchQuery.trim().isBlank()
    }

    private fun playerSourceForCurrentMode(): List<Player> {
        return if (shouldUseSeasonLeaderboard() && seasonLeaderboardPlayers.isNotEmpty()) {
            seasonLeaderboardPlayers
        } else {
            players
        }
    }

    private fun playerComparator(sortOption: PlayerSortOption): Comparator<Player> {
        return when (sortOption) {
            PlayerSortOption.NAME -> compareBy<Player> { it.lastName.lowercase(Locale.US) }
                .thenBy { it.firstName.lowercase(Locale.US) }

            else -> compareByDescending<Player> { playerSortMetric(it, sortOption) }
                .thenBy { it.lastName.lowercase(Locale.US) }
                .thenBy { it.firstName.lowercase(Locale.US) }
        }
    }

    private fun playerSortMetric(player: Player, sortOption: PlayerSortOption): Double {
        val summary = playerSummaryForSorting(player.id) ?: return Double.NEGATIVE_INFINITY
        return when (sortOption) {
            PlayerSortOption.NAME -> 0.0
            PlayerSortOption.POINTS -> summary.pointsPerGame
            PlayerSortOption.REBOUNDS -> summary.reboundsPerGame
            PlayerSortOption.ASSISTS -> summary.assistsPerGame
            PlayerSortOption.STEALS -> summary.stealsPerGame
            PlayerSortOption.BLOCKS -> summary.blocksPerGame
            PlayerSortOption.MINUTES -> summary.minutesPerGame ?: Double.NEGATIVE_INFINITY
        }
    }

    private fun loadSeasonLeaderboard(forceRefresh: Boolean = false) {
        val season = currentRegularSeason()
        if (playersLoading) return
        if (!forceRefresh && seasonLeaderboardSeason == season && seasonLeaderboardPlayers.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            playersLoading = true
            playersError = null
            try {
                val leaderboard = fetchSeasonLeaderboard(season)
                seasonLeaderboardPlayers = leaderboard.map { it.player }
                seasonLeaderboardStats = leaderboard.associate { it.player.id to it.summary }
                seasonLeaderboardSeason = season
            } catch (e: Exception) {
                playersError = apiErrorMessage(
                    e,
                    "No se pudo construir el ranking de temporada de jugadores"
                )
            }
            playersLoading = false
        }
    }

    private suspend fun fetchSeasonLeaderboard(season: Int): List<PlayerSeasonLeaderboardEntry> {
        val accumulators = linkedMapOf<Int, MutablePlayerSeasonAccumulator>()
        var cursor: Int? = null
        var page = 0

        do {
            val response = api.getStats(
                seasons = listOf(season),
                perPage = 100,
                cursor = cursor
            )

            response.data.forEach { stat ->
                val statPlayer = stat.player ?: return@forEach
                val team = statPlayer.team ?: stat.team ?: return@forEach
                val game = stat.game ?: return@forEach

                if (game.postseason) return@forEach
                if (team.abbreviation !in currentNbaTeamAbbreviations) return@forEach

                val normalizedPlayer = statPlayer.copy(team = team)
                val accumulator = accumulators.getOrPut(normalizedPlayer.id) {
                    MutablePlayerSeasonAccumulator(player = normalizedPlayer)
                }
                accumulator.player = normalizedPlayer
                accumulator.add(stat)
            }

            cursor = response.meta?.nextCursor
            page++
        } while (cursor != null && page < 500)

        return accumulators.values
            .asSequence()
            .mapNotNull { it.toLeaderboardEntryOrNull(season) }
            .filter { entry ->
                entry.player.team != null &&
                    entry.summary.gamesPlayed >= MIN_GAMES_FOR_PLAYER_LEADERBOARD
            }
            .toList()
    }

    private fun apiErrorMessage(error: Exception, fallback: String): String {
        val message = error.message.orEmpty()
        return if (message.contains("429")) {
            "Demasiadas peticiones a la API. Espera unos segundos y vuelve a intentarlo."
        } else {
            message.ifBlank { fallback }
        }
    }

    private fun readCachedPlayersPage(query: String): CachedPlayersPage? {
        val raw = cachePreferences.getString(playersCacheKey(query), null) ?: return null
        return runCatching {
            gson.fromJson<CachedPlayersPage>(
                raw,
                object : TypeToken<CachedPlayersPage>() {}.type
            )
        }.getOrNull()
    }

    private fun cachePlayersPage(
        query: String,
        players: List<Player>,
        nextCursor: Int?
    ) {
        val payload = CachedPlayersPage(
            players = players,
            nextCursor = nextCursor,
            cachedAt = System.currentTimeMillis()
        )
        cachePreferences.edit()
            .putString(playersCacheKey(query), gson.toJson(payload))
            .apply()
    }

    private fun playersCacheKey(query: String): String {
        val normalized = query
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "all" }
        return "players_cache_$normalized"
    }

    private fun isCacheFresh(cache: CachedPlayersPage): Boolean {
        return System.currentTimeMillis() - cache.cachedAt <= playersCacheDurationMs
    }

    private suspend fun loadSeasonGames(season: Int): List<Game> {
        val seasonGames = mutableListOf<Game>()
        var cursor: Int? = null

        do {
            val response = api.getGames(
                seasons = listOf(season),
                perPage = 100,
                cursor = cursor
            )
            seasonGames += response.data
            cursor = response.meta?.nextCursor
        } while (cursor != null)

        return seasonGames
    }

    private fun buildTeamStandings(
        teams: List<Team>,
        seasonGames: List<Game>
    ): List<TeamStandingSummary> {
        val records = teams.associate { team ->
            team.id to MutableTeamRecord(team = team)
        }.toMutableMap()

        seasonGames
            .asSequence()
            .filter { !it.postseason && isCompletedGame(it) }
            .forEach { game ->
                val homeRecord = records.getOrPut(game.homeTeam.id) {
                    MutableTeamRecord(team = game.homeTeam)
                }
                val awayRecord = records.getOrPut(game.visitorTeam.id) {
                    MutableTeamRecord(team = game.visitorTeam)
                }

                homeRecord.points += game.homeTeamScore
                awayRecord.points += game.visitorTeamScore

                when {
                    game.homeTeamScore > game.visitorTeamScore -> {
                        homeRecord.wins += 1
                        awayRecord.losses += 1
                    }

                    game.homeTeamScore < game.visitorTeamScore -> {
                        awayRecord.wins += 1
                        homeRecord.losses += 1
                    }
                }
            }

        val standingsByConference = records.values
            .groupBy { it.team.conference }
            .values
            .flatMap { conferenceTeams ->
                conferenceTeams
                    .sortedWith(
                        compareByDescending<MutableTeamRecord> { it.wins }
                            .thenBy { it.losses }
                            .thenByDescending { it.points }
                            .thenBy { it.team.fullName }
                    )
                    .mapIndexed { index, record ->
                        TeamStandingSummary(
                            team = record.team,
                            leagueRank = 0,
                            conferenceRank = index + 1,
                            points = record.points,
                            wins = record.wins,
                            losses = record.losses
                        )
                    }
            }
        val rankedByLeague = standingsByConference
            .sortedWith(
                compareByDescending<TeamStandingSummary> { it.wins }
                    .thenBy { it.losses }
                    .thenByDescending { it.points }
                    .thenBy { it.team.fullName }
            )
            .mapIndexed { index, summary ->
                summary.team.id to (index + 1)
            }
            .toMap()

        return standingsByConference
            .map { summary ->
                summary.copy(leagueRank = rankedByLeague[summary.team.id] ?: 0)
            }
            .sortedBy { it.team.fullName }
    }

    private fun buildEmptyStandings(teams: List<Team>): List<TeamStandingSummary> {
        return teams.map { team ->
            TeamStandingSummary(
                team = team,
                leagueRank = 0,
                conferenceRank = 0,
                points = 0,
                wins = 0,
                losses = 0
            )
        }
    }

    private fun currentRegularSeason(): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        return if (month >= Calendar.OCTOBER) year else year - 1
    }

    private fun currentEspnSeason(): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        return if (month >= Calendar.OCTOBER) year + 1 else year
    }

    private fun isCompletedGame(game: Game): Boolean {
        return game.status.contains("Final", ignoreCase = true)
    }

    private data class MutableTeamRecord(
        val team: Team,
        var points: Int = 0,
        var wins: Int = 0,
        var losses: Int = 0
    )

    private data class PlayerSeasonLeaderboardEntry(
        val player: Player,
        val summary: PlayerSeasonStats
    )

    private data class MutablePlayerSeasonAccumulator(
        var player: Player,
        var gamesPlayed: Int = 0,
        var totalMinutes: Double = 0.0,
        var minutesSamples: Int = 0,
        var totalPoints: Int = 0,
        var totalRebounds: Int = 0,
        var totalAssists: Int = 0,
        var totalSteals: Int = 0,
        var totalBlocks: Int = 0,
        var totalTurnovers: Int = 0,
        var totalFouls: Int = 0,
        var totalOffensiveRebounds: Int = 0,
        var totalDefensiveRebounds: Int = 0,
        var totalPlusMinus: Int = 0,
        var plusMinusSamples: Int = 0,
        var fieldGoalsMade: Int = 0,
        var fieldGoalsAttempted: Int = 0,
        var threePointsMade: Int = 0,
        var threePointsAttempted: Int = 0,
        var freeThrowsMade: Int = 0,
        var freeThrowsAttempted: Int = 0,
        var doubleDoubles: Int = 0,
        var tripleDoubles: Int = 0
    ) {
        fun add(stat: com.example.nba_salary_manager.data.model.PlayerStats) {
            gamesPlayed += 1
            Companion.parseMinutesPlayed(stat.min)?.let { minutes ->
                totalMinutes += minutes
                minutesSamples += 1
            }

            val points = stat.pts ?: 0
            val rebounds = stat.reb ?: 0
            val assists = stat.ast ?: 0
            val steals = stat.stl ?: 0
            val blocks = stat.blk ?: 0
            val turnovers = stat.turnover ?: 0
            val fouls = stat.pf ?: 0
            val offensiveRebounds = stat.oreb ?: 0
            val defensiveRebounds = stat.dreb ?: 0
            val plusMinus = stat.plusMinus

            totalPoints += points
            totalRebounds += rebounds
            totalAssists += assists
            totalSteals += steals
            totalBlocks += blocks
            totalTurnovers += turnovers
            totalFouls += fouls
            totalOffensiveRebounds += offensiveRebounds
            totalDefensiveRebounds += defensiveRebounds
            if (plusMinus != null) {
                totalPlusMinus += plusMinus
                plusMinusSamples += 1
            }

            fieldGoalsMade += stat.fgm ?: 0
            fieldGoalsAttempted += stat.fga ?: 0
            threePointsMade += stat.fg3m ?: 0
            threePointsAttempted += stat.fg3a ?: 0
            freeThrowsMade += stat.ftm ?: 0
            freeThrowsAttempted += stat.fta ?: 0

            val doubleDigitCategories = listOf(
                points,
                rebounds,
                assists,
                steals,
                blocks
            ).count { it >= 10 }

            if (doubleDigitCategories >= 2) doubleDoubles += 1
            if (doubleDigitCategories >= 3) tripleDoubles += 1
        }

        fun toLeaderboardEntryOrNull(season: Int): PlayerSeasonLeaderboardEntry? {
            if (gamesPlayed == 0) return null

            val summary = PlayerSeasonStats(
                season = season,
                gamesPlayed = gamesPlayed,
                gamesStarted = null,
                minutesPerGame = Companion.averageOrNull(totalMinutes, minutesSamples),
                pointsPerGame = totalPoints.toDouble() / gamesPlayed,
                reboundsPerGame = totalRebounds.toDouble() / gamesPlayed,
                assistsPerGame = totalAssists.toDouble() / gamesPlayed,
                stealsPerGame = totalSteals.toDouble() / gamesPlayed,
                blocksPerGame = totalBlocks.toDouble() / gamesPlayed,
                turnoversPerGame = totalTurnovers.toDouble() / gamesPlayed,
                foulsPerGame = totalFouls.toDouble() / gamesPlayed,
                offensiveReboundsPerGame = totalOffensiveRebounds.toDouble() / gamesPlayed,
                defensiveReboundsPerGame = totalDefensiveRebounds.toDouble() / gamesPlayed,
                plusMinusPerGame = Companion.averageOrNull(totalPlusMinus.toDouble(), plusMinusSamples),
                fieldGoalsMade = fieldGoalsMade,
                fieldGoalsAttempted = fieldGoalsAttempted,
                threePointsMade = threePointsMade,
                threePointsAttempted = threePointsAttempted,
                freeThrowsMade = freeThrowsMade,
                freeThrowsAttempted = freeThrowsAttempted,
                doubleDoubles = doubleDoubles,
                tripleDoubles = tripleDoubles,
                assistToTurnoverRatio = if (totalTurnovers > 0) {
                    totalAssists.toDouble() / totalTurnovers
                } else {
                    null
                },
                scoringEfficiency = null,
                shootingEfficiency = null,
                fieldGoalPercentage = Companion.percentage(fieldGoalsMade, fieldGoalsAttempted),
                threePointPercentage = Companion.percentage(threePointsMade, threePointsAttempted),
                freeThrowPercentage = Companion.percentage(freeThrowsMade, freeThrowsAttempted)
            )

            return PlayerSeasonLeaderboardEntry(player = player, summary = summary)
        }
    }

    private val currentNbaTeamAbbreviations = setOf(
        "ATL", "BOS", "BKN", "CHA", "CHI", "CLE", "DAL", "DEN", "DET", "GSW",
        "HOU", "IND", "LAC", "LAL", "MEM", "MIA", "MIL", "MIN", "NOP", "NYK",
        "OKC", "ORL", "PHI", "PHX", "POR", "SAC", "SAS", "TOR", "UTA", "WAS"
    )

    private companion object {
        const val MIN_GAMES_FOR_PLAYER_LEADERBOARD = 10

        fun averageOrNull(total: Double, samples: Int): Double? {
            return if (samples > 0) total / samples else null
        }

        fun percentage(made: Int, attempted: Int): Double? {
            return if (attempted > 0) made.toDouble() / attempted * 100.0 else null
        }

        fun parseMinutesPlayed(value: String?): Double? {
            if (value.isNullOrBlank()) return null
            return if (":" in value) {
                val parts = value.split(":")
                val minutes = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
                val seconds = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                minutes + (seconds / 60.0)
            } else {
                value.toDoubleOrNull()
            }
        }
    }

    private data class CachedPlayersPage(
        val players: List<Player>,
        val nextCursor: Int?,
        val cachedAt: Long
    )
}
