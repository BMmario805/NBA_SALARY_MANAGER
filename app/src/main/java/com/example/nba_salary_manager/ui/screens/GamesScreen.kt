package com.example.nba_salary_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nba_salary_manager.data.model.Game
import com.example.nba_salary_manager.viewmodel.NbaViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val games = viewModel.games
    val isLoading = viewModel.gamesLoading
    val error = viewModel.gamesError
    val selectedSeason = viewModel.selectedSeason
    val hasMore = viewModel.gamesHasMore

    Column(modifier = modifier.fillMaxSize()) {
        // Header
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
                    "🏀 Partidos NBA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    if (selectedSeason == 2026) "Año 2026 (Temp. 25-26)" else "Temporada ${selectedSeason}-${selectedSeason + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Season selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Temporada:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            listOf(2026, 2025, 2024, 2023).forEach { season ->
                FilterChip(
                    selected = selectedSeason == season,
                    onClick = { viewModel.updateSeason(season) },
                    label = { Text("$season", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF006BB6),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        when {
            isLoading && games.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF006BB6))
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando partidos…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            error != null && games.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("❌ Error", style = MaterialTheme.typography.titleLarge, color = Color(0xFFED174C))
                        Spacer(Modifier.height(8.dp))
                        Text(error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            "No hay partidos disponibles",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                    Text("Cargar más partidos")
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF006BB6), modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatGameDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val cleanDate = if (dateString.contains("T")) dateString.split("T")[0] else dateString
        cleanDate.replace("-", "/")
    } catch (e: Exception) {
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
    } catch (e: Exception) {
        status
    }
}

@Composable
private fun GameCard(game: Game) {
    val statusColor = when {
        game.status.contains("Final", ignoreCase = true) -> Color(0xFF2E7D32)
        game.status.contains("progress", ignoreCase = true) || game.period != null && game.period > 0 && !game.status.contains("Final", ignoreCase = true) -> Color(0xFFED174C)
        else -> Color(0xFF757575)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Date and status row
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

            // Score area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xFF006BB6).copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            game.homeTeam.abbreviation,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF006BB6)
                        )
                    }
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

                // Scores
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${game.homeTeamScore}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (game.homeTeamScore > game.visitorTeamScore)
                                Color(0xFF006BB6) else MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = if (game.visitorTeamScore > game.homeTeamScore)
                                Color(0xFFED174C) else MaterialTheme.colorScheme.onSurfaceVariant
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

                // Visitor team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xFFED174C).copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            game.visitorTeam.abbreviation,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFFED174C)
                        )
                    }
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
