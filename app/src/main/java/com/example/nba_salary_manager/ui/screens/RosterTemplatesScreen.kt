package com.example.nba_salary_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nba_salary_manager.data.model.RosterPlayerTemplate
import com.example.nba_salary_manager.data.model.RosterTemplate
import com.example.nba_salary_manager.data.model.RosterTemplateCategory
import com.example.nba_salary_manager.ui.components.PlayerAvatar
import com.example.nba_salary_manager.viewmodel.NbaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterTemplatesScreen(viewModel: NbaViewModel, modifier: Modifier = Modifier) {
    val selectedCategory = viewModel.selectedRosterCategory
    val templates = viewModel.filteredRosterTemplates
    val activeTemplate = viewModel.activeRosterTemplate
    val editableRosterName = viewModel.editableRosterName
    val editableRosterPlayStyle = viewModel.editableRosterPlayStyle
    val editablePlayers = viewModel.editableRosterPlayers

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F766E), Color(0xFF22C55E))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        "Plantillas NBA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Carga una base actual o clasica y ajusta tu roster jugador a jugador.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Ejemplos disponibles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == RosterTemplateCategory.CURRENT,
                        onClick = { viewModel.updateRosterCategory(RosterTemplateCategory.CURRENT) },
                        label = { Text("Actuales") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F766E),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedCategory == RosterTemplateCategory.CLASSIC,
                        onClick = { viewModel.updateRosterCategory(RosterTemplateCategory.CLASSIC) },
                        label = { Text("Clasicas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF14532D),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        items(templates, key = { it.id }) { template ->
            TemplateCard(
                template = template,
                isActive = template.id == activeTemplate?.id,
                onUseTemplate = { viewModel.useRosterTemplate(template.id) }
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Editor de plantilla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                activeTemplate?.let { template ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F766E).copy(alpha = 0.10f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Base cargada: ${template.teamName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                "${template.seasonLabel} · ${template.players.size} jugadores",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { viewModel.useRosterTemplate(template.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E))
                            ) {
                                Text("Restaurar ejemplo")
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = editableRosterName,
                    onValueChange = viewModel::updateEditableRosterName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre de la plantilla") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editableRosterPlayStyle,
                    onValueChange = viewModel::updateEditableRosterPlayStyle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Idea de juego") },
                    supportingText = {
                        Text("Ejemplo: small ball, doble interior, defensa fisica, cinco abiertos")
                    }
                )
            }
        }

        itemsIndexed(editablePlayers) { index, player ->
            EditableRosterPlayerCard(
                index = index,
                player = player,
                onNameChange = { value ->
                    viewModel.updateEditableRosterPlayer(index) { current ->
                        current.copy(
                            name = value,
                            nbaPlayerId = current.nbaPlayerId?.takeIf { value == current.name }
                        )
                    }
                },
                onPositionChange = { value ->
                    viewModel.updateEditableRosterPlayer(index) { current ->
                        current.copy(position = value)
                    }
                },
                onRoleChange = { value ->
                    viewModel.updateEditableRosterPlayer(index) { current ->
                        current.copy(role = value)
                    }
                },
                onSalaryTierChange = { value ->
                    viewModel.updateEditableRosterPlayer(index) { current ->
                        current.copy(salaryTier = value)
                    }
                },
                onNoteChange = { value ->
                    viewModel.updateEditableRosterPlayer(index) { current ->
                        current.copy(note = value)
                    }
                },
                onRemove = { viewModel.removeEditableRosterPlayer(index) }
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(
                    onClick = viewModel::addEditableRosterPlayer,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anadir jugador")
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: RosterTemplate,
    isActive: Boolean,
    onUseTemplate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                Color(0xFFECFDF5)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        template.teamName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        template.seasonLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF0F766E)
                    ) {
                        Text(
                            "Cargada",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                template.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                template.playStyle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                template.players.take(4).forEach { player ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerAvatar(
                                    playerName = player.name,
                                    nbaPlayerId = player.nbaPlayerId,
                                    modifier = Modifier.width(28.dp).height(28.dp),
                                    fallbackColor = Color(0xFF0F766E)
                                )
                                Text(player.name)
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF0F766E).copy(alpha = 0.10f),
                            labelColor = Color(0xFF0F766E)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onUseTemplate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text(if (isActive) "Recargar como base" else "Usar como ejemplo")
            }
        }
    }
}

@Composable
private fun EditableRosterPlayerCard(
    index: Int,
    player: RosterPlayerTemplate,
    onNameChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onSalaryTierChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        PlayerAvatar(
                            playerName = player.name,
                            nbaPlayerId = player.nbaPlayerId,
                            modifier = Modifier.width(52.dp).height(52.dp),
                            fallbackColor = Color(0xFF0F766E)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F766E)
                        ) {
                            Text(
                                "#${index + 1}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (player.name.isBlank()) "Jugador pendiente" else player.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar jugador",
                        tint = Color(0xFFB91C1C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = player.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Jugador") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = player.position,
                    onValueChange = onPositionChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Posicion") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = player.role,
                    onValueChange = onRoleChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Rol") },
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = player.salaryTier,
                onValueChange = onSalaryTierChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nivel salarial") },
                supportingText = { Text("Ejemplo: Max, Core, Value, Rookie, Vet minimum") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = player.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nota tactica") },
                supportingText = { Text("Uso ideal del jugador dentro de la plantilla") }
            )
        }
    }
}
