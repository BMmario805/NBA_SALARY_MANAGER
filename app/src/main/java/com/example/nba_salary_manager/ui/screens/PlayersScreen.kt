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

@Composable
fun PlayersScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val allPlayers = viewModel.players
    val players = viewModel.visiblePlayers
    val isLoading = viewModel.playersLoading
    val error = viewModel.playersError
    val searchQuery = viewModel.playerSearchQuery
    val canLoadMorePlayers = viewModel.canLoadMorePlayers
    val selectedPositionFilter = viewModel.selectedPlayerPositionFilter
    val selectedSortOption = viewModel.selectedPlayerSortOption
    val keyboardController = LocalSoftwareKeyboardController.current

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
                    "Busca entre mas de 4800 jugadores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updatePlayerSearch(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Buscar jugador (ej: LeBron, Curry)") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF552583))
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        viewModel.searchPlayers()
                        keyboardController?.hide()
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
                    viewModel.searchPlayers()
                    keyboardController?.hide()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF552583),
                cursorColor = Color(0xFF552583)
            )
        )

        PlayerFilterBar(
            selectedPositionFilter = selectedPositionFilter,
            selectedSortOption = selectedSortOption,
            onPositionSelected = viewModel::updatePlayerPositionFilter,
            onSortSelected = viewModel::updatePlayerSortOption
        )

        when {
            isLoading && allPlayers.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF552583))
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando jugadores...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            error != null && allPlayers.isEmpty() -> {
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
                            error,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.searchPlayers() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF552583))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }

            players.isEmpty() && !isLoading -> {
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
                        if (canLoadMorePlayers) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.loadMorePlayers() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF552583)
                                )
                            ) {
                                Text("Cargar mas jugadores")
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
                    if (selectedSortOption.requiresStats) {
                        item {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "Ranking real de temporada por ${selectedSortOption.label.lowercase(Locale.getDefault())}, usando estadisticas agregadas de jugadores activos."
                                } else {
                                    "Ordenando los resultados buscados por ${selectedSortOption.label.lowercase(Locale.getDefault())}."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(players, key = { it.id }) { player ->
                        PlayerCard(
                            player = player,
                            viewModel = viewModel,
                            sortOption = selectedSortOption
                        )
                    }

                    if (canLoadMorePlayers && !isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { viewModel.loadMorePlayers() },
                                    modifier = Modifier.padding(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF552583)
                                    )
                                ) {
                                    Text("Cargar mas jugadores")
                                }
                            }
                        }
                    }

                    if (isLoading) {
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
        if (!isLoading && allPlayers.isEmpty() && error == null) {
            viewModel.loadPlayers()
        }
    }
}

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
            "Filtrar por posicion",
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

@Composable
private fun PlayerCard(player: Player, viewModel: NbaViewModel, sortOption: PlayerSortOption) {
    var expanded by remember { mutableStateOf(false) }
    val statsState = viewModel.playerStatsState(player.id)
    val sortSummary = viewModel.playerSummaryForSorting(player.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val nextExpanded = !expanded
                expanded = nextExpanded
                if (nextExpanded) {
                    viewModel.loadPlayerStats(player)
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
                        playerName = "${player.firstName} ${player.lastName}",
                        nbaPlayerId = null,
                        modifier = Modifier.size(56.dp),
                        fallbackColor = Color(0xFF552583)
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
                            player.team?.fullName ?: "Free Agent",
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
                                text = "${sortOption.label}: ${playerSortMetricValue(statsState, sortSummary, sortOption)}",
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
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    player.height?.let { PlayerDetailRow("Altura", it) }
                    player.weight?.let { PlayerDetailRow("Peso", "$it lbs") }
                    player.college?.let { PlayerDetailRow("Universidad", it) }
                    player.country?.let { PlayerDetailRow("Pais", it) }
                    player.draftYear?.let { year ->
                        val round = player.draftRound ?: "-"
                        val number = player.draftNumber ?: "-"
                        PlayerDetailRow("Draft", "$year - Ronda $round - Pick #$number")
                    }
                    PlayerStatsSection(
                        statsState = statsState,
                        onRetry = { viewModel.loadPlayerStats(player, forceRefresh = true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerStatsSection(
    statsState: PlayerStatsUiState,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Estadisticas completas",
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
                        "Cargando estadisticas del jugador...",
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
                    "No hay estadisticas disponibles para este jugador.",
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
    PlayerDetailRow("Perdidas por partido", formatDecimal(summary.turnoversPerGame))
    PlayerDetailRow("Faltas por partido", formatDecimal(summary.foulsPerGame))
    PlayerDetailRow("Rebotes ofensivos por partido", formatDecimal(summary.offensiveReboundsPerGame))
    PlayerDetailRow("Rebotes defensivos por partido", formatDecimal(summary.defensiveReboundsPerGame))
    PlayerDetailRow("Mas/menos por partido", formatSignedDecimal(summary.plusMinusPerGame))
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
    summary.assistToTurnoverRatio?.let { PlayerDetailRow("Relacion asistencias/perdidas", formatDecimal(it)) }
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

private fun playerSortMetricValue(
    statsState: PlayerStatsUiState,
    summary: PlayerSeasonStats?,
    sortOption: PlayerSortOption
): String {
    if (statsState.isLoading) return "..."
    val resolvedSummary = summary ?: statsState.summary ?: return "-"
    return when (sortOption) {
        PlayerSortOption.NAME -> "-"
        PlayerSortOption.POINTS -> formatDecimal(resolvedSummary.pointsPerGame)
        PlayerSortOption.REBOUNDS -> formatDecimal(resolvedSummary.reboundsPerGame)
        PlayerSortOption.ASSISTS -> formatDecimal(resolvedSummary.assistsPerGame)
        PlayerSortOption.STEALS -> formatDecimal(resolvedSummary.stealsPerGame)
        PlayerSortOption.BLOCKS -> formatDecimal(resolvedSummary.blocksPerGame)
        PlayerSortOption.MINUTES -> formatDecimal(resolvedSummary.minutesPerGame)
    }
}

private fun formatDecimal(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
}

private fun formatSignedDecimal(value: Double?): String {
    return value?.let {
        val prefix = if (it > 0) "+" else ""
        prefix + String.format(Locale.US, "%.1f", it)
    } ?: "-"
}

private fun formatPercentage(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.1f%%", it) } ?: "-"
}
