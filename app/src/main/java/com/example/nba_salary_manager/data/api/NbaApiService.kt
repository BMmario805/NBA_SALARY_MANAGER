package com.example.nba_salary_manager.data.api

import com.example.nba_salary_manager.data.model.ApiResponse
import com.example.nba_salary_manager.data.model.Game
import com.example.nba_salary_manager.data.model.Player
import com.example.nba_salary_manager.data.model.PlayerStats
import com.example.nba_salary_manager.data.model.Team
import retrofit2.http.GET
import retrofit2.http.Query

// Contrato minimo con la API principal de balldontlie.
interface NbaApiService {

    @GET("teams")
    suspend fun obtenerEquipos(): ApiResponse<Team>

    @GET("players")
    suspend fun obtenerJugadores(
        @Query("search") search: String? = null,
        @Query("per_page") perPage: Int = 25,
        @Query("cursor") cursor: Int? = null
    ): ApiResponse<Player>

    @GET("games")
    suspend fun obtenerPartidos(
        @Query("seasons[]") seasons: List<Int>? = null,
        @Query("per_page") perPage: Int = 25,
        @Query("cursor") cursor: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ApiResponse<Game>

    @GET("stats")
    suspend fun obtenerEstadisticas(
        @Query("player_ids[]") playerIds: List<Int>? = null,
        @Query("game_ids[]") gameIds: List<Int>? = null,
        @Query("seasons[]") seasons: List<Int>? = null,
        @Query("per_page") perPage: Int = 25,
        @Query("cursor") cursor: Int? = null
    ): ApiResponse<PlayerStats>
}
