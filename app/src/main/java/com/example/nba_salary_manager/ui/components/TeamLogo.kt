package com.example.nba_salary_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun TeamLogo(
    abreviatura: String,
    nombreEquipo: String,
    modifier: Modifier = Modifier,
    colorRespaldo: Color
) {
    val candidatosLogo = remember(abreviatura) { construirCandidatosLogo(abreviatura) }
    var indiceCandidato by remember(abreviatura) { mutableStateOf(0) }
    var falloImagen by remember(abreviatura, indiceCandidato) { mutableStateOf(false) }
    val urlLogo = candidatosLogo.getOrNull(indiceCandidato)

    if (!urlLogo.isNullOrBlank() && !falloImagen) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(urlLogo)
                .crossfade(true)
                .build(),
            contentDescription = "Logo de $nombreEquipo",
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit,
            onError = {
                if (indiceCandidato < candidatosLogo.lastIndex) {
                    indiceCandidato += 1
                    falloImagen = false
                } else {
                    falloImagen = true
                }
            }
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colorRespaldo,
                            colorRespaldo.copy(alpha = 0.72f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = abreviatura,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

private fun construirCandidatosLogo(abreviatura: String): List<String> {
    val abreviaturaNormalizada = abreviatura.uppercase()
    val idEquipoNba = IDS_EQUIPOS_NBA_POR_ABREVIATURA[abreviaturaNormalizada] ?: return emptyList()
    val codigoEspn = CODIGOS_EQUIPOS_ESPN_POR_ABREVIATURA[abreviaturaNormalizada]
        ?: abreviaturaNormalizada.lowercase()

    return listOf(
        "https://cdn.nba.com/logos/nba/$idEquipoNba/global/L/logo.png",
        "https://cdn.nba.com/logos/nba/$idEquipoNba/primary/L/logo.png",
        "https://cdn.nba.com/logos/nba/$idEquipoNba/global/D/logo.png",
        "https://cdn.nba.com/logos/nba/$idEquipoNba/primary/D/logo.png",
        "https://a.espncdn.com/i/teamlogos/nba/500/$codigoEspn.png"
    )
}

private val IDS_EQUIPOS_NBA_POR_ABREVIATURA = mapOf(
    "ATL" to 1610612737,
    "BOS" to 1610612738,
    "BKN" to 1610612751,
    "CHA" to 1610612766,
    "CHI" to 1610612741,
    "CLE" to 1610612739,
    "DAL" to 1610612742,
    "DEN" to 1610612743,
    "DET" to 1610612765,
    "GSW" to 1610612744,
    "HOU" to 1610612745,
    "IND" to 1610612754,
    "LAC" to 1610612746,
    "LAL" to 1610612747,
    "MEM" to 1610612763,
    "MIA" to 1610612748,
    "MIL" to 1610612749,
    "MIN" to 1610612750,
    "NOP" to 1610612740,
    "NYK" to 1610612752,
    "OKC" to 1610612760,
    "ORL" to 1610612753,
    "PHI" to 1610612755,
    "PHX" to 1610612756,
    "POR" to 1610612757,
    "SAC" to 1610612758,
    "SAS" to 1610612759,
    "TOR" to 1610612761,
    "UTA" to 1610612762,
    "WAS" to 1610612764
)

private val CODIGOS_EQUIPOS_ESPN_POR_ABREVIATURA = mapOf(
    "GSW" to "gs",
    "NYK" to "ny",
    "NOP" to "no",
    "SAS" to "sa"
)
