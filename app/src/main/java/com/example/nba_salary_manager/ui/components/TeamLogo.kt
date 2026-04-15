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
    abbreviation: String,
    teamName: String,
    modifier: Modifier = Modifier,
    fallbackColor: Color
) {
    val logoCandidates = remember(abbreviation) { buildLogoCandidates(abbreviation) }
    var candidateIndex by remember(abbreviation) { mutableStateOf(0) }
    var imageFailed by remember(abbreviation, candidateIndex) { mutableStateOf(false) }
    val logoUrl = logoCandidates.getOrNull(candidateIndex)

    if (!logoUrl.isNullOrBlank() && !imageFailed) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Logo $teamName",
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit,
            onError = {
                if (candidateIndex < logoCandidates.lastIndex) {
                    candidateIndex += 1
                    imageFailed = false
                } else {
                    imageFailed = true
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
                            fallbackColor,
                            fallbackColor.copy(alpha = 0.72f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = abbreviation,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

private fun buildLogoCandidates(abbreviation: String): List<String> {
    val normalized = abbreviation.uppercase()
    val nbaTeamId = NBA_TEAM_IDS_BY_ABBR[normalized] ?: return emptyList()
    val espnCode = ESPN_TEAM_CODES_BY_ABBR[normalized] ?: normalized.lowercase()

    return listOf(
        "https://cdn.nba.com/logos/nba/$nbaTeamId/global/L/logo.png",
        "https://cdn.nba.com/logos/nba/$nbaTeamId/primary/L/logo.png",
        "https://cdn.nba.com/logos/nba/$nbaTeamId/global/D/logo.png",
        "https://cdn.nba.com/logos/nba/$nbaTeamId/primary/D/logo.png",
        "https://a.espncdn.com/i/teamlogos/nba/500/$espnCode.png"
    )
}

private val NBA_TEAM_IDS_BY_ABBR = mapOf(
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

private val ESPN_TEAM_CODES_BY_ABBR = mapOf(
    "GSW" to "gs",
    "NYK" to "ny",
    "NOP" to "no",
    "SAS" to "sa"
)
