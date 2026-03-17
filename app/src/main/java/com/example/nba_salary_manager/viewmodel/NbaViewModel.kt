package com.example.nba_salary_manager.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nba_salary_manager.data.api.RetrofitClient
import com.example.nba_salary_manager.data.model.Game
import com.example.nba_salary_manager.data.model.Player
import com.example.nba_salary_manager.data.model.Team
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NbaViewModel : ViewModel() {

    // ── Teams state ──
    var teams by mutableStateOf<List<Team>>(emptyList())
        private set
    var teamsLoading by mutableStateOf(false)
        private set
    var teamsError by mutableStateOf<String?>(null)
        private set

    // ── Players state ──
    var players by mutableStateOf<List<Player>>(emptyList())
        private set
    var playersLoading by mutableStateOf(false)
        private set
    var playersError by mutableStateOf<String?>(null)
        private set
    var playerSearchQuery by mutableStateOf("")
        private set
    var playersNextCursor by mutableStateOf<Int?>(null)
        private set
    var playersHasMore by mutableStateOf(true)
        private set

    // ── Games state ──
    var games by mutableStateOf<List<Game>>(emptyList())
        private set
    var gamesLoading by mutableStateOf(false)
        private set
    var gamesError by mutableStateOf<String?>(null)
        private set
    var selectedSeason by mutableStateOf(2026)
        private set
    var gamesNextCursor by mutableStateOf<Int?>(null)
        private set
    var gamesHasMore by mutableStateOf(true)
        private set

    private val api = RetrofitClient.api

    init {
        loadTeams()
        loadPlayers()
        loadGames()
    }

    // ── Teams ──

    fun loadTeams() {
        viewModelScope.launch {
            teamsLoading = true
            teamsError = null
            try {
                val response = api.getTeams()
                teams = response.data
            } catch (e: Exception) {
                teamsError = e.message ?: "Error desconocido al cargar equipos"
            }
            teamsLoading = false
        }
    }

    // ── Players ──

    fun updatePlayerSearch(query: String) {
        playerSearchQuery = query
    }

    fun searchPlayers() {
        viewModelScope.launch {
            playersLoading = true
            playersError = null
            playersNextCursor = null
            playersHasMore = true
            try {
                val response = api.getPlayers(
                    search = playerSearchQuery.ifBlank { null },
                    perPage = 25
                )
                players = response.data.sortedByDescending { it.id }
                playersNextCursor = response.meta?.nextCursor
                playersHasMore = response.meta?.nextCursor != null
            } catch (e: Exception) {
                playersError = e.message ?: "Error desconocido al cargar jugadores"
            }
            playersLoading = false
        }
    }

    fun loadPlayers() {
        searchPlayers()
    }

    fun loadMorePlayers() {
        val cursor = playersNextCursor ?: return
        viewModelScope.launch {
            playersLoading = true
            try {
                val response = api.getPlayers(
                    search = playerSearchQuery.ifBlank { null },
                    perPage = 25,
                    cursor = cursor
                )
                players = (players + response.data).sortedByDescending { it.id }
                playersNextCursor = response.meta?.nextCursor
                playersHasMore = response.meta?.nextCursor != null
            } catch (e: Exception) {
                playersError = e.message ?: "Error cargando más jugadores"
            }
            playersLoading = false
        }
    }

    // ── Games ──

    fun updateSeason(season: Int) {
        selectedSeason = season
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            gamesLoading = true
            gamesError = null
            gamesNextCursor = null
            gamesHasMore = true
            try {
                val apiSeason = if (selectedSeason == 2026) 2025 else selectedSeason
                
                var startDate: String? = null
                var endDate: String? = null
                if (selectedSeason == 2026) {
                    val cal = Calendar.getInstance()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    startDate = sdf.format(cal.time)
                    
                    cal.add(Calendar.DAY_OF_YEAR, 2)
                    endDate = sdf.format(cal.time)
                }
                
                val response = api.getGames(
                    seasons = listOf(apiSeason),
                    perPage = 50,
                    startDate = startDate,
                    endDate = endDate
                )
                games = response.data.sortedByDescending { it.date }
                gamesNextCursor = response.meta?.nextCursor
                gamesHasMore = response.meta?.nextCursor != null
            } catch (e: Exception) {
                gamesError = e.message ?: "Error desconocido al cargar partidos"
            }
            gamesLoading = false
        }
    }

    fun loadMoreGames() {
        val cursor = gamesNextCursor ?: return
        viewModelScope.launch {
            gamesLoading = true
            try {
                val apiSeason = if (selectedSeason == 2026) 2025 else selectedSeason
                
                var startDate: String? = null
                var endDate: String? = null
                if (selectedSeason == 2026) {
                    val cal = Calendar.getInstance()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    startDate = sdf.format(cal.time)
                    
                    cal.add(Calendar.DAY_OF_YEAR, 2)
                    endDate = sdf.format(cal.time)
                }

                val response = api.getGames(
                    seasons = listOf(apiSeason),
                    perPage = 50,
                    cursor = cursor,
                    startDate = startDate,
                    endDate = endDate
                )
                games = (games + response.data).sortedByDescending { it.date }
                gamesNextCursor = response.meta?.nextCursor
                gamesHasMore = response.meta?.nextCursor != null
            } catch (e: Exception) {
                gamesError = e.message ?: "Error cargando más partidos"
            }
            gamesLoading = false
        }
    }
}
