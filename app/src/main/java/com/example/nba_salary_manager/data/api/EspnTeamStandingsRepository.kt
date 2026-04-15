package com.example.nba_salary_manager.data.api

import com.example.nba_salary_manager.data.model.Team
import com.example.nba_salary_manager.data.model.TeamStandingSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

object EspnTeamStandingsRepository {

    private const val BASE_URL =
        "https://site.web.api.espn.com/apis/v2/sports/basketball/nba/standings" +
            "?region=us&lang=en&contentorigin=espn&type=0&level=1" +
            "&sort=winpercent%3Adesc%2Cwins%3Adesc%2Cgamesbehind%3Aasc"

    suspend fun getTeamStandings(
        seasonYear: Int,
        teams: List<Team>
    ): List<TeamStandingSummary> = withContext(Dispatchers.IO) {
        val connection = (URL("$BASE_URL&season=$seasonYear").openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseStandings(body, teams)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseStandings(body: String, teams: List<Team>): List<TeamStandingSummary> {
        val root = JSONObject(body)
        val entries = root
            .optJSONObject("standings")
            ?.optJSONArray("entries")
            ?: return emptyList()

        val teamsByAbbreviation = teams.associateBy { it.abbreviation.uppercase(Locale.US) }
        val rawEntries = mutableListOf<RawStandingEntry>()

        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            val teamJson = entry.optJSONObject("team") ?: continue
            val abbreviation = teamJson.optString("abbreviation").uppercase(Locale.US)
            val team = teamsByAbbreviation[abbreviation] ?: continue
            val stats = entry.optJSONArray("stats")

            val wins = statInt(stats, "wins")
            val losses = statInt(stats, "losses")
            val pointsFor = statInt(stats, "pointsfor")
                .takeIf { it > 0 }
                ?: estimatedPointsFor(stats, wins + losses)

            rawEntries += RawStandingEntry(
                team = team,
                leagueRank = index + 1,
                wins = wins,
                losses = losses,
                points = pointsFor
            )
        }

        val conferenceRanks = rawEntries
            .groupBy { it.team.conference }
            .flatMap { (_, conferenceTeams) ->
                conferenceTeams
                    .sortedBy { it.leagueRank }
                    .mapIndexed { index, item -> item.team.id to (index + 1) }
            }
            .toMap()

        return rawEntries
            .map { item ->
                TeamStandingSummary(
                    team = item.team,
                    leagueRank = item.leagueRank,
                    conferenceRank = conferenceRanks[item.team.id] ?: 0,
                    points = item.points,
                    wins = item.wins,
                    losses = item.losses
                )
            }
            .sortedBy { it.team.fullName }
    }

    private fun statInt(stats: org.json.JSONArray?, type: String): Int {
        return statString(stats, type)?.toDoubleOrNull()?.roundToInt() ?: 0
    }

    private fun estimatedPointsFor(stats: org.json.JSONArray?, gamesPlayed: Int): Int {
        val average = statString(stats, "avgpointsfor")?.toDoubleOrNull() ?: return 0
        return (average * gamesPlayed).roundToInt()
    }

    private fun statString(stats: org.json.JSONArray?, type: String): String? {
        if (stats == null) return null
        for (i in 0 until stats.length()) {
            val stat = stats.optJSONObject(i) ?: continue
            if (!stat.optString("type").equals(type, ignoreCase = true)) continue

            val value = stat.opt("value")
            if (value != null && value.toString().isNotBlank()) {
                return value.toString()
            }

            val summary = stat.optString("summary")
            if (summary.isNotBlank()) {
                return summary
            }
        }
        return null
    }

    private data class RawStandingEntry(
        val team: Team,
        val leagueRank: Int,
        val wins: Int,
        val losses: Int,
        val points: Int
    )
}
