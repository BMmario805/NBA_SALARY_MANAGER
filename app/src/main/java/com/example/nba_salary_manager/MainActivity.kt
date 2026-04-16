package com.example.nba_salary_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.nba_salary_manager.ui.screens.GamesScreen
import com.example.nba_salary_manager.ui.screens.PlayersScreen
import com.example.nba_salary_manager.ui.screens.RosterTemplatesScreen
import com.example.nba_salary_manager.ui.screens.TeamsScreen
import com.example.nba_salary_manager.ui.theme.NBA_SALARY_MANAGERTheme
import com.example.nba_salary_manager.ui.components.AuthDialog
import com.example.nba_salary_manager.ui.components.UserAccessButton
import com.example.nba_salary_manager.ui.components.UserSettingsSheet
import com.example.nba_salary_manager.viewmodel.AuthViewModel
import com.example.nba_salary_manager.viewmodel.NbaViewModel

class MainActivity : ComponentActivity() {
    private val viewModelAutenticacion by lazy { AuthViewModel(applicationContext) }
    private val viewModelNba by lazy { NbaViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NBA_SALARY_MANAGERTheme {
                NBA_SALARY_MANAGERApp(viewModelNba, viewModelAutenticacion)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBA_SALARY_MANAGERApp(viewModelNba: NbaViewModel, viewModelAutenticacion: AuthViewModel) {
    val usuarioActual by viewModelAutenticacion.currentUser
    var destinoActual by rememberSaveable { mutableStateOf<DestinosApp>(DestinosApp.EQUIPOS) }
    var mostrarDialogoAuth by rememberSaveable { mutableStateOf(false) }
    var mostrarAjustesUsuario by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(usuarioActual) {
        if (usuarioActual == null) {
            mostrarAjustesUsuario = false
        } else {
            mostrarDialogoAuth = false
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            DestinosApp.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == destinoActual,
                    onClick = { destinoActual = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Gestor salarial NBA") },
                    navigationIcon = {
                        UserAccessButton(
                            user = usuarioActual,
                            onClick = {
                                if (usuarioActual == null) {
                                    mostrarDialogoAuth = true
                                } else {
                                    mostrarAjustesUsuario = true
                                }
                            }
                        )
                    }
                )
            }
        ) { innerPadding ->
            when (destinoActual) {
                DestinosApp.EQUIPOS -> TeamsScreen(
                    viewModel = viewModelNba,
                    modifier = Modifier.padding(innerPadding)
                )
                DestinosApp.JUGADORES -> PlayersScreen(
                    viewModel = viewModelNba,
                    modifier = Modifier.padding(innerPadding)
                )
                DestinosApp.PARTIDOS -> GamesScreen(
                    viewModel = viewModelNba,
                    modifier = Modifier.padding(innerPadding)
                )
                DestinosApp.PLANTILLAS -> RosterTemplatesScreen(
                    viewModel = viewModelNba,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (mostrarDialogoAuth) {
        AuthDialog(
            viewModel = viewModelAutenticacion,
            onDismiss = { mostrarDialogoAuth = false },
            onAuthenticated = { mostrarDialogoAuth = false }
        )
    }

    if (mostrarAjustesUsuario && usuarioActual != null) {
        UserSettingsSheet(
            user = usuarioActual!!,
            viewModel = viewModelAutenticacion,
            onDismiss = { mostrarAjustesUsuario = false }
        )
    }
}

enum class DestinosApp(
    val label: String,
    val icon: ImageVector,
) {
    EQUIPOS("Equipos", Icons.Default.Home),
    JUGADORES("Jugadores", Icons.Default.Person),
    PARTIDOS("Partidos", Icons.Default.DateRange),
    PLANTILLAS("Plantillas", Icons.Default.Edit),
}
