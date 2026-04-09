package com.example.nba_salary_manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nba_salary_manager.data.model.Player
import com.example.nba_salary_manager.ui.components.PlayerAvatar
import com.example.nba_salary_manager.viewmodel.NbaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val players = viewModel.players
    val isLoading = viewModel.playersLoading
    val error = viewModel.playersError
    val searchQuery = viewModel.playerSearchQuery
    val hasMore = viewModel.playersHasMore
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        // Header
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
                    "🏀 Jugadores NBA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Busca entre más de 4800 jugadores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updatePlayerSearch(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Buscar jugador (ej: LeBron, Curry…)") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF552583))
            },
            trailingIcon = {
                IconButton(onClick = {
                    viewModel.searchPlayers()
                    keyboardController?.hide()
                }) {
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

        when {
            isLoading && players.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF552583))
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando jugadores…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            error != null && players.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("❌ Error", style = MaterialTheme.typography.titleLarge, color = Color(0xFFC8102E))
                        Spacer(Modifier.height(8.dp))
                        Text(error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(players) { player ->
                        PlayerCard(player)
                    }

                    if (hasMore && !isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { viewModel.loadMorePlayers() },
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

                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF552583), modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(player: Player) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    PlayerAvatar(
                        playerName = "${player.firstName} ${player.lastName}",
                        // balldontlie id != NBA personId, use name resolution like roster templates.
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
                    player.country?.let { PlayerDetailRow("País", it) }
                    player.draftYear?.let { year ->
                        val round = player.draftRound ?: "-"
                        val number = player.draftNumber ?: "-"
                        PlayerDetailRow("Draft", "$year · Ronda $round · Pick #$number")
                    }
                }
            }
        }
    }
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
