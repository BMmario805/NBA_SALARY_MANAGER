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
    private val authViewModel by lazy { AuthViewModel(applicationContext) }
    private val nbaViewModel by lazy { NbaViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NBA_SALARY_MANAGERTheme {
                NBA_SALARY_MANAGERApp(nbaViewModel, authViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBA_SALARY_MANAGERApp(nbaViewModel: NbaViewModel, authViewModel: AuthViewModel) {
    val currentUser by authViewModel.currentUser
    var currentDestination by rememberSaveable { mutableStateOf<AppDestinations>(AppDestinations.TEAMS) }
    var showAuthDialog by rememberSaveable { mutableStateOf(false) }
    var showUserSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            showUserSettings = false
        } else {
            showAuthDialog = false
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("NBA Manager") },
                    navigationIcon = {
                        UserAccessButton(
                            user = currentUser,
                            onClick = {
                                if (currentUser == null) {
                                    showAuthDialog = true
                                } else {
                                    showUserSettings = true
                                }
                            }
                        )
                    }
                )
            }
        ) { innerPadding ->
            when (currentDestination) {
                AppDestinations.TEAMS -> TeamsScreen(
                    viewModel = nbaViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.PLAYERS -> PlayersScreen(
                    viewModel = nbaViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.GAMES -> GamesScreen(
                    viewModel = nbaViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.ROSTERS -> RosterTemplatesScreen(
                    viewModel = nbaViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            viewModel = authViewModel,
            onDismiss = { showAuthDialog = false },
            onAuthenticated = { showAuthDialog = false }
        )
    }

    if (showUserSettings && currentUser != null) {
        UserSettingsSheet(
            user = currentUser!!,
            viewModel = authViewModel,
            onDismiss = { showUserSettings = false }
        )
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    TEAMS("Equipos", Icons.Default.Home),
    PLAYERS("Jugadores", Icons.Default.Person),
    GAMES("Partidos", Icons.Default.DateRange),
    ROSTERS("Plantillas", Icons.Default.Edit),
}
