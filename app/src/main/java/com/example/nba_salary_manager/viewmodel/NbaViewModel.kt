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

    private val catalogoPlantillas = RosterTemplateRepository.templates
    private val preferenciasCache = appContext.getSharedPreferences("nba_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Teams state ──
    // Estado que alimenta la pantalla de equipos y su clasificacion.
    var equipos by mutableStateOf<List<Team>>(emptyList())
        private set
    var cargandoEquipos by mutableStateOf(false)
        private set
    var errorEquipos by mutableStateOf<String?>(null)
        private set
    var cargandoEstadisticasEquipos by mutableStateOf(false)
        private set
    var errorEstadisticasEquipos by mutableStateOf<String?>(null)
        private set
    var clasificacionEquipos by mutableStateOf<List<TeamStandingSummary>>(emptyList())
        private set

    // ── Players state ──
    // Estado para busqueda, orden y detalle estadistico de jugadores.
    var jugadores by mutableStateOf<List<Player>>(emptyList())
        private set
    var cargandoJugadores by mutableStateOf(false)
        private set
    var errorJugadores by mutableStateOf<String?>(null)
        private set
    var busquedaJugador by mutableStateOf("")
        private set
    var cursorSiguienteJugadores by mutableStateOf<Int?>(null)
        private set
    var hayMasJugadores by mutableStateOf(true)
        private set
    var estadosEstadisticasJugadores by mutableStateOf<Map<Int, PlayerStatsUiState>>(emptyMap())
        private set
    var filtroPosicionJugadorSeleccionado by mutableStateOf(PlayerPositionFilter.ALL)
        private set
    var ordenJugadorSeleccionado by mutableStateOf(PlayerSortOption.NAME)
        private set
    var estadisticasRankingTemporada by mutableStateOf<Map<Int, PlayerSeasonStats>>(emptyMap())
        private set
    private var jugadoresRankingTemporada by mutableStateOf<List<Player>>(emptyList())
    private var temporadaRankingTemporada by mutableStateOf<Int?>(null)

    // ── Games state ──
    // Estado para filtros y paginacion de partidos.
    var partidos by mutableStateOf<List<Game>>(emptyList())
        private set
    var cargandoPartidos by mutableStateOf(false)
        private set
    var errorPartidos by mutableStateOf<String?>(null)
        private set
    var cursorSiguientePartidos by mutableStateOf<Int?>(null)
        private set
    var hayMasPartidos by mutableStateOf(true)
        private set
    var anioPartidosSeleccionado by mutableStateOf<Int?>(null)
        private set
    var mesPartidosSeleccionado by mutableStateOf<Int?>(null)
        private set

    var categoriaPlantillaSeleccionada by mutableStateOf(RosterTemplateCategory.CURRENT)
        private set
    var idPlantillaActiva by mutableStateOf(catalogoPlantillas.firstOrNull()?.id)
        private set
    var nombrePlantillaEditable by mutableStateOf(catalogoPlantillas.firstOrNull()?.teamName.orEmpty())
        private set
    var estiloPlantillaEditable by mutableStateOf(catalogoPlantillas.firstOrNull()?.playStyle.orEmpty())
        private set
    var jugadoresPlantillaEditable by mutableStateOf(
        catalogoPlantillas.firstOrNull()?.jugadores ?: emptyList()
    )
        private set
    var candidatosReemplazo by mutableStateOf<List<ReplacementCandidate>>(emptyList())
        private set
    var cargandoCandidatosReemplazo by mutableStateOf(false)
        private set
    var errorCandidatosReemplazo by mutableStateOf<String?>(null)
        private set
    var estadisticasObjetivoReemplazo by mutableStateOf<PlayerSeasonStats?>(null)
        private set
    var posicionObjetivoReemplazo by mutableStateOf("")
        private set

    private val servicioApi = RetrofitClient.servicioApi
    private val duracionCacheJugadoresMs = 1000L * 60L * 30L

    val plantillasBase: List<RosterTemplate>
        get() = catalogoPlantillas

    val plantillasFiltradas: List<RosterTemplate>
        get() = catalogoPlantillas.filter { it.category == categoriaPlantillaSeleccionada }

    val plantillaActiva: RosterTemplate?
        get() = catalogoPlantillas.firstOrNull { it.id == idPlantillaActiva }

    val aniosPartidosDisponibles: List<Int>
        get() = (Calendar.getInstance().get(Calendar.YEAR) downTo 1970).toList()

    val mesesPartidosDisponibles: List<Int>
        get() = (1..12).toList()

    val jugadoresVisibles: List<Player>
        get() = fuenteJugadoresModoActual()
            .filter { jugador -> filtroPosicionJugadorSeleccionado.matches(jugador.position) }
            .sortedWith(comparadorJugadores(ordenJugadorSeleccionado))

    val puedeCargarMasJugadores: Boolean
        get() = !debeUsarRankingTemporada() && hayMasJugadores

    fun actualizarCategoriaPlantilla(categoria: RosterTemplateCategory) {
        categoriaPlantillaSeleccionada = categoria
    }

    // Copia la plantilla elegida al editor para poder ajustarla sin tocar la base.
    fun usarPlantillaBase(idPlantilla: String) {
        val plantilla = catalogoPlantillas.firstOrNull { it.id == idPlantilla } ?: return
        idPlantillaActiva = plantilla.id
        categoriaPlantillaSeleccionada = plantilla.category
        nombrePlantillaEditable = plantilla.teamName
        estiloPlantillaEditable = plantilla.playStyle
        jugadoresPlantillaEditable = plantilla.jugadores.map { it.copy() }
        limpiarCandidatosReemplazo()
    }

    fun actualizarNombrePlantillaEditable(nombre: String) {
        nombrePlantillaEditable = nombre
    }

    fun actualizarEstiloPlantillaEditable(estilo: String) {
        estiloPlantillaEditable = estilo
    }

    fun actualizarJugadorPlantillaEditable(
        index: Int,
        transform: (RosterPlayerTemplate) -> RosterPlayerTemplate
    ) {
        if (index !in jugadoresPlantillaEditable.indices) return
        val jugadoresActualizados = jugadoresPlantillaEditable.toMutableList()
        jugadoresActualizados[index] = transform(jugadoresActualizados[index])
        jugadoresPlantillaEditable = jugadoresActualizados
    }

    // Anade un hueco vacio para construir una plantilla personalizada desde cero.
    fun agregarJugadorPlantillaEditable() {
        jugadoresPlantillaEditable = jugadoresPlantillaEditable + RosterPlayerTemplate(
            name = "",
            position = "",
            role = "Rotacion",
            salaryTier = "",
            note = ""
        )
    }

    fun eliminarJugadorPlantillaEditable(index: Int) {
        if (index !in jugadoresPlantillaEditable.indices) return
        jugadoresPlantillaEditable = jugadoresPlantillaEditable.filterIndexed { currentIndex, _ ->
            currentIndex != index
        }
    }

    fun limpiarCandidatosReemplazo() {
        candidatosReemplazo = emptyList()
        cargandoCandidatosReemplazo = false
        errorCandidatosReemplazo = null
        estadisticasObjetivoReemplazo = null
        posicionObjetivoReemplazo = ""
    }

    // Busca jugadores reales que se parezcan al perfil del hueco seleccionado.
    fun cargarCandidatosReemplazo(jugadorObjetivo: RosterPlayerTemplate) {
        viewModelScope.launch {
            cargandoCandidatosReemplazo = true
            errorCandidatosReemplazo = null
            candidatosReemplazo = emptyList()
            posicionObjetivoReemplazo = jugadorObjetivo.position

            try {
                val jugadorBase = plantillaAJugador(jugadorObjetivo)
                estadisticasObjetivoReemplazo =
                    jugadorBase?.let { EspnPlayerStatsRepository.getPlayerSeasonStats(it) }
                val grupoJugadores = obtenerGrupoReemplazo(jugadorObjetivo)
                candidatosReemplazo = ordenarCandidatosReemplazo(
                    jugadorObjetivo = jugadorObjetivo,
                    estadisticasObjetivo = estadisticasObjetivoReemplazo,
                    grupoJugadores = grupoJugadores
                )

                if (candidatosReemplazo.isEmpty()) {
                    errorCandidatosReemplazo =
                        "No se encontraron candidatos similares para ${jugadorObjetivo.position}."
                }
            } catch (e: Exception) {
                errorCandidatosReemplazo = e.message ?: "Error buscando reemplazos"
            }

            cargandoCandidatosReemplazo = false
        }
    }

    fun reemplazarJugadorPlantillaEditable(index: Int, jugador: Player) {
        actualizarJugadorPlantillaEditable(index) { actual ->
            actual.copy(
                name = "${jugador.firstName} ${jugador.lastName}",
                nbaPlayerId = jugador.id,
                position = actual.position
            )
        }
    }

    fun cargarEstadisticasPlantillaEditable(forzarRecarga: Boolean = false) {
        jugadoresPlantillaEditable.forEach { jugador ->
            cargarEstadisticasJugadorPlantilla(jugador, forzarRecarga = forzarRecarga)
        }
    }

    fun cargarEstadisticasJugadorPlantilla(
        jugador: RosterPlayerTemplate,
        forzarRecarga: Boolean = false
    ) {
        val jugadorPlantilla = plantillaAJugador(jugador) ?: return
        cargarEstadisticasJugador(jugadorPlantilla, forzarRecarga = forzarRecarga)
    }

    fun estadoEstadisticasJugadorPlantilla(jugador: RosterPlayerTemplate): PlayerStatsUiState {
        val jugadorPlantilla = plantillaAJugador(jugador) ?: return PlayerStatsUiState()
        return estadoEstadisticasJugador(jugadorPlantilla.id)
    }

    // ── Teams ──

    // Carga la lista base de equipos y reinicia la clasificacion derivada.
    fun cargarEquipos() {
        if (cargandoEquipos) return

        viewModelScope.launch {
            cargandoEquipos = true
            errorEquipos = null
            cargandoEstadisticasEquipos = false
            errorEstadisticasEquipos = null
            try {
                val respuesta = servicioApi.obtenerEquipos()
                val equiposCargados = respuesta.data
                    .filter { it.abbreviation in currentNbaTeamAbbreviations }
                    .distinctBy { it.id }
                    .sortedBy { it.fullName }
                equipos = equiposCargados
                clasificacionEquipos = emptyList()
            } catch (e: Exception) {
                errorEquipos = mensajeErrorApi(e, "Error desconocido al cargar equipos")
                clasificacionEquipos = emptyList()
            }
            cargandoEquipos = false
        }
    }

    fun cargarClasificacionEquiposSiHaceFalta() {
        if (cargandoEstadisticasEquipos || clasificacionEquipos.isNotEmpty() || equipos.isEmpty()) return

        viewModelScope.launch {
            cargandoEstadisticasEquipos = true
            errorEstadisticasEquipos = null
            try {
                clasificacionEquipos = EspnTeamStandingsRepository.obtenerClasificacionEquipos(
                    anioTemporada = temporadaEspnActual(),
                    equipos = equipos
                )
            } catch (e: Exception) {
                errorEstadisticasEquipos = mensajeErrorApi(e, "Error desconocido al cargar estadisticas de equipos")
                clasificacionEquipos = emptyList()
            }
            cargandoEstadisticasEquipos = false
        }
    }

    // ── Players ──

    // Mantiene sincronizada la consulta de texto usada por la pantalla de jugadores.
    fun actualizarBusquedaJugador(consulta: String) {
        busquedaJugador = consulta
    }

    fun actualizarFiltroPosicionJugador(filtro: PlayerPositionFilter) {
        filtroPosicionJugadorSeleccionado = filtro
        precargarEstadisticasSegunOrden()
    }

    fun actualizarOrdenJugador(criterioOrden: PlayerSortOption) {
        ordenJugadorSeleccionado = criterioOrden
        precargarEstadisticasSegunOrden()
    }

    fun resumenJugadorParaOrdenar(idJugador: Int): PlayerSeasonStats? {
        return estadisticasRankingTemporada[idJugador] ?: estadosEstadisticasJugadores[idJugador]?.summary
    }

    // Decide si usa cache local, ranking agregado o consulta directa a la API.
    fun buscarJugadores() {
        if (debeUsarRankingTemporada()) {
            cargarRankingTemporada(forzarRecarga = true)
            return
        }
        if (cargandoJugadores) return
        val consulta = busquedaJugador.trim()
        val paginaCache = leerPaginaJugadoresCache(consulta)
        if (paginaCache != null && cacheSigueVigente(paginaCache)) {
            jugadores = paginaCache.jugadores.sortedByDescending { it.id }
            cursorSiguienteJugadores = paginaCache.nextCursor
            hayMasJugadores = paginaCache.nextCursor != null
            errorJugadores = null
            precargarEstadisticasSegunOrden()
            return
        }

        viewModelScope.launch {
            cargandoJugadores = true
            errorJugadores = null
            cursorSiguienteJugadores = null
            hayMasJugadores = true
            try {
                val respuesta = servicioApi.obtenerJugadores(
                    search = consulta.ifBlank { null },
                    perPage = 15
                )
                jugadores = respuesta.data.sortedByDescending { it.id }
                cursorSiguienteJugadores = respuesta.meta?.nextCursor
                hayMasJugadores = respuesta.meta?.nextCursor != null
                guardarPaginaJugadoresCache(
                    consulta = consulta,
                    jugadores = jugadores,
                    nextCursor = cursorSiguienteJugadores
                )
                precargarEstadisticasSegunOrden()
            } catch (e: Exception) {
                if (paginaCache != null) {
                    jugadores = paginaCache.jugadores.sortedByDescending { it.id }
                    cursorSiguienteJugadores = paginaCache.nextCursor
                    hayMasJugadores = paginaCache.nextCursor != null
                    errorJugadores = null
                    precargarEstadisticasSegunOrden()
                } else {
                    errorJugadores = mensajeErrorApi(e, "Error desconocido al cargar jugadores")
                }
            }
            cargandoJugadores = false
        }
    }

    fun cargarJugadores() {
        buscarJugadores()
    }

    fun cargarMasJugadores() {
        if (debeUsarRankingTemporada()) return
        val cursor = cursorSiguienteJugadores ?: return
        if (cargandoJugadores) return
        viewModelScope.launch {
            cargandoJugadores = true
            try {
                val respuesta = servicioApi.obtenerJugadores(
                    search = busquedaJugador.ifBlank { null },
                    perPage = 15,
                    cursor = cursor
                )
                jugadores = (jugadores + respuesta.data).sortedByDescending { it.id }
                cursorSiguienteJugadores = respuesta.meta?.nextCursor
                hayMasJugadores = respuesta.meta?.nextCursor != null
                guardarPaginaJugadoresCache(
                    consulta = busquedaJugador.trim(),
                    jugadores = jugadores,
                    nextCursor = cursorSiguienteJugadores
                )
                precargarEstadisticasSegunOrden()
            } catch (e: Exception) {
                errorJugadores = e.message ?: "Error cargando mas jugadores"
            }
            cargandoJugadores = false
        }
    }

    // ── Games ──

    // Actualiza la ventana temporal de partidos segun los filtros activos.
    fun actualizarFiltroAnioPartidos(anio: Int?) {
        anioPartidosSeleccionado = anio
        cargarPartidos()
    }

    fun actualizarFiltroMesPartidos(mes: Int?) {
        if (mes == null) {
            mesPartidosSeleccionado = null
            anioPartidosSeleccionado = null
        } else {
            mesPartidosSeleccionado = mes
            if (anioPartidosSeleccionado == null) {
                anioPartidosSeleccionado = Calendar.getInstance().get(Calendar.YEAR)
            }
        }
        cargarPartidos()
    }

    fun limpiarFiltrosPartidos() {
        anioPartidosSeleccionado = null
        mesPartidosSeleccionado = null
        cargarPartidos()
    }

    // Consulta los partidos visibles en pantalla usando el rango ya calculado.
    fun cargarPartidos() {
        if (cargandoPartidos) return

        viewModelScope.launch {
            cargandoPartidos = true
            errorPartidos = null
            cursorSiguientePartidos = null
            hayMasPartidos = true
            partidos = emptyList()
            try {
                val (fechaInicio, fechaFin) = obtenerRangoFechasPartidos()

                val respuesta = servicioApi.obtenerPartidos(
                    perPage = 50,
                    startDate = fechaInicio,
                    endDate = fechaFin
                )
                partidos = respuesta.data.sortedBy { it.date }
                cursorSiguientePartidos = respuesta.meta?.nextCursor
                hayMasPartidos = respuesta.meta?.nextCursor != null
            } catch (e: Exception) {
                errorPartidos = mensajeErrorApi(e, "Error desconocido al cargar partidos")
            }
            cargandoPartidos = false
        }
    }

    fun cargarMasPartidos() {
        val cursor = cursorSiguientePartidos ?: return
        if (cargandoPartidos) return
        viewModelScope.launch {
            cargandoPartidos = true
            try {
                val (fechaInicio, fechaFin) = obtenerRangoFechasPartidos()

                val respuesta = servicioApi.obtenerPartidos(
                    perPage = 50,
                    cursor = cursor,
                    startDate = fechaInicio,
                    endDate = fechaFin
                )
                partidos = (partidos + respuesta.data).sortedBy { it.date }
                cursorSiguientePartidos = respuesta.meta?.nextCursor
                hayMasPartidos = respuesta.meta?.nextCursor != null
            } catch (e: Exception) {
                errorPartidos = e.message ?: "Error cargando mas partidos"
            }
            cargandoPartidos = false
        }
    }

    fun estadoEstadisticasJugador(playerId: Int): PlayerStatsUiState {
        return estadosEstadisticasJugadores[playerId] ?: PlayerStatsUiState()
    }

    fun cargarEstadisticasJugador(jugador: Player, forzarRecarga: Boolean = false) {
        val idJugador = jugador.id
        val estadoActual = estadoEstadisticasJugador(idJugador)
        if (estadoActual.isLoading) return
        if (estadoActual.hasLoaded && !forzarRecarga) return

        viewModelScope.launch {
            estadosEstadisticasJugadores = estadosEstadisticasJugadores + (
                idJugador to estadoActual.copy(
                    isLoading = true,
                    error = null
                )
            )

            try {
                val resumen = EspnPlayerStatsRepository.getPlayerSeasonStats(jugador)
                estadosEstadisticasJugadores = estadosEstadisticasJugadores + (
                    idJugador to PlayerStatsUiState(
                        isLoading = false,
                        hasLoaded = true,
                        summary = resumen,
                        error = null
                    )
                )
            } catch (e: Exception) {
                estadosEstadisticasJugadores = estadosEstadisticasJugadores + (
                    idJugador to estadoActual.copy(
                        isLoading = false,
                        error = mensajeErrorEstadisticasJugador(e)
                    )
                )
            }
        }
    }

    // Reune una muestra amplia de jugadores compatibles para luego ordenarlos por parecido.
    private suspend fun obtenerGrupoReemplazo(jugadorObjetivo: RosterPlayerTemplate): List<Player> {
        val jugadoresRecogidos = linkedMapOf<Int, Player>()
        var cursor: Int? = null
        var pagina = 0

        while (pagina < 8 && jugadoresRecogidos.size < 36) {
            val respuesta = servicioApi.obtenerJugadores(
                perPage = 100,
                cursor = cursor
            )

            respuesta.data
                .filter { candidato ->
                    candidato.id != jugadorObjetivo.nbaPlayerId &&
                        candidato.team != null &&
                        esReemplazoCompatible(jugadorObjetivo.position, candidato.position.orEmpty())
                }
                .forEach { candidato ->
                    jugadoresRecogidos.putIfAbsent(candidato.id, candidato)
                }

            cursor = respuesta.meta?.nextCursor
            if (cursor == null) break
            pagina++
        }

        return jugadoresRecogidos.values.toList()
    }

    // Pide estadisticas de varios candidatos en paralelo y se queda con los mas cercanos.
    private suspend fun ordenarCandidatosReemplazo(
        jugadorObjetivo: RosterPlayerTemplate,
        estadisticasObjetivo: PlayerSeasonStats?,
        grupoJugadores: List<Player>
    ): List<ReplacementCandidate> = coroutineScope {
        grupoJugadores.take(18).map { candidato ->
            async {
                val estadisticas = runCatching {
                    EspnPlayerStatsRepository.getPlayerSeasonStats(candidato)
                }.getOrNull()

                ReplacementCandidate(
                    player = candidato,
                    stats = estadisticas,
                    similarityScore = calcularPuntuacionSimilitud(
                        posicionHueco = jugadorObjetivo.position,
                        estadisticasObjetivo = estadisticasObjetivo,
                        estadisticasCandidato = estadisticas
                    ),
                    similarityLabel = construirEtiquetaSimilitud(
                        posicionHueco = jugadorObjetivo.position,
                        estadisticasObjetivo = estadisticasObjetivo,
                        estadisticasCandidato = estadisticas
                    )
                )
            }
        }.awaitAll()
            .sortedBy { it.similarityScore }
            .take(8)
    }

    private fun calcularPuntuacionSimilitud(
        posicionHueco: String,
        estadisticasObjetivo: PlayerSeasonStats?,
        estadisticasCandidato: PlayerSeasonStats?
    ): Double {
        if (estadisticasObjetivo == null || estadisticasCandidato == null) return 9999.0

        val posicion = posicionPrincipal(posicionHueco)
        val pesoPuntos = when (posicion) {
            "PG", "SG", "SF" -> 3.2
            else -> 2.4
        }
        val pesoRebotes = if (posicion == "C" || posicion == "PF") 3.1 else 1.8
        val pesoAsistencias = if (posicion == "PG") 3.3 else 1.9
        val pesoTapones = if (posicion == "C" || posicion == "PF") 2.7 else 0.9
        val pesoTriples = if (posicion == "PG" || posicion == "SG" || posicion == "SF") 2.1 else 1.1

        var puntuacion = 0.0
        puntuacion += abs(estadisticasObjetivo.pointsPerGame - estadisticasCandidato.pointsPerGame) * pesoPuntos
        puntuacion += abs(estadisticasObjetivo.reboundsPerGame - estadisticasCandidato.reboundsPerGame) * pesoRebotes
        puntuacion += abs(estadisticasObjetivo.assistsPerGame - estadisticasCandidato.assistsPerGame) * pesoAsistencias
        puntuacion += abs(estadisticasObjetivo.blocksPerGame - estadisticasCandidato.blocksPerGame) * pesoTapones
        puntuacion += abs(estadisticasObjetivo.threePointsMade.toDouble() - estadisticasCandidato.threePointsMade.toDouble()) / 20.0 * pesoTriples
        puntuacion += abs((estadisticasObjetivo.threePointPercentage ?: 0.0) - (estadisticasCandidato.threePointPercentage ?: 0.0)) * 0.18
        puntuacion += abs((estadisticasObjetivo.minutesPerGame ?: 0.0) - (estadisticasCandidato.minutesPerGame ?: 0.0)) * 0.6
        return puntuacion
    }

    private fun construirEtiquetaSimilitud(
        posicionHueco: String,
        estadisticasObjetivo: PlayerSeasonStats?,
        estadisticasCandidato: PlayerSeasonStats?
    ): String {
        if (estadisticasObjetivo == null || estadisticasCandidato == null) {
            return "Misma posicion, sin comparativa completa"
        }

        val puntuacion = calcularPuntuacionSimilitud(
            posicionHueco,
            estadisticasObjetivo,
            estadisticasCandidato
        )
        return when {
            puntuacion < 18 -> "Perfil muy parecido"
            puntuacion < 30 -> "Encaje similar"
            else -> "Alternativa de misma posicion"
        }
    }

    private fun esReemplazoCompatible(posicionHueco: String, posicionCandidato: String): Boolean {
        val posicionBase = posicionPrincipal(posicionHueco)
        val tokensPosicion = normalizarTokensPosicion(posicionCandidato)
        return when (posicionBase) {
            "PG" -> tokensPosicion.any { it == "PG" || it == "G" }
            "SG" -> tokensPosicion.any { it == "SG" || it == "G" }
            "SF" -> tokensPosicion.any { it == "SF" || it == "F" }
            "PF" -> tokensPosicion.any { it == "PF" || it == "F" }
            "C" -> "C" in tokensPosicion
            else -> false
        }
    }

    private fun posicionPrincipal(posicion: String): String {
        return normalizarTokensPosicion(posicion).firstOrNull()
            ?: posicion.uppercase(Locale.US).trim().ifBlank { "G" }
    }

    private fun normalizarTokensPosicion(posicion: String): List<String> {
        return posicion
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

    private fun plantillaAJugador(jugadorPlantilla: RosterPlayerTemplate): Player? {
        val partesNombre = jugadorPlantilla.name.trim().split(" ").filter { it.isNotBlank() }
        val nombre = partesNombre.firstOrNull() ?: return null
        val apellidos = partesNombre.drop(1).joinToString(" ").ifBlank { "-" }

        return Player(
            id = jugadorPlantilla.nbaPlayerId ?: -abs(jugadorPlantilla.name.hashCode()),
            firstName = nombre,
            lastName = apellidos,
            position = jugadorPlantilla.position,
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

    // Convierte los filtros de anio y mes en un rango exacto para la API.
    private fun obtenerRangoFechasPartidos(): Pair<String?, String?> {
        val formateador = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val hoy = Calendar.getInstance()
        val calendarioInicio = hoy.clone() as Calendar
        val calendarioFin = hoy.clone() as Calendar

        when {
            anioPartidosSeleccionado != null && mesPartidosSeleccionado != null -> {
                calendarioInicio.set(Calendar.YEAR, anioPartidosSeleccionado!!)
                calendarioInicio.set(Calendar.MONTH, mesPartidosSeleccionado!! - 1)
                calendarioInicio.set(Calendar.DAY_OF_MONTH, 1)

                calendarioFin.set(Calendar.YEAR, anioPartidosSeleccionado!!)
                calendarioFin.set(Calendar.MONTH, mesPartidosSeleccionado!! - 1)
                calendarioFin.set(Calendar.DAY_OF_MONTH, calendarioFin.getActualMaximum(Calendar.DAY_OF_MONTH))
            }

            anioPartidosSeleccionado != null -> {
                calendarioInicio.set(Calendar.YEAR, anioPartidosSeleccionado!!)
                calendarioInicio.set(Calendar.MONTH, Calendar.JANUARY)
                calendarioInicio.set(Calendar.DAY_OF_MONTH, 1)

                calendarioFin.set(Calendar.YEAR, anioPartidosSeleccionado!!)
                calendarioFin.set(Calendar.MONTH, Calendar.DECEMBER)
                calendarioFin.set(Calendar.DAY_OF_MONTH, 31)
            }

            mesPartidosSeleccionado != null -> {
                calendarioInicio.set(Calendar.YEAR, hoy.get(Calendar.YEAR))
                calendarioInicio.set(Calendar.MONTH, mesPartidosSeleccionado!! - 1)
                calendarioInicio.set(Calendar.DAY_OF_MONTH, 1)

                calendarioFin.set(Calendar.YEAR, hoy.get(Calendar.YEAR))
                calendarioFin.set(Calendar.MONTH, mesPartidosSeleccionado!! - 1)
                calendarioFin.set(Calendar.DAY_OF_MONTH, calendarioFin.getActualMaximum(Calendar.DAY_OF_MONTH))
            }

            else -> {
                calendarioInicio.add(Calendar.MONTH, -1)
                calendarioFin.add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        return formateador.format(calendarioInicio.time) to formateador.format(calendarioFin.time)
    }

    private fun mensajeErrorEstadisticasJugador(excepcion: Exception): String {
        return excepcion.message ?: "Error cargando estadisticas"
    }

    private fun precargarEstadisticasSegunOrden() {
        if (!ordenJugadorSeleccionado.requiresStats) return
        if (debeUsarRankingTemporada()) {
            cargarRankingTemporada()
            return
        }
        jugadoresVisibles.forEach { jugador ->
            cargarEstadisticasJugador(jugador)
        }
    }

    private fun debeUsarRankingTemporada(): Boolean {
        return ordenJugadorSeleccionado.requiresStats && busquedaJugador.trim().isBlank()
    }

    private fun fuenteJugadoresModoActual(): List<Player> {
        return if (debeUsarRankingTemporada() && jugadoresRankingTemporada.isNotEmpty()) {
            jugadoresRankingTemporada
        } else {
            jugadores
        }
    }

    private fun comparadorJugadores(criterioOrden: PlayerSortOption): Comparator<Player> {
        return when (criterioOrden) {
            PlayerSortOption.NAME -> compareBy<Player> { it.lastName.lowercase(Locale.US) }
                .thenBy { it.firstName.lowercase(Locale.US) }

            else -> compareByDescending<Player> { metricaOrdenJugador(it, criterioOrden) }
                .thenBy { it.lastName.lowercase(Locale.US) }
                .thenBy { it.firstName.lowercase(Locale.US) }
        }
    }

    private fun metricaOrdenJugador(jugador: Player, criterioOrden: PlayerSortOption): Double {
        val resumen = resumenJugadorParaOrdenar(jugador.id) ?: return Double.NEGATIVE_INFINITY
        return when (criterioOrden) {
            PlayerSortOption.NAME -> 0.0
            PlayerSortOption.POINTS -> resumen.pointsPerGame
            PlayerSortOption.REBOUNDS -> resumen.reboundsPerGame
            PlayerSortOption.ASSISTS -> resumen.assistsPerGame
            PlayerSortOption.STEALS -> resumen.stealsPerGame
            PlayerSortOption.BLOCKS -> resumen.blocksPerGame
            PlayerSortOption.MINUTES -> resumen.minutesPerGame ?: Double.NEGATIVE_INFINITY
        }
    }

    // Construye un ranking propio con estadisticas acumuladas cuando no hay busqueda activa.
    private fun cargarRankingTemporada(forzarRecarga: Boolean = false) {
        val temporada = temporadaRegularActual()
        if (cargandoJugadores) return
        if (!forzarRecarga && temporadaRankingTemporada == temporada && jugadoresRankingTemporada.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            cargandoJugadores = true
            errorJugadores = null
            try {
                val ranking = obtenerRankingTemporada(temporada)
                jugadoresRankingTemporada = ranking.map { it.player }
                estadisticasRankingTemporada = ranking.associate { it.player.id to it.summary }
                temporadaRankingTemporada = temporada
            } catch (e: Exception) {
                errorJugadores = mensajeErrorApi(
                    e,
                    "No se pudo construir el ranking de temporada de jugadores"
                )
            }
            cargandoJugadores = false
        }
    }

    private suspend fun obtenerRankingTemporada(temporada: Int): List<PlayerSeasonLeaderboardEntry> {
        val acumuladores = linkedMapOf<Int, MutablePlayerSeasonAccumulator>()
        var cursor: Int? = null
        var pagina = 0

        do {
            val respuesta = servicioApi.obtenerEstadisticas(
                seasons = listOf(temporada),
                perPage = 100,
                cursor = cursor
            )

            respuesta.data.forEach { estadistica ->
                val jugadorEstadistica = estadistica.player ?: return@forEach
                val equipo = jugadorEstadistica.team ?: estadistica.team ?: return@forEach
                val partido = estadistica.game ?: return@forEach

                if (partido.postseason) return@forEach
                if (equipo.abbreviation !in currentNbaTeamAbbreviations) return@forEach

                val jugadorNormalizado = jugadorEstadistica.copy(team = equipo)
                val acumulador = acumuladores.getOrPut(jugadorNormalizado.id) {
                    MutablePlayerSeasonAccumulator(player = jugadorNormalizado)
                }
                acumulador.player = jugadorNormalizado
                acumulador.add(estadistica)
            }

            cursor = respuesta.meta?.nextCursor
            pagina++
        } while (cursor != null && pagina < 500)

        return acumuladores.values
            .asSequence()
            .mapNotNull { it.toLeaderboardEntryOrNull(temporada) }
            .filter { entry ->
                entry.player.team != null &&
                    entry.summary.gamesPlayed >= MIN_GAMES_FOR_PLAYER_LEADERBOARD
            }
            .toList()
    }

    private fun mensajeErrorApi(excepcion: Exception, mensajeAlternativo: String): String {
        val mensaje = excepcion.message.orEmpty()
        return if (mensaje.contains("429")) {
            "Demasiadas peticiones a la API. Espera unos segundos y vuelve a intentarlo."
        } else {
            mensaje.ifBlank { mensajeAlternativo }
        }
    }

    // Recupera la ultima pagina guardada para evitar peticiones repetidas al escribir lo mismo.
    private fun leerPaginaJugadoresCache(consulta: String): CachedPlayersPage? {
        val contenidoCache = preferenciasCache.getString(claveCacheJugadores(consulta), null) ?: return null
        return runCatching {
            gson.fromJson<CachedPlayersPage>(
                contenidoCache,
                object : TypeToken<CachedPlayersPage>() {}.type
            )
        }.getOrNull()
    }

    private fun guardarPaginaJugadoresCache(
        consulta: String,
        jugadores: List<Player>,
        nextCursor: Int?
    ) {
        val cargaCache = CachedPlayersPage(
            jugadores = jugadores,
            nextCursor = nextCursor,
            cachedAt = System.currentTimeMillis()
        )
        preferenciasCache.edit()
            .putString(claveCacheJugadores(consulta), gson.toJson(cargaCache))
            .apply()
    }

    private fun claveCacheJugadores(consulta: String): String {
        val consultaNormalizada = consulta
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "all" }
        return "cache_jugadores_$consultaNormalizada"
    }

    private fun cacheSigueVigente(cache: CachedPlayersPage): Boolean {
        return System.currentTimeMillis() - cache.cachedAt <= duracionCacheJugadoresMs
    }

    private suspend fun cargarPartidosTemporada(temporada: Int): List<Game> {
        val partidosTemporada = mutableListOf<Game>()
        var cursor: Int? = null

        do {
            val respuesta = servicioApi.obtenerPartidos(
                seasons = listOf(temporada),
                perPage = 100,
                cursor = cursor
            )
            partidosTemporada += respuesta.data
            cursor = respuesta.meta?.nextCursor
        } while (cursor != null)

        return partidosTemporada
    }

    // Reconstruye una clasificacion simple a partir de partidos finalizados cuando hace falta.
    private fun construirClasificacionEquipos(
        equipos: List<Team>,
        seasonGames: List<Game>
    ): List<TeamStandingSummary> {
        val records = equipos.associate { team ->
            team.id to MutableTeamRecord(team = team)
        }.toMutableMap()

        seasonGames
            .asSequence()
            .filter { !it.postseason && esPartidoFinalizado(it) }
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

    // Devuelve una tabla vacia para no romper la interfaz cuando aun no hay datos.
    private fun construirClasificacionVacia(equipos: List<Team>): List<TeamStandingSummary> {
        return equipos.map { team ->
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

    // Ajusta el anio segun el mes para encajar con el calendario real de la NBA.
    private fun temporadaRegularActual(): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        return if (month >= Calendar.OCTOBER) year else year - 1
    }

    // ESPN etiqueta la temporada con el anio en que termina, no con el que empieza.
    private fun temporadaEspnActual(): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        return if (month >= Calendar.OCTOBER) year + 1 else year
    }

    private fun esPartidoFinalizado(game: Game): Boolean {
        return game.status.contains("Final", ignoreCase = true)
    }

    // Acumula el balance de cada equipo para luego derivar su posicion en la tabla.
    private data class MutableTeamRecord(
        val team: Team,
        var points: Int = 0,
        var wins: Int = 0,
        var losses: Int = 0
    )

    // Empareja el jugador con su resumen estadistico ya calculado para el ranking.
    private data class PlayerSeasonLeaderboardEntry(
        val player: Player,
        val summary: PlayerSeasonStats
    )

    // Suma estadisticas partido a partido para construir un resumen medio de temporada.
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
        // Agrega una linea estadistica individual al acumulado de temporada.
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

        // Convierte el acumulado bruto en un resumen apto para ordenar y mostrar en interfaz.
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

    // Lista cerrada de franquicias vigentes para filtrar equipos historicos o datos inconsistentes.
    private val currentNbaTeamAbbreviations = setOf(
        "ATL", "BOS", "BKN", "CHA", "CHI", "CLE", "DAL", "DEN", "DET", "GSW",
        "HOU", "IND", "LAC", "LAL", "MEM", "MIA", "MIL", "MIN", "NOP", "NYK",
        "OKC", "ORL", "PHI", "PHX", "POR", "SAC", "SAS", "TOR", "UTA", "WAS"
    )

    private companion object {
        const val MIN_GAMES_FOR_PLAYER_LEADERBOARD = 10

        // Evita divisiones por cero cuando todavia no hay suficientes muestras.
        fun averageOrNull(total: Double, samples: Int): Double? {
            return if (samples > 0) total / samples else null
        }

        // Devuelve porcentajes en base 100 para que la capa de UI no tenga que convertirlos.
        fun percentage(made: Int, attempted: Int): Double? {
            return if (attempted > 0) made.toDouble() / attempted * 100.0 else null
        }

        // Admite minutos en formato decimal o en formato mm:ss segun la fuente de datos.
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

    // Estructura serializable que se guarda en preferencias para reutilizar paginas recientes.
    private data class CachedPlayersPage(
        val jugadores: List<Player>,
        val nextCursor: Int?,
        val cachedAt: Long
    )
}
