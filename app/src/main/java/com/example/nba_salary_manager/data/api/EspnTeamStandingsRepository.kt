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

// Descarga la clasificacion desde ESPN y la adapta al modelo que usa la interfaz.
object EspnTeamStandingsRepository {

    private const val BASE_URL =
        "https://site.web.api.espn.com/apis/v2/sports/basketball/nba/standings" +
            "?region=us&lang=en&contentorigin=espn&type=0&level=1" +
            "&sort=winpercent%3Adesc%2Cwins%3Adesc%2Cgamesbehind%3Aasc"

    suspend fun obtenerClasificacionEquipos(
        anioTemporada: Int,
        equipos: List<Team>
    ): List<TeamStandingSummary> = withContext(Dispatchers.IO) {
        val conexion = (URL("$BASE_URL&season=$anioTemporada").openConnection() as HttpURLConnection)
        conexion.requestMethod = "GET"
        conexion.connectTimeout = 10000
        conexion.readTimeout = 10000
        conexion.setRequestProperty("Accept", "application/json")
        conexion.setRequestProperty("User-Agent", "Mozilla/5.0")

        try {
            val codigoRespuesta = conexion.responseCode
            if (codigoRespuesta !in 200..299) {
                throw IllegalStateException("HTTP $codigoRespuesta")
            }

            val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
            parsearClasificacion(cuerpo, equipos)
        } finally {
            conexion.disconnect()
        }
    }

    // Recorre la respuesta cruda y calcula el ranking general y por conferencia.
    private fun parsearClasificacion(cuerpo: String, equipos: List<Team>): List<TeamStandingSummary> {
        val raiz = JSONObject(cuerpo)
        val entradas = raiz
            .optJSONObject("standings")
            ?.optJSONArray("entries")
            ?: return emptyList()

        val equiposPorAbreviatura = equipos.associateBy { it.abbreviation.uppercase(Locale.US) }
        val entradasCrudas = mutableListOf<EntradaClasificacionCruda>()

        for (indice in 0 until entradas.length()) {
            val entrada = entradas.optJSONObject(indice) ?: continue
            val equipoJson = entrada.optJSONObject("team") ?: continue
            val abreviatura = equipoJson.optString("abbreviation").uppercase(Locale.US)
            val equipo = equiposPorAbreviatura[abreviatura] ?: continue
            val estadisticas = entrada.optJSONArray("stats")

            val victorias = estadisticaEntera(estadisticas, "wins")
            val derrotas = estadisticaEntera(estadisticas, "losses")
            val puntosFavor = estadisticaEntera(estadisticas, "pointsfor")
                .takeIf { it > 0 }
                ?: estimarPuntosFavor(estadisticas, victorias + derrotas)

            entradasCrudas += EntradaClasificacionCruda(
                team = equipo,
                leagueRank = indice + 1,
                wins = victorias,
                losses = derrotas,
                points = puntosFavor
            )
        }

        val rangosConferencia = entradasCrudas
            .groupBy { it.team.conference }
            .flatMap { (_, equiposConferencia) ->
                equiposConferencia
                    .sortedBy { it.leagueRank }
                    .mapIndexed { indice, item -> item.team.id to (indice + 1) }
            }
            .toMap()

        return entradasCrudas
            .map { item ->
                TeamStandingSummary(
                    team = item.team,
                    leagueRank = item.leagueRank,
                    conferenceRank = rangosConferencia[item.team.id] ?: 0,
                    points = item.points,
                    wins = item.wins,
                    losses = item.losses
                )
            }
            .sortedBy { it.team.fullName }
    }

    private fun estadisticaEntera(estadisticas: org.json.JSONArray?, tipo: String): Int {
        return estadisticaTexto(estadisticas, tipo)?.toDoubleOrNull()?.roundToInt() ?: 0
    }

    private fun estimarPuntosFavor(estadisticas: org.json.JSONArray?, partidosJugados: Int): Int {
        val promedio = estadisticaTexto(estadisticas, "avgpointsfor")?.toDoubleOrNull() ?: return 0
        return (promedio * partidosJugados).roundToInt()
    }

    private fun estadisticaTexto(estadisticas: org.json.JSONArray?, tipo: String): String? {
        if (estadisticas == null) return null
        for (indice in 0 until estadisticas.length()) {
            val estadistica = estadisticas.optJSONObject(indice) ?: continue
            if (!estadistica.optString("type").equals(tipo, ignoreCase = true)) continue

            val valor = estadistica.opt("value")
            if (valor != null && valor.toString().isNotBlank()) {
                return valor.toString()
            }

            val resumen = estadistica.optString("summary")
            if (resumen.isNotBlank()) {
                return resumen
            }
        }
        return null
    }

    private data class EntradaClasificacionCruda(
        val team: Team,
        val leagueRank: Int,
        val wins: Int,
        val losses: Int,
        val points: Int
    )
}
