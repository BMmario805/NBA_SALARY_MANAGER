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
    POSITION("Posicion"),
    POINTS("Puntos")
}

@Composable
fun TeamsScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val teams = viewModel.teams
    val isLoading = viewModel.teamsLoading
    val error = viewModel.teamsError
    val standings = viewModel.teamStandings
    val teamStatsLoading = viewModel.teamStatsLoading
    val teamStatsError = viewModel.teamStatsError

    var selectedConference by remember { mutableStateOf(ConferenceFilter.ALL) }
    var selectedSort by remember { mutableStateOf(TeamSortOption.NAME) }

    val standingsByTeamId = remember(standings) {
        standings.associateBy { it.team.id }
    }

    val filteredTeams = remember(teams, selectedConference, selectedSort, standingsByTeamId) {
        teams
            .filter { team ->
            selectedConference.value == null || team.conference == selectedConference.value
        }
            .sortedWith(teamComparator(selectedSort, standingsByTeamId))
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
            isLoading && teams.isEmpty() -> {
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

            error != null && teams.isEmpty() -> {
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
                            error,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadTeams() },
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
                                            selected = selectedConference == option,
                                            onClick = { selectedConference = option },
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
                                            selected = selectedSort == option,
                                            onClick = { selectedSort = option },
                                            label = { Text(option.label) }
                                        )
                                    }
                                }
                            }

                            if (selectedSort != TeamSortOption.NAME && teamStatsLoading && standings.isEmpty()) {
                                Text(
                                    "Cargando clasificacion para aplicar el orden...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(filteredTeams, key = { it.id }) { team ->
                        TeamCard(
                            team = team,
                            standing = standingsByTeamId[team.id],
                            isStatsLoading = teamStatsLoading,
                            statsError = teamStatsError,
                            onRequestStats = { viewModel.loadTeamStandingsIfNeeded() }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isLoading && teams.isEmpty() && error == null) {
            viewModel.loadTeams()
        }
    }

    LaunchedEffect(selectedSort, teams) {
        if (selectedSort != TeamSortOption.NAME && teams.isNotEmpty()) {
            viewModel.loadTeamStandingsIfNeeded()
        }
    }
}

@Composable
private fun TeamCard(
    team: Team,
    standing: TeamStandingSummary?,
    isStatsLoading: Boolean,
    statsError: String?,
    onRequestStats: () -> Unit
) {
    var expanded by remember(team.id) { mutableStateOf(false) }
    val accentColor = if (team.conference == "East") {
        Color(0xFF1D428A)
    } else {
        Color(0xFFC8102E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                if (expanded && standing == null && !isStatsLoading) {
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
                    abbreviation = team.abbreviation,
                    teamName = team.fullName,
                    modifier = Modifier.size(56.dp),
                    fallbackColor = accentColor
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
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = team.abbreviation,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
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
                                    color = accentColor
                                )
                            }
                        }

                        standing != null -> {
                            DetailRow("Victorias", standing.wins.toString())
                            DetailRow("Posicion en la liga", positionValue(standing.leagueRank))
                            DetailRow("Puntos totales", standing.points.toString())
                        }

                        else -> {
                            DetailRow(
                                "Estado",
                                statsError ?: "No se pudieron cargar las estadisticas"
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

private fun teamComparator(
    sortOption: TeamSortOption,
    standingsByTeamId: Map<Int, TeamStandingSummary>
): Comparator<Team> {
    return when (sortOption) {
        TeamSortOption.NAME -> compareBy { it.fullName }
        TeamSortOption.POSITION -> compareBy<Team> { standingsByTeamId[it.id]?.leagueRank ?: Int.MAX_VALUE }
            .thenBy { it.fullName }
        TeamSortOption.POINTS -> compareByDescending<Team> { standingsByTeamId[it.id]?.points ?: Int.MIN_VALUE }
            .thenBy { it.fullName }
    }
}
