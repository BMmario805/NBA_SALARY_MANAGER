package com.example.nba_salary_manager.data.api

import com.example.nba_salary_manager.data.model.Player
import com.example.nba_salary_manager.data.model.PlayerSeasonStats
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private data class EspnAthleteMatch(
    val athleteId: String,
    val displayName: String,
    val teamLabel: String?,
    val isActive: Boolean,
    val score: Int
)

object EspnPlayerStatsRepository {
    private const val NO_MATCH = "__NO_MATCH__"
    private const val SEARCH_BASE_URL =
        "https://site.web.api.espn.com/apis/common/v3/search?region=us&lang=en&limit=8&mode=prefix&type=player&query="
    private const val STATS_PAGE_BASE_URL = "https://www.espn.com/nba/player/stats?id="
    private const val ESPN_SCRIPT_MARKER = "window['__espnfitt__']="
    private const val BASKETBALL_REFERENCE_SEARCH_BASE_URL =
        "https://www.basketball-reference.com/search/search.fcgi?search="
    private const val BASKETBALL_REFERENCE_BASE_URL = "https://www.basketball-reference.com"

    private val athleteIdCache = ConcurrentHashMap<String, String>()
    private val statsCache = ConcurrentHashMap<String, PlayerSeasonStats>()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getPlayerSeasonStats(player: Player): PlayerSeasonStats? = withContext(Dispatchers.IO) {
        val athleteId = resolveAthleteId(player)
        if (athleteId != null) {
            statsCache[athleteId]?.let { return@withContext it }

            val html = getBody("$STATS_PAGE_BASE_URL$athleteId")
            val embeddedJson = extractEmbeddedPageJson(html)
            if (embeddedJson != null) {
                val root = JsonParser().parse(embeddedJson)
                val tables = LinkedHashMap<String, JsonObject>()
                collectTables(root, tables)

                val averagesTable = tables["Regular Season Averages"]
                if (averagesTable != null) {
                    val totalsTable = tables["Regular Season Totals"]
                    val miscTotalsTable = tables["Regular Season Misc Totals"]

                    val summary = buildSummary(
                        averagesTable = averagesTable,
                        totalsTable = totalsTable,
                        miscTotalsTable = miscTotalsTable
                    )
                    if (summary != null) {
                        statsCache[athleteId] = summary
                        return@withContext summary
                    }
                }
            }
        }

        getBasketballReferenceStats(player)
    }

    private fun resolveAthleteId(player: Player): String? {
        val cacheKey = normalizeName("${player.firstName} ${player.lastName}")
        if (cacheKey.isBlank()) return null

        athleteIdCache[cacheKey]?.let { cached ->
            return cached.takeUnless { it == NO_MATCH }
        }

        val encodedQuery = URLEncoder.encode("${player.firstName} ${player.lastName}", StandardCharsets.UTF_8)
        val body = getBody(SEARCH_BASE_URL + encodedQuery)
        val searchRoot = JsonParser().parse(body).asJsonObject
        val items = searchRoot.getAsJsonArray("items") ?: JsonArray()
        val desiredName = normalizeName("${player.firstName} ${player.lastName}")
        val desiredTeam = normalizeName(player.team?.fullName.orEmpty())

        val bestMatch = items.mapNotNull { item ->
            val obj = item.asJsonObject
            val athleteId = obj.getString("id") ?: return@mapNotNull null
            val type = obj.getString("type")
            val league = obj.getString("league")
            val defaultLeagueSlug = obj.getString("defaultLeagueSlug")
            if (type != "player") return@mapNotNull null
            if (league != "nba" && defaultLeagueSlug != "nba") return@mapNotNull null

            val displayName = obj.getString("displayName") ?: return@mapNotNull null
            val teamLabel = obj.getString("label")
            val normalizedDisplayName = normalizeName(displayName)
            val normalizedTeamLabel = normalizeName(teamLabel.orEmpty())
            val isActive = obj.getBoolean("isActive") ?: false

            var score = 0
            if (normalizedDisplayName == desiredName) score += 100
            if (normalizedDisplayName.contains(desiredName) || desiredName.contains(normalizedDisplayName)) {
                score += 25
            }
            if (desiredTeam.isNotBlank() && normalizedTeamLabel.contains(desiredTeam)) {
                score += 20
            }
            if (teamLabel?.contains("NBA", ignoreCase = true) == true) {
                score += 5
            }
            if (isActive) {
                score += 10
            }

            EspnAthleteMatch(
                athleteId = athleteId,
                displayName = displayName,
                teamLabel = teamLabel,
                isActive = isActive,
                score = score
            )
        }.maxWithOrNull(
            compareBy<EspnAthleteMatch> { it.score }
                .thenBy { if (it.isActive) 1 else 0 }
                .thenBy { it.displayName.length * -1 }
        )

        val athleteId = bestMatch?.athleteId
        athleteIdCache[cacheKey] = athleteId ?: NO_MATCH
        return athleteId
    }

    private fun getBody(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/html, */*")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            response.body?.string().orEmpty()
        }
    }

    private fun extractEmbeddedPageJson(html: String): String? {
        val start = html.indexOf(ESPN_SCRIPT_MARKER)
        if (start == -1) return null
        val jsonStart = start + ESPN_SCRIPT_MARKER.length
        val end = html.indexOf(";</script>", jsonStart)
        if (end == -1) return null
        return html.substring(jsonStart, end)
    }

    private fun collectTables(element: JsonElement, tables: MutableMap<String, JsonObject>) {
        when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                if (
                    obj.has("ttl") &&
                    obj.get("ttl").isJsonPrimitive &&
                    obj.has("col") &&
                    obj.has("row")
                ) {
                    tables[obj.get("ttl").asString] = obj
                }
                obj.entrySet().forEach { (_, value) -> collectTables(value, tables) }
            }

            element.isJsonArray -> {
                element.asJsonArray.forEach { collectTables(it, tables) }
            }
        }
    }

    private fun buildSummary(
        averagesTable: JsonObject,
        totalsTable: JsonObject?,
        miscTotalsTable: JsonObject?
    ): PlayerSeasonStats? {
        val averageRows = averagesTable.getAsJsonArray("row") ?: return null
        val latestAverageRow = averageRows.lastOrNull()?.asJsonArray ?: return null

        val seasonText = latestAverageRow.getString(0) ?: return null
        val season = seasonText.take(4).toIntOrNull() ?: return null
        val gamesPlayed = latestAverageRow.getString(2)?.toIntOrNull() ?: return null
        val gamesStarted = latestAverageRow.getString(3)?.toIntOrNull()

        val totalsRow = totalsTable
            ?.getAsJsonArray("row")
            ?.firstOrNull { row -> row.asJsonArray.getString(0) == seasonText }
            ?.asJsonArray

        val miscRow = miscTotalsTable
            ?.getAsJsonArray("row")
            ?.firstOrNull { row -> row.asJsonArray.getString(0) == seasonText }
            ?.asJsonArray

        val fgTotals = totalsRow?.getString(2)?.parseMadeAttemptedInts()
            ?: latestAverageRow.getString(5)?.parseMadeAttemptedDoubles()?.scaleByGames(gamesPlayed)
        val threeTotals = totalsRow?.getString(4)?.parseMadeAttemptedInts()
            ?: latestAverageRow.getString(7)?.parseMadeAttemptedDoubles()?.scaleByGames(gamesPlayed)
        val ftTotals = totalsRow?.getString(6)?.parseMadeAttemptedInts()
            ?: latestAverageRow.getString(9)?.parseMadeAttemptedDoubles()?.scaleByGames(gamesPlayed)

        return PlayerSeasonStats(
            season = season,
            gamesPlayed = gamesPlayed,
            gamesStarted = gamesStarted,
            minutesPerGame = latestAverageRow.getString(4)?.toDoubleOrNull(),
            pointsPerGame = latestAverageRow.getString(19)?.toDoubleOrNull() ?: 0.0,
            reboundsPerGame = latestAverageRow.getString(13)?.toDoubleOrNull() ?: 0.0,
            assistsPerGame = latestAverageRow.getString(14)?.toDoubleOrNull() ?: 0.0,
            stealsPerGame = latestAverageRow.getString(16)?.toDoubleOrNull() ?: 0.0,
            blocksPerGame = latestAverageRow.getString(15)?.toDoubleOrNull() ?: 0.0,
            turnoversPerGame = latestAverageRow.getString(18)?.toDoubleOrNull() ?: 0.0,
            foulsPerGame = latestAverageRow.getString(17)?.toDoubleOrNull() ?: 0.0,
            offensiveReboundsPerGame = latestAverageRow.getString(11)?.toDoubleOrNull() ?: 0.0,
            defensiveReboundsPerGame = latestAverageRow.getString(12)?.toDoubleOrNull() ?: 0.0,
            plusMinusPerGame = null,
            fieldGoalsMade = fgTotals?.first ?: 0,
            fieldGoalsAttempted = fgTotals?.second ?: 0,
            threePointsMade = threeTotals?.first ?: 0,
            threePointsAttempted = threeTotals?.second ?: 0,
            freeThrowsMade = ftTotals?.first ?: 0,
            freeThrowsAttempted = ftTotals?.second ?: 0,
            doubleDoubles = miscRow?.getString(2)?.toIntOrNull(),
            tripleDoubles = miscRow?.getString(3)?.toIntOrNull(),
            assistToTurnoverRatio = miscRow?.getString(8)?.toDoubleOrNull(),
            scoringEfficiency = miscRow?.getString(10)?.toDoubleOrNull(),
            shootingEfficiency = miscRow?.getString(11)?.toDoubleOrNull(),
            fieldGoalPercentage = latestAverageRow.getString(6)?.toDoubleOrNull(),
            threePointPercentage = latestAverageRow.getString(8)?.toDoubleOrNull(),
            freeThrowPercentage = latestAverageRow.getString(10)?.toDoubleOrNull()
        )
    }

    private fun getBasketballReferenceStats(player: Player): PlayerSeasonStats? {
        val query = URLEncoder.encode("${player.firstName} ${player.lastName}", StandardCharsets.UTF_8)
        val searchHtml = getBody(BASKETBALL_REFERENCE_SEARCH_BASE_URL + query)
        val playerPath = findBasketballReferencePlayerPath(searchHtml) ?: return null
        val pageHtml = getBody(BASKETBALL_REFERENCE_BASE_URL + playerPath)
        val latestRow = extractLatestBasketballReferenceRow(pageHtml) ?: return null

        val seasonText = latestRow["year_id"] ?: return null
        val season = seasonText.take(4).toIntOrNull() ?: return null
        val gamesPlayed = latestRow["games"]?.toIntOrNull() ?: return null

        val fgPerGame = latestRow["fg_per_g"]?.toDoubleOrNull() ?: 0.0
        val fgaPerGame = latestRow["fga_per_g"]?.toDoubleOrNull() ?: 0.0
        val fg3PerGame = latestRow["fg3_per_g"]?.toDoubleOrNull() ?: 0.0
        val fg3aPerGame = latestRow["fg3a_per_g"]?.toDoubleOrNull() ?: 0.0
        val ftPerGame = latestRow["ft_per_g"]?.toDoubleOrNull() ?: 0.0
        val ftaPerGame = latestRow["fta_per_g"]?.toDoubleOrNull() ?: 0.0

        return PlayerSeasonStats(
            season = season,
            gamesPlayed = gamesPlayed,
            gamesStarted = latestRow["games_started"]?.toIntOrNull(),
            minutesPerGame = latestRow["mp_per_g"]?.toDoubleOrNull(),
            pointsPerGame = latestRow["pts_per_g"]?.toDoubleOrNull() ?: 0.0,
            reboundsPerGame = latestRow["trb_per_g"]?.toDoubleOrNull() ?: 0.0,
            assistsPerGame = latestRow["ast_per_g"]?.toDoubleOrNull() ?: 0.0,
            stealsPerGame = latestRow["stl_per_g"]?.toDoubleOrNull() ?: 0.0,
            blocksPerGame = latestRow["blk_per_g"]?.toDoubleOrNull() ?: 0.0,
            turnoversPerGame = latestRow["tov_per_g"]?.toDoubleOrNull() ?: 0.0,
            foulsPerGame = latestRow["pf_per_g"]?.toDoubleOrNull() ?: 0.0,
            offensiveReboundsPerGame = latestRow["orb_per_g"]?.toDoubleOrNull() ?: 0.0,
            defensiveReboundsPerGame = latestRow["drb_per_g"]?.toDoubleOrNull() ?: 0.0,
            plusMinusPerGame = null,
            fieldGoalsMade = (fgPerGame * gamesPlayed).toInt(),
            fieldGoalsAttempted = (fgaPerGame * gamesPlayed).toInt(),
            threePointsMade = (fg3PerGame * gamesPlayed).toInt(),
            threePointsAttempted = (fg3aPerGame * gamesPlayed).toInt(),
            freeThrowsMade = (ftPerGame * gamesPlayed).toInt(),
            freeThrowsAttempted = (ftaPerGame * gamesPlayed).toInt(),
            fieldGoalPercentage = latestRow["fg_pct"]?.toDoubleOrNull()?.times(100.0),
            threePointPercentage = latestRow["fg3_pct"]?.toDoubleOrNull()?.times(100.0),
            freeThrowPercentage = latestRow["ft_pct"]?.toDoubleOrNull()?.times(100.0)
        )
    }

    private fun findBasketballReferencePlayerPath(html: String): String? {
        val regex = Regex("""<a href="(/players/[a-z]/[a-z0-9]+\.html)">([^<]+)</a>""")
        return regex.findAll(html)
            .firstOrNull { match ->
                !match.value.contains("international", ignoreCase = true)
            }
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun extractLatestBasketballReferenceRow(html: String): Map<String, String>? {
        val tableMatch = Regex("""<table[^>]*id="per_game_stats"[\s\S]*?</table>""")
            .find(html)
            ?.value
            ?: return null

        val rowRegex = Regex("""<tr id="per_game_stats\.\d+"(?![^>]*partial_table)[^>]*>([\s\S]*?)</tr>""")
        val rows = rowRegex.findAll(tableMatch).toList()
        if (rows.isEmpty()) return null

        val latestRowHtml = rows.last().groupValues[1]
        val cellRegex = Regex("""data-stat="([^"]+)"[^>]*>(?:<a [^>]*>)?([^<]*)""")
        return cellRegex.findAll(latestRowHtml)
            .associate { match ->
                match.groupValues[1] to match.groupValues[2]
                    .replace("&nbsp;", " ")
                    .trim()
            }
    }

    private fun JsonArray.getString(index: Int): String? {
        if (index !in 0 until size()) return null
        val element = get(index)
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            element.asString
        } else {
            null
        }
    }

    private fun JsonObject.getString(name: String): String? {
        val value = get(name) ?: return null
        return if (value.isJsonPrimitive && value.asJsonPrimitive.isString) value.asString else null
    }

    private fun JsonObject.getBoolean(name: String): Boolean? {
        val value = get(name) ?: return null
        return if (value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) value.asBoolean else null
    }

    private fun String.parseMadeAttemptedInts(): Pair<Int, Int>? {
        val parts = split("-")
        if (parts.size != 2) return null
        val made = parts[0].trim().toIntOrNull() ?: return null
        val attempted = parts[1].trim().toIntOrNull() ?: return null
        return made to attempted
    }

    private fun String.parseMadeAttemptedDoubles(): Pair<Double, Double>? {
        val parts = split("-")
        if (parts.size != 2) return null
        val made = parts[0].trim().toDoubleOrNull() ?: return null
        val attempted = parts[1].trim().toDoubleOrNull() ?: return null
        return made to attempted
    }

    private fun Pair<Double, Double>.scaleByGames(games: Int): Pair<Int, Int> {
        return (first * games).toInt() to (second * games).toInt()
    }

    private fun normalizeName(name: String): String {
        val normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        return normalized
            .replace(" iii", " 3")
            .replace(" ii", " 2")
            .replace(" iv", " 4")
    }
}
