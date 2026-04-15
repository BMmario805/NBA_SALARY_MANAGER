package com.example.nba_salary_manager.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val data: List<T>,
    val meta: Meta? = null
)

data class Meta(
    @SerializedName("next_cursor") val nextCursor: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null
)

data class Team(
    val id: Int,
    val conference: String,
    val division: String,
    val city: String,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val abbreviation: String
)

data class TeamStandingSummary(
    val team: Team,
    val leagueRank: Int,
    val conferenceRank: Int,
    val points: Int,
    val wins: Int,
    val losses: Int
)

data class Player(
    val id: Int,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val position: String?,
    val height: String?,
    val weight: String?,
    @SerializedName("jersey_number") val jerseyNumber: String?,
    val college: String?,
    val country: String?,
    @SerializedName("draft_year") val draftYear: Int?,
    @SerializedName("draft_round") val draftRound: Int?,
    @SerializedName("draft_number") val draftNumber: Int?,
    val team: Team?
)

enum class PlayerPositionFilter(val label: String) {
    ALL("Todas"),
    PG("Base"),
    SG("Escolta"),
    SF("Alero"),
    PF("Ala-pivot"),
    C("Pivot");

    fun matches(position: String?): Boolean {
        if (this == ALL) return true
        if (position.isNullOrBlank()) return false
        val tokens = position
            .uppercase()
            .replace("/", "-")
            .split("-", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return name in tokens
    }
}

enum class PlayerSortOption(val label: String, val requiresStats: Boolean) {
    NAME("Nombre", false),
    POINTS("Puntos", true),
    REBOUNDS("Rebotes", true),
    ASSISTS("Asistencias", true),
    STEALS("Robos", true),
    BLOCKS("Tapones", true),
    MINUTES("Minutos", true)
}

data class Game(
    val id: Int,
    val date: String,
    val season: Int,
    val status: String,
    val period: Int?,
    val time: String?,
    val postseason: Boolean,
    @SerializedName("home_team_score") val homeTeamScore: Int,
    @SerializedName("visitor_team_score") val visitorTeamScore: Int,
    @SerializedName("home_team") val homeTeam: Team,
    @SerializedName("visitor_team") val visitorTeam: Team
)

data class PlayerStats(
    val id: Int,
    val min: String?,
    val pts: Int?,
    val reb: Int?,
    val ast: Int?,
    val stl: Int?,
    val blk: Int?,
    val turnover: Int?,
    val pf: Int?,
    @SerializedName("fg_pct") val fgPct: Double?,
    @SerializedName("fg3_pct") val fg3Pct: Double?,
    @SerializedName("ft_pct") val ftPct: Double?,
    val fgm: Int?,
    val fga: Int?,
    val fg3m: Int?,
    val fg3a: Int?,
    val ftm: Int?,
    val fta: Int?,
    val oreb: Int?,
    val dreb: Int?,
    @SerializedName("plus_minus") val plusMinus: Int?,
    val player: Player?,
    val team: Team?,
    val game: Game?
)

data class PlayerSeasonStats(
    val season: Int,
    val gamesPlayed: Int,
    val gamesStarted: Int? = null,
    val minutesPerGame: Double?,
    val pointsPerGame: Double,
    val reboundsPerGame: Double,
    val assistsPerGame: Double,
    val stealsPerGame: Double,
    val blocksPerGame: Double,
    val turnoversPerGame: Double,
    val foulsPerGame: Double,
    val offensiveReboundsPerGame: Double,
    val defensiveReboundsPerGame: Double,
    val plusMinusPerGame: Double?,
    val fieldGoalsMade: Int,
    val fieldGoalsAttempted: Int,
    val threePointsMade: Int,
    val threePointsAttempted: Int,
    val freeThrowsMade: Int,
    val freeThrowsAttempted: Int,
    val doubleDoubles: Int? = null,
    val tripleDoubles: Int? = null,
    val assistToTurnoverRatio: Double? = null,
    val scoringEfficiency: Double? = null,
    val shootingEfficiency: Double? = null,
    val fieldGoalPercentage: Double?,
    val threePointPercentage: Double?,
    val freeThrowPercentage: Double?
)

data class PlayerStatsUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val summary: PlayerSeasonStats? = null,
    val error: String? = null
)

data class ReplacementCandidate(
    val player: Player,
    val stats: PlayerSeasonStats?,
    val similarityScore: Double,
    val similarityLabel: String
)
