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

private data class CoincidenciaAtletaEspn(
    val idAtleta: String,
    val nombreVisible: String,
    val etiquetaEquipo: String?,
    val estaActivo: Boolean,
    val puntuacion: Int
)

// Intenta sacar las estadisticas desde ESPN y, si fallan o faltan datos,
// recurre a Basketball Reference como respaldo.
object EspnPlayerStatsRepository {
    private const val SIN_COINCIDENCIA = "__NO_MATCH__"
    private const val URL_BASE_BUSQUEDA =
        "https://site.web.api.espn.com/apis/common/v3/search?region=us&lang=en&limit=8&mode=prefix&type=player&query="
    private const val URL_BASE_ESTADISTICAS = "https://www.espn.com/nba/player/stats?id="
    private const val MARCADOR_SCRIPT_ESPN = "window['__espnfitt__']="
    private const val URL_BASE_BUSQUEDA_BASKETBALL_REFERENCE =
        "https://www.basketball-reference.com/search/search.fcgi?search="
    private const val URL_BASE_BASKETBALL_REFERENCE = "https://www.basketball-reference.com"

    private val cacheIdAtleta = ConcurrentHashMap<String, String>()
    private val cacheEstadisticas = ConcurrentHashMap<String, PlayerSeasonStats>()

    private val clienteHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // Primero resuelve el ID del jugador en ESPN. Si la pagina no trae una tabla
    // util o no existe coincidencia, se hace un segundo intento con Basketball Reference.
    suspend fun getPlayerSeasonStats(jugador: Player): PlayerSeasonStats? = withContext(Dispatchers.IO) {
        val idAtleta = resolverIdAtleta(jugador)
        if (idAtleta != null) {
            cacheEstadisticas[idAtleta]?.let { return@withContext it }

            val contenidoHtml = obtenerContenido("$URL_BASE_ESTADISTICAS$idAtleta")
            val jsonEmbebido = extraerJsonEmbebidoPagina(contenidoHtml)
            if (jsonEmbebido != null) {
                val raiz = JsonParser().parse(jsonEmbebido)
                val tablas = LinkedHashMap<String, JsonObject>()
                recopilarTablas(raiz, tablas)

                val tablaPromedios = tablas["Regular Season Averages"]
                if (tablaPromedios != null) {
                    val tablaTotales = tablas["Regular Season Totals"]
                    val tablaTotalesVarios = tablas["Regular Season Misc Totals"]

                    val resumen = construirResumen(
                        averagesTable = tablaPromedios,
                        totalsTable = tablaTotales,
                        miscTotalsTable = tablaTotalesVarios
                    )
                    if (resumen != null) {
                        cacheEstadisticas[idAtleta] = resumen
                        return@withContext resumen
                    }
                }
            }
        }

        obtenerEstadisticasBasketballReference(jugador)
    }

    // Busca el mejor candidato en el buscador de ESPN combinando nombre, equipo y estado activo.
    private fun resolverIdAtleta(jugador: Player): String? {
        val claveCache = normalizarNombre("${jugador.firstName} ${jugador.lastName}")
        if (claveCache.isBlank()) return null

        cacheIdAtleta[claveCache]?.let { valorCache ->
            return valorCache.takeUnless { it == SIN_COINCIDENCIA }
        }

        val consultaCodificada =
            URLEncoder.encode("${jugador.firstName} ${jugador.lastName}", StandardCharsets.UTF_8)
        val cuerpo = obtenerContenido(URL_BASE_BUSQUEDA + consultaCodificada)
        val raizBusqueda = JsonParser().parse(cuerpo).asJsonObject
        val elementos = raizBusqueda.getAsJsonArray("items") ?: JsonArray()
        val nombreDeseado = normalizarNombre("${jugador.firstName} ${jugador.lastName}")
        val equipoDeseado = normalizarNombre(jugador.team?.fullName.orEmpty())

        val mejorCoincidencia = elementos.mapNotNull { item ->
            val objeto = item.asJsonObject
            val idAtleta = objeto.getString("id") ?: return@mapNotNull null
            val tipo = objeto.getString("type")
            val liga = objeto.getString("league")
            val ligaPredeterminada = objeto.getString("defaultLeagueSlug")
            if (tipo != "player") return@mapNotNull null
            if (liga != "nba" && ligaPredeterminada != "nba") return@mapNotNull null

            val nombreVisible = objeto.getString("displayName") ?: return@mapNotNull null
            val etiquetaEquipo = objeto.getString("label")
            val nombreVisibleNormalizado = normalizarNombre(nombreVisible)
            val etiquetaEquipoNormalizada = normalizarNombre(etiquetaEquipo.orEmpty())
            val estaActivo = objeto.getBoolean("isActive") ?: false

            var puntuacion = 0
            if (nombreVisibleNormalizado == nombreDeseado) puntuacion += 100
            if (
                nombreVisibleNormalizado.contains(nombreDeseado) ||
                nombreDeseado.contains(nombreVisibleNormalizado)
            ) {
                puntuacion += 25
            }
            if (equipoDeseado.isNotBlank() && etiquetaEquipoNormalizada.contains(equipoDeseado)) {
                puntuacion += 20
            }
            if (etiquetaEquipo?.contains("NBA", ignoreCase = true) == true) {
                puntuacion += 5
            }
            if (estaActivo) {
                puntuacion += 10
            }

            CoincidenciaAtletaEspn(
                idAtleta = idAtleta,
                nombreVisible = nombreVisible,
                etiquetaEquipo = etiquetaEquipo,
                estaActivo = estaActivo,
                puntuacion = puntuacion
            )
        }.maxWithOrNull(
            compareBy<CoincidenciaAtletaEspn> { it.puntuacion }
                .thenBy { if (it.estaActivo) 1 else 0 }
                .thenBy { it.nombreVisible.length * -1 }
        )

        val idAtleta = mejorCoincidencia?.idAtleta
        cacheIdAtleta[claveCache] = idAtleta ?: SIN_COINCIDENCIA
        return idAtleta
    }

    private fun obtenerContenido(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json, text/html, */*")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()

        return clienteHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            response.body?.string().orEmpty()
        }
    }

    private fun extraerJsonEmbebidoPagina(html: String): String? {
        val inicio = html.indexOf(MARCADOR_SCRIPT_ESPN)
        if (inicio == -1) return null
        val inicioJson = inicio + MARCADOR_SCRIPT_ESPN.length
        val fin = html.indexOf(";</script>", inicioJson)
        if (fin == -1) return null
        return html.substring(inicioJson, fin)
    }

    private fun recopilarTablas(elemento: JsonElement, tablas: MutableMap<String, JsonObject>) {
        when {
            elemento.isJsonObject -> {
                val objeto = elemento.asJsonObject
                if (
                    objeto.has("ttl") &&
                    objeto.get("ttl").isJsonPrimitive &&
                    objeto.has("col") &&
                    objeto.has("row")
                ) {
                    tablas[objeto.get("ttl").asString] = objeto
                }
                objeto.entrySet().forEach { (_, valor) -> recopilarTablas(valor, tablas) }
            }

            elemento.isJsonArray -> {
                elemento.asJsonArray.forEach { recopilarTablas(it, tablas) }
            }
        }
    }

    private fun construirResumen(
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

    private fun obtenerEstadisticasBasketballReference(jugador: Player): PlayerSeasonStats? {
        val consulta = URLEncoder.encode("${jugador.firstName} ${jugador.lastName}", StandardCharsets.UTF_8)
        val htmlBusqueda = obtenerContenido(URL_BASE_BUSQUEDA_BASKETBALL_REFERENCE + consulta)
        val rutaJugador = buscarRutaJugadorBasketballReference(htmlBusqueda) ?: return null
        val htmlPagina = obtenerContenido(URL_BASE_BASKETBALL_REFERENCE + rutaJugador)
        val ultimaFila = extraerUltimaFilaBasketballReference(htmlPagina) ?: return null

        val textoTemporada = ultimaFila["year_id"] ?: return null
        val temporada = textoTemporada.take(4).toIntOrNull() ?: return null
        val partidosJugados = ultimaFila["games"]?.toIntOrNull() ?: return null

        val tirosCampoPorPartido = ultimaFila["fg_per_g"]?.toDoubleOrNull() ?: 0.0
        val intentosCampoPorPartido = ultimaFila["fga_per_g"]?.toDoubleOrNull() ?: 0.0
        val triplesPorPartido = ultimaFila["fg3_per_g"]?.toDoubleOrNull() ?: 0.0
        val intentosTriplePorPartido = ultimaFila["fg3a_per_g"]?.toDoubleOrNull() ?: 0.0
        val tirosLibresPorPartido = ultimaFila["ft_per_g"]?.toDoubleOrNull() ?: 0.0
        val intentosLibrePorPartido = ultimaFila["fta_per_g"]?.toDoubleOrNull() ?: 0.0

        return PlayerSeasonStats(
            season = temporada,
            gamesPlayed = partidosJugados,
            gamesStarted = ultimaFila["games_started"]?.toIntOrNull(),
            minutesPerGame = ultimaFila["mp_per_g"]?.toDoubleOrNull(),
            pointsPerGame = ultimaFila["pts_per_g"]?.toDoubleOrNull() ?: 0.0,
            reboundsPerGame = ultimaFila["trb_per_g"]?.toDoubleOrNull() ?: 0.0,
            assistsPerGame = ultimaFila["ast_per_g"]?.toDoubleOrNull() ?: 0.0,
            stealsPerGame = ultimaFila["stl_per_g"]?.toDoubleOrNull() ?: 0.0,
            blocksPerGame = ultimaFila["blk_per_g"]?.toDoubleOrNull() ?: 0.0,
            turnoversPerGame = ultimaFila["tov_per_g"]?.toDoubleOrNull() ?: 0.0,
            foulsPerGame = ultimaFila["pf_per_g"]?.toDoubleOrNull() ?: 0.0,
            offensiveReboundsPerGame = ultimaFila["orb_per_g"]?.toDoubleOrNull() ?: 0.0,
            defensiveReboundsPerGame = ultimaFila["drb_per_g"]?.toDoubleOrNull() ?: 0.0,
            plusMinusPerGame = null,
            fieldGoalsMade = (tirosCampoPorPartido * partidosJugados).toInt(),
            fieldGoalsAttempted = (intentosCampoPorPartido * partidosJugados).toInt(),
            threePointsMade = (triplesPorPartido * partidosJugados).toInt(),
            threePointsAttempted = (intentosTriplePorPartido * partidosJugados).toInt(),
            freeThrowsMade = (tirosLibresPorPartido * partidosJugados).toInt(),
            freeThrowsAttempted = (intentosLibrePorPartido * partidosJugados).toInt(),
            fieldGoalPercentage = ultimaFila["fg_pct"]?.toDoubleOrNull()?.times(100.0),
            threePointPercentage = ultimaFila["fg3_pct"]?.toDoubleOrNull()?.times(100.0),
            freeThrowPercentage = ultimaFila["ft_pct"]?.toDoubleOrNull()?.times(100.0)
        )
    }

    private fun buscarRutaJugadorBasketballReference(html: String): String? {
        val patron = Regex("""<a href="(/players/[a-z]/[a-z0-9]+\.html)">([^<]+)</a>""")
        return patron.findAll(html)
            .firstOrNull { match ->
                !match.value.contains("international", ignoreCase = true)
            }
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun extraerUltimaFilaBasketballReference(html: String): Map<String, String>? {
        val coincidenciaTabla = Regex("""<table[^>]*id="per_game_stats"[\s\S]*?</table>""")
            .find(html)
            ?.value
            ?: return null

        val patronFila = Regex("""<tr id="per_game_stats\.\d+"(?![^>]*partial_table)[^>]*>([\s\S]*?)</tr>""")
        val filas = patronFila.findAll(coincidenciaTabla).toList()
        if (filas.isEmpty()) return null

        val htmlUltimaFila = filas.last().groupValues[1]
        val patronCelda = Regex("""data-stat="([^"]+)"[^>]*>(?:<a [^>]*>)?([^<]*)""")
        return patronCelda.findAll(htmlUltimaFila)
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

    private fun Pair<Double, Double>.scaleByGames(partidos: Int): Pair<Int, Int> {
        return (first * partidos).toInt() to (second * partidos).toInt()
    }

    private fun normalizarNombre(nombre: String): String {
        val nombreNormalizado = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        return nombreNormalizado
            .replace(" iii", " 3")
            .replace(" ii", " 2")
            .replace(" iv", " 4")
    }
}
