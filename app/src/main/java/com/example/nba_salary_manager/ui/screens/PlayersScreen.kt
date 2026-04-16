package com.example.nba_salary_manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nba_salary_manager.data.model.Player
import com.example.nba_salary_manager.data.model.PlayerPositionFilter
import com.example.nba_salary_manager.data.model.PlayerSeasonStats
import com.example.nba_salary_manager.data.model.PlayerSortOption
import com.example.nba_salary_manager.data.model.PlayerStatsUiState
import com.example.nba_salary_manager.ui.components.PlayerAvatar
import com.example.nba_salary_manager.viewmodel.NbaViewModel
import java.util.Locale

// Pantalla principal de jugadores: busqueda, filtros, orden y detalle estadistico.
@Composable
fun PlayersScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val todosLosJugadores = viewModel.jugadores
    val jugadoresVisibles = viewModel.jugadoresVisibles
    val cargando = viewModel.cargandoJugadores
    val mensajeError = viewModel.errorJugadores
    val busqueda = viewModel.busquedaJugador
    val puedeCargarMasJugadores = viewModel.puedeCargarMasJugadores
    val filtroPosicionSeleccionado = viewModel.filtroPosicionJugadorSeleccionado
    val ordenSeleccionado = viewModel.ordenJugadorSeleccionado
    val controladorTeclado = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF552583), Color(0xFFFDB927))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    "Jugadores NBA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Consulta una base con más de 4800 jugadores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = busqueda,
            onValueChange = { viewModel.actualizarBusquedaJugador(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Buscar jugador, por ejemplo: LeBron o Curry") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF552583))
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        viewModel.buscarJugadores()
                        controladorTeclado?.hide()
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFF552583))
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.buscarJugadores()
                    controladorTeclado?.hide()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF552583),
                cursorColor = Color(0xFF552583)
            )
        )

        PlayerFilterBar(
            selectedPositionFilter = filtroPosicionSeleccionado,
            selectedSortOption = ordenSeleccionado,
            onPositionSelected = viewModel::actualizarFiltroPosicionJugador,
            onSortSelected = viewModel::actualizarOrdenJugador
        )

        when {
            cargando && todosLosJugadores.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF552583))
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando jugadores...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            mensajeError != null && todosLosJugadores.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFC8102E)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            mensajeError,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.buscarJugadores() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF552583))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }

            jugadoresVisibles.isEmpty() && !cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No se encontraron jugadores",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (puedeCargarMasJugadores) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.cargarMasJugadores() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF552583)
                                )
                            ) {
                                Text("Cargar más jugadores")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ordenSeleccionado.requiresStats) {
                        item {
                            Text(
                                text = if (busqueda.isBlank()) {
                                    "Ranking real de la temporada por ${ordenSeleccionado.label.lowercase(Locale.getDefault())}, usando estadísticas agregadas de jugadores activos."
                                } else {
                                    "Los resultados buscados se están ordenando por ${ordenSeleccionado.label.lowercase(Locale.getDefault())}."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(jugadoresVisibles, key = { it.id }) { jugador ->
                        PlayerCard(
                            player = jugador,
                            viewModel = viewModel,
                            sortOption = ordenSeleccionado
                        )
                    }

                    if (puedeCargarMasJugadores && !cargando) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { viewModel.cargarMasJugadores() },
                                    modifier = Modifier.padding(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF552583)
                                    )
                                ) {
                                    Text("Cargar más jugadores")
                                }
                            }
                        }
                    }

                    if (cargando) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF552583),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cargando && todosLosJugadores.isEmpty() && mensajeError == null) {
            viewModel.cargarJugadores()
        }
    }
}

// Barra de chips para acotar la lista por posicion y criterio de orden.
@Composable
private fun PlayerFilterBar(
    selectedPositionFilter: PlayerPositionFilter,
    selectedSortOption: PlayerSortOption,
    onPositionSelected: (PlayerPositionFilter) -> Unit,
    onSortSelected: (PlayerSortOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Filtrar por posición",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PlayerPositionFilter.entries) { option ->
                FilterChip(
                    selected = selectedPositionFilter == option,
                    onClick = { onPositionSelected(option) },
                    label = { Text(option.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Ordenar por",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PlayerSortOption.entries) { option ->
                FilterChip(
                    selected = selectedSortOption == option,
                    onClick = { onSortSelected(option) },
                    label = { Text(option.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

// Tarjeta resumida de cada jugador con acceso rapido a sus estadisticas.
@Composable
private fun PlayerCard(player: Player, viewModel: NbaViewModel, sortOption: PlayerSortOption) {
    var expandida by remember { mutableStateOf(false) }
    val estadoEstadisticas = viewModel.estadoEstadisticasJugador(player.id)
    val resumenOrden = viewModel.resumenJugadorParaOrdenar(player.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val siguienteEstado = !expandida
                expandida = siguienteEstado
                if (siguienteEstado) {
                    viewModel.cargarEstadisticasJugador(player)
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    PlayerAvatar(
                        nombreJugador = "${player.firstName} ${player.lastName}",
                        idJugadorNba = null,
                        modifier = Modifier.size(56.dp),
                        colorRespaldo = Color(0xFF552583)
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF552583),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = player.jerseyNumber ?: "#",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${player.firstName} ${player.lastName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!player.position.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF552583).copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    player.position,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF552583)
                                )
                            }
                        }
                        Text(
                            player.team?.fullName ?: "Agente libre",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (sortOption.requiresStats) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFFDB927).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "${sortOption.label}: ${playerSortMetricValue(estadoEstadisticas, resumenOrden, sortOption)}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7A4E00)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expandida,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    player.height?.let { PlayerDetailRow("Altura", it) }
                    player.weight?.let { PlayerDetailRow("Peso", "$it lbs") }
                    player.college?.let { PlayerDetailRow("Universidad", it) }
                    player.country?.let { PlayerDetailRow("País", it) }
                    player.draftYear?.let { year ->
                        val round = player.draftRound ?: "-"
                        val number = player.draftNumber ?: "-"
                        PlayerDetailRow("Draft", "$year - Ronda $round - Pick #$number")
                    }
                    PlayerStatsSection(
                        statsState = estadoEstadisticas,
                        onRetry = { viewModel.cargarEstadisticasJugador(player, forzarRecarga = true) }
                    )
                }
            }
        }
    }
}

// Bloque expandible que muestra carga, error o el resumen de temporada del jugador.
@Composable
private fun PlayerStatsSection(
    statsState: PlayerStatsUiState,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Estadísticas completas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF552583)
        )
        Spacer(Modifier.height(8.dp))

        when {
            statsState.isLoading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF552583)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Cargando estadísticas del jugador...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            statsState.error != null -> {
                Column {
                    Text(
                        statsState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC8102E)
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            statsState.hasLoaded && statsState.summary == null -> {
                Text(
                    "No hay estadísticas disponibles para este jugador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            statsState.summary != null -> {
                PlayerStatsSummary(summary = statsState.summary)
            }
        }
    }
}

// Resumen compacto que reutiliza el mismo formato fila-etiqueta para todas las metricas.
@Composable
private fun PlayerStatsSummary(summary: PlayerSeasonStats) {
    PlayerDetailRow("Temporada", summary.season.toString())
    PlayerDetailRow("Partidos", summary.gamesPlayed.toString())
    summary.gamesStarted?.let { PlayerDetailRow("Partidos como titular", it.toString()) }
    PlayerDetailRow("Minutos por partido", formatDecimal(summary.minutesPerGame))
    PlayerDetailRow("Puntos por partido", formatDecimal(summary.pointsPerGame))
    PlayerDetailRow("Rebotes por partido", formatDecimal(summary.reboundsPerGame))
    PlayerDetailRow("Asistencias por partido", formatDecimal(summary.assistsPerGame))
    PlayerDetailRow("Robos por partido", formatDecimal(summary.stealsPerGame))
    PlayerDetailRow("Tapones por partido", formatDecimal(summary.blocksPerGame))
    PlayerDetailRow("Pérdidas por partido", formatDecimal(summary.turnoversPerGame))
    PlayerDetailRow("Faltas por partido", formatDecimal(summary.foulsPerGame))
    PlayerDetailRow("Rebotes ofensivos por partido", formatDecimal(summary.offensiveReboundsPerGame))
    PlayerDetailRow("Rebotes defensivos por partido", formatDecimal(summary.defensiveReboundsPerGame))
    PlayerDetailRow("Más/menos por partido", formatSignedDecimal(summary.plusMinusPerGame))
    PlayerDetailRow(
        "Tiros de campo",
        "${formatPercentage(summary.fieldGoalPercentage)} (${summary.fieldGoalsMade}/${summary.fieldGoalsAttempted})"
    )
    PlayerDetailRow(
        "Triples",
        "${formatPercentage(summary.threePointPercentage)} (${summary.threePointsMade}/${summary.threePointsAttempted})"
    )
    PlayerDetailRow(
        "Tiros libres",
        "${formatPercentage(summary.freeThrowPercentage)} (${summary.freeThrowsMade}/${summary.freeThrowsAttempted})"
    )
    summary.doubleDoubles?.let { PlayerDetailRow("Dobles-dobles", it.toString()) }
    summary.tripleDoubles?.let { PlayerDetailRow("Triples-dobles", it.toString()) }
    summary.assistToTurnoverRatio?.let { PlayerDetailRow("Relación asistencias/pérdidas", formatDecimal(it)) }
    summary.scoringEfficiency?.let { PlayerDetailRow("Eficiencia anotadora", formatDecimal(it)) }
    summary.shootingEfficiency?.let { PlayerDetailRow("Eficiencia de tiro", formatDecimal(it)) }
}

@Composable
private fun PlayerDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Convierte el criterio de orden actual en el valor que se ensena dentro de la tarjeta.
private fun playerSortMetricValue(
    estadoEstadisticas: PlayerStatsUiState,
    resumen: PlayerSeasonStats?,
    opcionOrden: PlayerSortOption
): String {
    if (estadoEstadisticas.isLoading) return "..."
    val resumenResuelto = resumen ?: estadoEstadisticas.summary ?: return "-"
    return when (opcionOrden) {
        PlayerSortOption.NAME -> "-"
        PlayerSortOption.POINTS -> formatDecimal(resumenResuelto.pointsPerGame)
        PlayerSortOption.REBOUNDS -> formatDecimal(resumenResuelto.reboundsPerGame)
        PlayerSortOption.ASSISTS -> formatDecimal(resumenResuelto.assistsPerGame)
        PlayerSortOption.STEALS -> formatDecimal(resumenResuelto.stealsPerGame)
        PlayerSortOption.BLOCKS -> formatDecimal(resumenResuelto.blocksPerGame)
        PlayerSortOption.MINUTES -> formatDecimal(resumenResuelto.minutesPerGame)
    }
}

private fun formatDecimal(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
}

// Anade signo explicito para metricas donde interesa distinguir positivos y negativos.
private fun formatSignedDecimal(value: Double?): String {
    return value?.let {
        val prefix = if (it > 0) "+" else ""
        prefix + String.format(Locale.US, "%.1f", it)
    } ?: "-"
}

private fun formatPercentage(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.1f%%", it) } ?: "-"
}
