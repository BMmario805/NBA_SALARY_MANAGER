package com.example.nba_salary_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nba_salary_manager.data.api.PlayerPhotoRepository

@Composable
fun PlayerAvatar(
    playerName: String,
    nbaPlayerId: Int? = null,
    modifier: Modifier = Modifier,
    fallbackColor: Color
) {
    val photoUrlState = produceState<String?>(initialValue = null, key1 = playerName, key2 = nbaPlayerId) {
        value = try {
            PlayerPhotoRepository.getHeadshotUrl(nbaPlayerId)
                ?: PlayerPhotoRepository.getPhotoUrl(playerName)
        } catch (_: Exception) {
            null
        }
    }

    val photoUrl = photoUrlState.value
    var imageFailed by remember(playerName, nbaPlayerId, photoUrl) {
        mutableStateOf(false)
    }

    if (!photoUrl.isNullOrBlank() && !imageFailed) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = playerName,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
            onError = { imageFailed = true }
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(fallbackColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = playerInitials(playerName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun playerInitials(playerName: String): String {
    return playerName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
}
