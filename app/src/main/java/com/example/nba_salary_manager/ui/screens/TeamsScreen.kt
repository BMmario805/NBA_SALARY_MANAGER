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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nba_salary_manager.data.model.Team
import com.example.nba_salary_manager.data.model.TeamStandingSummary
import com.example.nba_salary_manager.ui.components.TeamLogo
import com.example.nba_salary_manager.viewmodel.NbaViewModel

private enum class ConferenceFilter(val label: String, val value: String?) {
    ALL("Todas", null),
    EAST("Este", "East"),
    WEST("Oeste", "West")
}

private enum class TeamSortOption(val label: String) {
    NAME("Nombre"),
    POSITION("Posición"),
    POINTS("Puntos")
}

// Pantalla de equipos con filtros simples y apoyo de clasificacion cuando hace falta ordenar.
@Composable
fun TeamsScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val equipos = viewModel.equipos
    val cargando = viewModel.cargandoEquipos
    val mensajeError = viewModel.errorEquipos
    val clasificacion = viewModel.clasificacionEquipos
    val cargandoEstadisticas = viewModel.cargandoEstadisticasEquipos
    val errorEstadisticas = viewModel.errorEstadisticasEquipos

    var conferenciaSeleccionada by remember { mutableStateOf(ConferenceFilter.ALL) }
    var ordenSeleccionado by remember { mutableStateOf(TeamSortOption.NAME) }

    val clasificacionPorEquipo = remember(clasificacion) {
        clasificacion.associateBy { it.team.id }
    }

    val equiposFiltrados = remember(equipos, conferenciaSeleccionada, ordenSeleccionado, clasificacionPorEquipo) {
        equipos
            .filter { equipo ->
                conferenciaSeleccionada.value == null || equipo.conference == conferenciaSeleccionada.value
            }
            .sortedWith(teamComparator(ordenSeleccionado, clasificacionPorEquipo))
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1D428A), Color(0xFFC8102E))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    "Equipos NBA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "30 franquicias actuales con su logo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }

        when {
            cargando && equipos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFC8102E))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Cargando equipos...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            mensajeError != null && equipos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            "Error cargando equipos",
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
                            onClick = { viewModel.cargarEquipos() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D428A))
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Conferencia",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ConferenceFilter.entries.forEach { option ->
                                        FilterChip(
                                            selected = conferenciaSeleccionada == option,
                                            onClick = { conferenciaSeleccionada = option },
                                            label = { Text(option.label) }
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Ordenar por",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TeamSortOption.entries.forEach { option ->
                                        FilterChip(
                                            selected = ordenSeleccionado == option,
                                            onClick = { ordenSeleccionado = option },
                                            label = { Text(option.label) }
                                        )
                                    }
                                }
                            }

                            if (ordenSeleccionado != TeamSortOption.NAME && cargandoEstadisticas && clasificacion.isEmpty()) {
                                Text(
                                    "Cargando la clasificación para aplicar el orden...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(equiposFiltrados, key = { it.id }) { equipo ->
                        TeamCard(
                            team = equipo,
                            standing = clasificacionPorEquipo[equipo.id],
                            isStatsLoading = cargandoEstadisticas,
                            statsError = errorEstadisticas,
                            onRequestStats = viewModel::cargarClasificacionEquiposSiHaceFalta
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cargando && equipos.isEmpty() && mensajeError == null) {
            viewModel.cargarEquipos()
        }
    }

    LaunchedEffect(ordenSeleccionado, equipos) {
        if (ordenSeleccionado != TeamSortOption.NAME && equipos.isNotEmpty()) {
            viewModel.cargarClasificacionEquiposSiHaceFalta()
        }
    }
}

// Tarjeta de equipo que se expande para mostrar datos basicos de rendimiento.
@Composable
private fun TeamCard(
    team: Team,
    standing: TeamStandingSummary?,
    isStatsLoading: Boolean,
    statsError: String?,
    onRequestStats: () -> Unit
) {
    var expandida by remember(team.id) { mutableStateOf(false) }
    val colorAcento = if (team.conference == "East") {
        Color(0xFF1D428A)
    } else {
        Color(0xFFC8102E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expandida = !expandida
                if (expandida && standing == null && !isStatsLoading) {
                    onRequestStats()
                }
            },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(
                    abreviatura = team.abbreviation,
                    nombreEquipo = team.fullName,
                    modifier = Modifier.size(56.dp),
                    colorRespaldo = colorAcento
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        team.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        team.city,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${team.conference} · ${team.division}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colorAcento.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = team.abbreviation,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorAcento
                    )
                }
            }

            AnimatedVisibility(
                visible = expandida,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    when {
                        isStatsLoading && standing == null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = colorAcento
                                )
                            }
                        }

                        standing != null -> {
                            DetailRow("Victorias", standing.wins.toString())
                            DetailRow("Posición en la liga", positionValue(standing.leagueRank))
                            DetailRow("Puntos totales", standing.points.toString())
                        }

                        else -> {
                            DetailRow(
                                "Estado",
                                statsError ?: "No se pudieron cargar las estadísticas"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
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

private fun positionValue(rank: Int): String {
    return if (rank > 0) "#$rank" else "--"
}

// Devuelve el comparador adecuado segun el criterio elegido por el usuario.
private fun teamComparator(
    opcionOrden: TeamSortOption,
    clasificacionPorEquipo: Map<Int, TeamStandingSummary>
): Comparator<Team> {
    return when (opcionOrden) {
        TeamSortOption.NAME -> compareBy { it.fullName }
        TeamSortOption.POSITION -> compareBy<Team> { clasificacionPorEquipo[it.id]?.leagueRank ?: Int.MAX_VALUE }
            .thenBy { it.fullName }
        TeamSortOption.POINTS -> compareByDescending<Team> { clasificacionPorEquipo[it.id]?.points ?: Int.MIN_VALUE }
            .thenBy { it.fullName }
    }
}
