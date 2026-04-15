package com.example.nba_salary_manager.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nba_salary_manager.data.model.Game
import com.example.nba_salary_manager.ui.components.TeamLogo
import com.example.nba_salary_manager.viewmodel.NbaViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun GamesScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val games = viewModel.games
    val isLoading = viewModel.gamesLoading
    val error = viewModel.gamesError
    val hasMore = viewModel.gamesHasMore
    val selectedGameYear = viewModel.selectedGameYear
    val selectedGameMonth = viewModel.selectedGameMonth
    val availableGameYears = viewModel.availableGameYears
    val availableGameMonths = viewModel.availableGameMonths
    val hasActiveFilters = selectedGameYear != null || selectedGameMonth != null

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF006BB6), Color(0xFFED174C))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    "Partidos NBA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    headerSubtitle(selectedGameYear, selectedGameMonth),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        GamesFilterSection(
            selectedYear = selectedGameYear,
            selectedMonth = selectedGameMonth,
            availableYears = availableGameYears,
            availableMonths = availableGameMonths,
            hasActiveFilters = hasActiveFilters,
            onYearSelected = viewModel::updateGameYearFilter,
            onMonthSelected = viewModel::updateGameMonthFilter,
            onClearFilters = viewModel::clearGameFilters
        )

        when {
            isLoading && games.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF006BB6))
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando partidos...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            error != null && games.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFED174C)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadGames() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006BB6))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }

            games.isEmpty() && !isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            emptyGamesMessage(hasActiveFilters),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Mostrando ${games.size} partidos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(games) { game ->
                        GameCard(game)
                    }

                    if (hasMore && !isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { viewModel.loadMoreGames() },
                                    modifier = Modifier.padding(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF006BB6)
                                    )
                                ) {
                                    Text("Cargar mas partidos")
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
                                    color = Color(0xFF006BB6),
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
        if (!isLoading && games.isEmpty() && error == null) {
            viewModel.loadGames()
        }
    }
}

@Composable
private fun GamesFilterSection(
    selectedYear: Int?,
    selectedMonth: Int?,
    availableYears: List<Int>,
    availableMonths: List<Int>,
    hasActiveFilters: Boolean,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Filtros",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IntFilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Año",
                selectedLabel = selectedYear?.toString() ?: "Todos",
                values = availableYears,
                valueLabel = { it.toString() },
                onValueSelected = onYearSelected
            )

            IntFilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Mes",
                selectedLabel = selectedMonth?.let(::monthLabel) ?: "Todos",
                values = availableMonths,
                valueLabel = ::monthLabel,
                onValueSelected = onMonthSelected
            )
        }

        if (hasActiveFilters) {
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Limpiar filtros")
            }
        }
    }
}

@Composable
private fun IntFilterDropdown(
    modifier: Modifier = Modifier,
    label: String,
    selectedLabel: String,
    values: List<Int>,
    valueLabel: (Int) -> String,
    onValueSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onValueSelected(null)
                    expanded = false
                }
            )

            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(valueLabel(value)) },
                    onClick = {
                        onValueSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun headerSubtitle(selectedYear: Int?, selectedMonth: Int?): String {
    return when {
        selectedYear != null && selectedMonth != null -> "${monthLabel(selectedMonth)} de $selectedYear"
        selectedYear != null -> "Partidos del anio $selectedYear"
        selectedMonth != null -> "${monthLabel(selectedMonth)} del anio actual"
        else -> "Ultimo mes y proximos 7 dias"
    }
}

private fun emptyGamesMessage(hasActiveFilters: Boolean): String {
    return if (hasActiveFilters) {
        "No hay partidos para el filtro seleccionado"
    } else {
        "No hay partidos en el ultimo mes ni en los proximos 7 dias"
    }
}

private fun monthLabel(month: Int): String {
    return when (month) {
        1 -> "Enero"
        2 -> "Febrero"
        3 -> "Marzo"
        4 -> "Abril"
        5 -> "Mayo"
        6 -> "Junio"
        7 -> "Julio"
        8 -> "Agosto"
        9 -> "Septiembre"
        10 -> "Octubre"
        11 -> "Noviembre"
        12 -> "Diciembre"
        else -> month.toString()
    }
}

private fun formatGameDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val cleanDate = if (dateString.contains("T")) dateString.split("T")[0] else dateString
        cleanDate.replace("-", "/")
    } catch (_: Exception) {
        dateString.replace("-", "/")
    }
}

private fun formatGameTime(status: String?): String {
    if (status == null) return ""
    return try {
        if (status.contains("T") && status.contains("Z")) {
            status.split("T")[1].substring(0, 5)
        } else if (status.contains("AM", ignoreCase = true) || status.contains("PM", ignoreCase = true)) {
            val timePart = status.split(" ")[0]
            val amPm = if (status.contains("PM", ignoreCase = true)) "PM" else "AM"
            val inputFormat = SimpleDateFormat("h:mm a", Locale.US)
            val outputFormat = SimpleDateFormat("HH:mm", Locale.US)
            val date = inputFormat.parse("$timePart $amPm")
            date?.let { outputFormat.format(it) } ?: status
        } else {
            status
        }
    } catch (_: Exception) {
        status
    }
}

@Composable
private fun GameCard(game: Game) {
    val statusColor = when {
        game.status.contains("Final", ignoreCase = true) -> Color(0xFF2E7D32)
        game.status.contains("progress", ignoreCase = true) ||
            (game.period != null && game.period > 0 && !game.status.contains("Final", ignoreCase = true)) -> Color(0xFFED174C)

        else -> Color(0xFF757575)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatGameDate(game.date),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        formatGameTime(game.status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogo(
                        abbreviation = game.homeTeam.abbreviation,
                        teamName = game.homeTeam.fullName,
                        modifier = Modifier.size(40.dp),
                        fallbackColor = Color(0xFF006BB6)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        game.homeTeam.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Local",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${game.homeTeamScore}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (game.homeTeamScore > game.visitorTeamScore) {
                                Color(0xFF006BB6)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            " - ",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${game.visitorTeamScore}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (game.visitorTeamScore > game.homeTeamScore) {
                                Color(0xFFED174C)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (game.postseason) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFDB927).copy(alpha = 0.2f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "PLAYOFFS",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogo(
                        abbreviation = game.visitorTeam.abbreviation,
                        teamName = game.visitorTeam.fullName,
                        modifier = Modifier.size(40.dp),
                        fallbackColor = Color(0xFFED174C)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        game.visitorTeam.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Visitante",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
