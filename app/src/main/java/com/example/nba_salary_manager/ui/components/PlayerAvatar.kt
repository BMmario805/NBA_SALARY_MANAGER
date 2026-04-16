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
    nombreJugador: String,
    idJugadorNba: Int? = null,
    modifier: Modifier = Modifier,
    colorRespaldo: Color
) {
    val estadoUrlFoto = produceState<String?>(initialValue = null, key1 = nombreJugador, key2 = idJugadorNba) {
        value = try {
            PlayerPhotoRepository.getHeadshotUrl(idJugadorNba)
                ?: PlayerPhotoRepository.getPhotoUrl(nombreJugador)
        } catch (_: Exception) {
            null
        }
    }

    val urlFoto = estadoUrlFoto.value
    var falloImagen by remember(nombreJugador, idJugadorNba, urlFoto) {
        mutableStateOf(false)
    }

    if (!urlFoto.isNullOrBlank() && !falloImagen) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(urlFoto)
                .crossfade(true)
                .build(),
            contentDescription = nombreJugador,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
            onError = { falloImagen = true }
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(colorRespaldo),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = inicialesJugador(nombreJugador),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun inicialesJugador(nombreJugador: String): String {
    return nombreJugador
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
}
