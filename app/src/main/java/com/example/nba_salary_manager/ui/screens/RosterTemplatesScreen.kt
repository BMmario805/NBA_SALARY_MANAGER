package com.example.nba_salary_manager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nba_salary_manager.data.model.Player
import com.example.nba_salary_manager.data.model.PlayerSeasonStats
import com.example.nba_salary_manager.data.model.ReplacementCandidate
import com.example.nba_salary_manager.data.model.RosterPlayerTemplate
import com.example.nba_salary_manager.data.model.RosterTemplate
import com.example.nba_salary_manager.data.model.RosterTemplateCategory
import com.example.nba_salary_manager.ui.components.PlayerAvatar
import com.example.nba_salary_manager.viewmodel.NbaViewModel
import java.util.Locale

private val PageBackground = Color(0xFFF6F7FB)
private val Ink = Color(0xFF122033)
private val MutedInk = Color(0xFF66758C)
private val Emerald = Color(0xFF1C8C7C)
private val DeepEmerald = Color(0xFF083B38)
private val SoftMint = Color(0xFFE8F6F1)
private val CourtWood = Color(0xFFC78040)
private val CourtWoodDark = Color(0xFFB96D33)
private val CourtPanel = Color(0xFF111A2D)

// Pantalla de plantillas: mezcla galeria, editor visual en cancha y comparativa de reemplazos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterTemplatesScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val plantillaActiva = viewModel.plantillaActiva
    val categoriaSeleccionada = viewModel.categoriaPlantillaSeleccionada
    val plantillas = viewModel.plantillasFiltradas
    val jugadoresEditables = viewModel.jugadoresPlantillaEditable
    val asignacionesCancha = remember(jugadoresEditables) { buildCourtAssignments(jugadoresEditables) }
    val entradasEstadisticasPlantilla = remember(jugadoresEditables, viewModel.estadosEstadisticasJugadores) {
        jugadoresEditables.map { jugador -> jugador to viewModel.estadoEstadisticasJugadorPlantilla(jugador) }
    }
    val estadisticasCargadasPlantilla = remember(entradasEstadisticasPlantilla) {
        entradasEstadisticasPlantilla.mapNotNull { (_, estado) -> estado.summary }
    }
    val candidatosReemplazo = viewModel.candidatosReemplazo
    val cargandoCandidatosReemplazo = viewModel.cargandoCandidatosReemplazo
    val errorCandidatosReemplazo = viewModel.errorCandidatosReemplazo
    val estadisticasObjetivoReemplazo = viewModel.estadisticasObjetivoReemplazo
    val posicionObjetivoReemplazo = viewModel.posicionObjetivoReemplazo

    var indiceJugadorSeleccionado by rememberSaveable { mutableStateOf(0) }
    var mostrarSelectorPlantillas by rememberSaveable { mutableStateOf(false) }
    var mostrarHojaReemplazos by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(jugadoresEditables.size) {
        indiceJugadorSeleccionado = when {
            jugadoresEditables.isEmpty() -> -1
            indiceJugadorSeleccionado !in jugadoresEditables.indices -> 0
            else -> indiceJugadorSeleccionado
        }
    }

    LaunchedEffect(jugadoresEditables.map { "${it.nbaPlayerId}-${it.name}" }) {
        viewModel.cargarEstadisticasPlantillaEditable()
    }

    val jugadorSeleccionado = jugadoresEditables.getOrNull(indiceJugadorSeleccionado)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ActiveRosterCard(
                activeTemplate = plantillaActiva,
                playerCount = asignacionesCancha.count { it.player != null },
                onChangeTemplate = { mostrarSelectorPlantillas = true }
            )
        }

        item {
            CourtEditorCard(
                assignments = asignacionesCancha,
                selectedPlayerIndex = indiceJugadorSeleccionado,
                onSelectPlayer = { indiceJugadorSeleccionado = it }
            )
        }

        item {
            RosterStatsOverviewCard(
                totalPlayers = jugadoresEditables.size,
                loadedPlayers = estadisticasCargadasPlantilla.size,
                isLoading = entradasEstadisticasPlantilla.any { (_, estado) -> estado.isLoading } && estadisticasCargadasPlantilla.isEmpty(),
                averageStats = estadisticasCargadasPlantilla.toAverageSummaryOrNull()
            )
        }

        item {
            SelectedPlayerCard(
                player = jugadorSeleccionado,
                estadoEstadisticasJugador = jugadorSeleccionado?.let(viewModel::estadoEstadisticasJugadorPlantilla),
                onRetryStats = {
                    jugadorSeleccionado?.let { jugador -> viewModel.cargarEstadisticasJugadorPlantilla(jugador, forzarRecarga = true) }
                },
                onShowReplacements = {
                    jugadorSeleccionado?.let {
                        mostrarHojaReemplazos = true
                        viewModel.cargarCandidatosReemplazo(it)
                    }
                }
            )
        }
    }

    if (mostrarSelectorPlantillas) {
        TemplatePickerSheet(
            selectedCategory = categoriaSeleccionada,
            templates = plantillas,
            activeTemplateId = plantillaActiva?.id,
            onCategoryChange = viewModel::actualizarCategoriaPlantilla,
            onSelectTemplate = { template ->
                viewModel.usarPlantillaBase(template.id)
                indiceJugadorSeleccionado = 0
                mostrarSelectorPlantillas = false
            },
            onDismiss = { mostrarSelectorPlantillas = false }
        )
    }

    if (mostrarHojaReemplazos && jugadorSeleccionado != null && indiceJugadorSeleccionado in jugadoresEditables.indices) {
        ReplacementCandidatesSheet(
            targetPlayer = jugadorSeleccionado,
            targetStats = estadisticasObjetivoReemplazo,
            lockedPosition = posicionObjetivoReemplazo.ifBlank { jugadorSeleccionado.position },
            candidates = candidatosReemplazo,
            isLoading = cargandoCandidatosReemplazo,
            error = errorCandidatosReemplazo,
            onReplace = { player ->
                viewModel.reemplazarJugadorPlantillaEditable(indiceJugadorSeleccionado, player)
                mostrarHojaReemplazos = false
                viewModel.limpiarCandidatosReemplazo()
            },
            onRetry = { viewModel.cargarCandidatosReemplazo(jugadorSeleccionado) },
            onDismiss = {
                mostrarHojaReemplazos = false
                viewModel.limpiarCandidatosReemplazo()
            }
        )
    }
}

// Tarjeta resumen de la plantilla activa y su idea principal de juego.
@Composable
private fun ActiveRosterCard(
    activeTemplate: RosterTemplate?,
    playerCount: Int,
    onChangeTemplate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DeepEmerald),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(DeepEmerald, Color(0xFF0E544E), Color(0xFF163D33))
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Editor activo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = activeTemplate?.let { "${it.teamName} - ${it.seasonLabel}" } ?: "Plantilla personalizada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.96f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(label = "$playerCount en pista", background = Emerald)
                    activeTemplate?.playStyle?.takeIf { it.isNotBlank() }?.let {
                        InfoPill(label = it, background = Color(0xFF385A2E))
                    }
                }

                Text(
                    text = activeTemplate?.summary ?: "Selecciona una plantilla para empezar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.84f),
                    lineHeight = 25.sp
                )

                OutlinedButton(
                    onClick = onChangeTemplate,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Cambiar plantilla entera")
                }
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, background: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background.copy(alpha = 0.9f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Tarjeta agregada con medias de la plantilla para comparar el quinteto de un vistazo.
@Composable
private fun RosterStatsOverviewCard(
    totalPlayers: Int,
    loadedPlayers: Int,
    isLoading: Boolean,
    averageStats: RosterAverageSummary?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Media general de la plantilla",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )

            Text(
                text = if (loadedPlayers == 0) {
                    "Cargando estadísticas de la plantilla..."
                } else {
                    "Comparativa agregada con $loadedPlayers de $totalPlayers jugadores con datos."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk
            )

            when {
                isLoading && averageStats == null -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Emerald
                        )
                        Text("Calculando medias...", color = MutedInk)
                    }
                }

                averageStats != null -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("PTS", averageStats.points)
                        StatPill("REB", averageStats.rebounds)
                        StatPill("AST", averageStats.assists)
                        StatPill("BLK", averageStats.blocks)
                        StatPill("3P%", averageStats.threePointPct)
                    }
                }

                else -> {
                    Text(
                        text = "No hay estadísticas disponibles todavía para esta plantilla.",
                        color = MutedInk
                    )
                }
            }
        }
    }
}

// Editor visual que coloca a cada jugador en una zona aproximada de la pista.
@Composable
private fun CourtEditorCard(
    assignments: List<CourtAssignment>,
    selectedPlayerIndex: Int,
    onSelectPlayer: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = CourtPanel),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Quinteto en cancha",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Selecciona una plaza para ver reemplazos de la misma posición.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.74f)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(CourtWood, CourtWoodDark, Color(0xFFAB632F))
                        )
                    )
            ) {
                CourtCanvas()

                val nodeWidth = (maxWidth * 0.23f).coerceIn(76.dp, 92.dp)
                val nodeHeight = 110.dp

                assignments.forEach { assignment ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (maxWidth * assignment.slot.x) - (nodeWidth / 2),
                                y = (maxHeight * assignment.slot.y) - (nodeHeight / 2)
                            )
                            .width(nodeWidth)
                            .height(nodeHeight)
                    ) {
                        CourtPlayerNode(
                            assignment = assignment,
                            isSelected = assignment.playerIndex == selectedPlayerIndex,
                            onClick = { assignment.playerIndex?.let(onSelectPlayer) }
                        )
                    }
                }
            }
        }
    }
}

// Dibuja una media pista estilizada que sirve solo como base visual del editor.
@Composable
private fun CourtCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.86f)
        val softLine = Color.White.copy(alpha = 0.20f)
        val stroke = 4.dp.toPx()
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val keyWidth = width * 0.42f
        val keyHeight = height * 0.20f
        val rimY = height * 0.11f

        drawRoundRect(
            color = softLine,
            cornerRadius = CornerRadius(36f, 36f)
        )

        drawRoundRect(
            color = lineColor,
            style = Stroke(width = stroke),
            cornerRadius = CornerRadius(36f, 36f)
        )

        drawRect(
            color = lineColor,
            topLeft = Offset((width - keyWidth) / 2f, 0f),
            size = androidx.compose.ui.geometry.Size(keyWidth, keyHeight),
            style = Stroke(width = stroke)
        )

        drawCircle(
            color = lineColor,
            radius = width * 0.095f,
            center = Offset(centerX, keyHeight),
            style = Stroke(width = stroke)
        )

        drawArc(
            color = lineColor,
            startAngle = 22f,
            sweepAngle = 136f,
            useCenter = false,
            topLeft = Offset(width * 0.12f, height * 0.02f),
            size = androidx.compose.ui.geometry.Size(width * 0.76f, height * 0.84f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        drawLine(
            color = lineColor,
            start = Offset(width * 0.12f, height * 0.08f),
            end = Offset(width * 0.12f, height * 0.45f),
            strokeWidth = stroke
        )

        drawLine(
            color = lineColor,
            start = Offset(width * 0.88f, height * 0.08f),
            end = Offset(width * 0.88f, height * 0.45f),
            strokeWidth = stroke
        )

        drawCircle(
            color = lineColor,
            radius = width * 0.018f,
            center = Offset(centerX, rimY),
            style = Stroke(width = stroke)
        )

        drawLine(
            color = lineColor,
            start = Offset(centerX - width * 0.07f, rimY - height * 0.03f),
            end = Offset(centerX + width * 0.07f, rimY - height * 0.03f),
            strokeWidth = stroke
        )
    }
}

// Nodo interactivo de un jugador dentro de la pista.
@Composable
private fun CourtPlayerNode(
    assignment: CourtAssignment,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val player = assignment.player
    val borderColor = if (isSelected) Emerald else Color.White.copy(alpha = 0.18f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.97f))
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(enabled = player != null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Ink
        ) {
            Text(
                text = assignment.slot.key,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }

        PlayerAvatar(
            nombreJugador = player?.name ?: assignment.slot.label,
            idJugadorNba = player?.nbaPlayerId,
            modifier = Modifier.size(42.dp),
            colorRespaldo = Emerald
        )

        Text(
            text = player?.let(::compactPlayerName) ?: assignment.slot.label,
            textAlign = TextAlign.Center,
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = player?.salaryTier ?: "Seleccionar",
            textAlign = TextAlign.Center,
            color = MutedInk,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Panel lateral con el jugador seleccionado, su nota y acceso a reemplazos.
@Composable
private fun SelectedPlayerCard(
    player: RosterPlayerTemplate?,
    estadoEstadisticasJugador: com.example.nba_salary_manager.data.model.PlayerStatsUiState?,
    onRetryStats: () -> Unit,
    onShowReplacements: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        if (player == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Selecciona un jugador en la cancha.",
                    color = MutedInk,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Jugador seleccionado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PlayerAvatar(
                        nombreJugador = player.name,
                        idJugadorNba = player.nbaPlayerId,
                        modifier = Modifier.size(62.dp),
                        colorRespaldo = Emerald
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink
                        )
                        Text(
                            text = "Posición bloqueada: ${player.position}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Emerald,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = player.note.ifBlank { "Solo se permite sustituirlo por perfiles de la misma posición." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedInk,
                            lineHeight = 21.sp
                        )
                    }
                }

                when {
                    estadoEstadisticasJugador?.isLoading == true && estadoEstadisticasJugador.summary == null -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Emerald
                            )
                            Text("Cargando estadísticas del jugador...", color = MutedInk)
                        }
                    }

                    estadoEstadisticasJugador?.error != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = estadoEstadisticasJugador.error,
                                color = Color(0xFFB42318),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(onClick = onRetryStats) {
                                Text("Reintentar")
                            }
                        }
                    }

                    estadoEstadisticasJugador?.summary != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Estadisticas individuales",
                                style = MaterialTheme.typography.labelLarge,
                                color = Ink,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatPill("PTS", formatStat(estadoEstadisticasJugador.summary.pointsPerGame))
                                StatPill("REB", formatStat(estadoEstadisticasJugador.summary.reboundsPerGame))
                                StatPill("AST", formatStat(estadoEstadisticasJugador.summary.assistsPerGame))
                                StatPill("BLK", formatStat(estadoEstadisticasJugador.summary.blocksPerGame))
                                StatPill("3P%", formatPercent(estadoEstadisticasJugador.summary.threePointPercentage))
                                StatPill("TEMP", estadoEstadisticasJugador.summary.season.toString())
                            }
                        }
                    }
                }

                Button(
                    onClick = onShowReplacements,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) {
                    Text("Buscar reemplazos similares")
                }
            }
        }
    }
}

// Hoja modal para cambiar entre plantillas actuales y clasicas.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatePickerSheet(
    selectedCategory: RosterTemplateCategory,
    templates: List<RosterTemplate>,
    activeTemplateId: String?,
    onCategoryChange: (RosterTemplateCategory) -> Unit,
    onSelectTemplate: (RosterTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Cambiar plantilla entera",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Text(
                text = "La galería solo aparece aquí para no recargar el editor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedCategory == RosterTemplateCategory.CURRENT,
                    onClick = { onCategoryChange(RosterTemplateCategory.CURRENT) },
                    label = { Text("Actuales") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedCategory == RosterTemplateCategory.CLASSIC,
                    onClick = { onCategoryChange(RosterTemplateCategory.CLASSIC) },
                    label = { Text("Clásicas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Ink,
                        selectedLabelColor = Color.White
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        isActive = template.id == activeTemplateId,
                        onUseTemplate = { onSelectTemplate(template) }
                    )
                }
            }
        }
    }
}

// Hoja modal con candidatos de reemplazo y su comparativa frente al jugador actual.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplacementCandidatesSheet(
    targetPlayer: RosterPlayerTemplate,
    targetStats: PlayerSeasonStats?,
    lockedPosition: String,
    candidates: List<ReplacementCandidate>,
    isLoading: Boolean,
    error: String?,
    onReplace: (Player) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Reemplazos para ${targetPlayer.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftMint
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Solo aparecen perfiles compatibles con $lockedPosition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Emerald,
                        fontWeight = FontWeight.Bold
                    )
                    StatsRow(
                        title = "Jugador actual",
                        stats = targetStats,
                        fallback = "Sin comparativa avanzada disponible"
                    )
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Emerald)
                            Text(
                                text = "Buscando jugadores de la misma posición y comparando estadísticas...",
                                color = MutedInk,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                error != null -> {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFF1F2)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFB42318),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(onClick = onRetry) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reintentar")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(candidates, key = { it.player.id }) { candidate ->
                            ReplacementCandidateCard(
                                candidate = candidate,
                                onReplace = { onReplace(candidate.player) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta individual de un posible reemplazo con accion directa para aplicarlo.
@Composable
private fun ReplacementCandidateCard(
    candidate: ReplacementCandidate,
    onReplace: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerAvatar(
                        nombreJugador = playerFullName(candidate.player),
                        idJugadorNba = candidate.player.id,
                        modifier = Modifier.size(54.dp),
                        colorRespaldo = Emerald
                    )

                    Column {
                        Text(
                            text = playerFullName(candidate.player),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = candidate.similarityLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = listOfNotNull(
                                candidate.player.position?.takeIf { it.isNotBlank() },
                                candidate.player.team?.fullName
                            ).joinToString(" - ").ifBlank { "Sin equipo" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                }

                Button(
                    onClick = onReplace,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aplicar")
                }
            }

            StatsRow(
                title = "Comparativa",
                stats = candidate.stats,
                fallback = "No se pudo cargar la comparativa avanzada"
            )
        }
    }
}

// Fila compacta de estadisticas que se reutiliza en comparativas y resúmenes.
@Composable
private fun StatsRow(
    title: String,
    stats: PlayerSeasonStats?,
    fallback: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Ink,
            fontWeight = FontWeight.ExtraBold
        )

        if (stats == null) {
            Text(
                text = fallback,
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill("PTS", formatStat(stats.pointsPerGame))
                StatPill("REB", formatStat(stats.reboundsPerGame))
                StatPill("AST", formatStat(stats.assistsPerGame))
                StatPill("BLK", formatStat(stats.blocksPerGame))
                StatPill("3P%", formatPercent(stats.threePointPercentage))
                StatPill("TEMP", stats.season.toString())
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PageBackground
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MutedInk
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
        }
    }
}

// Tarjeta de la galeria de plantillas disponibles.
@Composable
private fun TemplateCard(
    template: RosterTemplate,
    isActive: Boolean,
    onUseTemplate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) SoftMint else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.teamName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Text(
                        text = template.seasonLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                }

                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Emerald
                    ) {
                        Text(
                            text = "Activa",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = template.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
                lineHeight = 28.sp
            )

            Text(
                text = template.playStyle,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
                lineHeight = 22.sp
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                template.jugadores.take(5).forEach { player ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PageBackground
                    ) {
                        Text(
                            text = "${player.position} ${compactPlayerName(player)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            color = Emerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = onUseTemplate,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text(if (isActive) "Recargar base" else "Usar esta plantilla")
            }
        }
    }
}

private data class CourtSlot(
    val key: String,
    val label: String,
    val x: Float,
    val y: Float
)

private data class CourtAssignment(
    val slot: CourtSlot,
    val playerIndex: Int?,
    val player: RosterPlayerTemplate?
)

private data class RosterAverageSummary(
    val points: String,
    val rebounds: String,
    val assists: String,
    val blocks: String,
    val threePointPct: String
)

private val COURT_SLOTS = listOf(
    CourtSlot("PG", "Base", 0.50f, 0.79f),
    CourtSlot("SG", "Escolta", 0.24f, 0.66f),
    CourtSlot("SF", "Alero", 0.76f, 0.66f),
    CourtSlot("PF", "Ala-pivot", 0.25f, 0.38f),
    CourtSlot("C", "Pivot", 0.50f, 0.22f)
)

// Intenta colocar cada jugador en el hueco de pista mas coherente segun su posicion.
private fun buildCourtAssignments(jugadores: List<RosterPlayerTemplate>): List<CourtAssignment> {
    val remainingIndexes = jugadores.indices.toMutableSet()

    return COURT_SLOTS.map { slot ->
        val exactMatch = remainingIndexes.firstOrNull { index ->
            slot.key in normalizedPositionTokens(jugadores[index].position)
        }
        val familyMatch = exactMatch ?: remainingIndexes.firstOrNull { index ->
            matchesPositionFamily(normalizedPositionTokens(jugadores[index].position), slot.key)
        }
        val fallback = familyMatch ?: remainingIndexes.firstOrNull()

        if (fallback != null) {
            remainingIndexes.remove(fallback)
        }

        CourtAssignment(
            slot = slot,
            playerIndex = fallback,
            player = fallback?.let(jugadores::get)
        )
    }
}

private fun normalizedPositionTokens(position: String): Set<String> {
    if (position.isBlank()) return emptySet()
    return position
        .uppercase(Locale.US)
        .replace("/", "-")
        .split("-", " ")
        .mapNotNull { token ->
            when (token.trim()) {
                "PG", "SG", "SF", "PF", "C", "G", "F" -> token.trim()
                else -> null
            }
        }
        .toSet()
}

// Acepta equivalencias amplias como G o F cuando no hay coincidencia exacta.
private fun matchesPositionFamily(tokens: Set<String>, slot: String): Boolean {
    return when (slot) {
        "PG" -> tokens.any { it == "PG" || it == "G" }
        "SG" -> tokens.any { it == "SG" || it == "G" }
        "SF" -> tokens.any { it == "SF" || it == "F" }
        "PF" -> tokens.any { it == "PF" || it == "F" }
        "C" -> "C" in tokens
        else -> false
    }
}

private fun compactPlayerName(player: RosterPlayerTemplate): String {
    val parts = player.name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.size <= 1) return player.name.ifBlank { "Jugador" }
    val firstInitial = parts.first().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val lastName = parts.drop(1).joinToString(" ")
    return "$firstInitial. $lastName"
}

private fun playerFullName(player: Player): String {
    return "${player.firstName} ${player.lastName}".trim()
}

private fun formatStat(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
}

private fun formatPercent(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.0f%%", it) } ?: "-"
}

// Calcula una media sencilla de la plantilla para el panel comparativo superior.
private fun List<PlayerSeasonStats>.toAverageSummaryOrNull(): RosterAverageSummary? {
    if (isEmpty()) return null

    fun avg(selector: (PlayerSeasonStats) -> Double): Double {
        return sumOf(selector) / size.toDouble()
    }

    val threePointValues = mapNotNull { it.threePointPercentage }
    val threePointAverage = if (threePointValues.isEmpty()) null else threePointValues.sum() / threePointValues.size

    return RosterAverageSummary(
        points = formatStat(avg { it.pointsPerGame }),
        rebounds = formatStat(avg { it.reboundsPerGame }),
        assists = formatStat(avg { it.assistsPerGame }),
        blocks = formatStat(avg { it.blocksPerGame }),
        threePointPct = formatPercent(threePointAverage)
    )
}

private fun Dp.coerceIn(min: Dp, max: Dp): Dp {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}
