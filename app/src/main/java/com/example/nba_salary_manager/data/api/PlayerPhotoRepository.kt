package com.example.nba_salary_manager.data.api

import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private data class NbaPlayerDirectoryEntry(
    val personId: Int,
    val isActive: Boolean
)

object PlayerPhotoRepository {
    private const val NO_PLAYER = -1
    private const val DIRECTORY_RETRY_DELAY_MS = 60_000L
    private const val DIRECTORY_URL =
        "https://raw.githubusercontent.com/swar/nba_api/master/src/nba_api/stats/library/data.py"

    private val photoUrlCache = ConcurrentHashMap<String, String>()
    private val playerIdCache = ConcurrentHashMap<String, Int>()
    private val directoryByName = ConcurrentHashMap<String, NbaPlayerDirectoryEntry>()
    private val loadLock = Any()
    private val directoryLineRegex = Regex(
        """\[(\d+),\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)",\s*(True|False)\]"""
    )

    @Volatile
    private var directoryLoaded = false

    @Volatile
    private var lastDirectoryLoadAttemptAt = 0L

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    fun getHeadshotUrl(playerId: Int?): String? {
        return playerId?.takeIf { it > 0 }?.let(::buildHeadshotUrl)
    }

    suspend fun getPhotoUrl(playerName: String): String? {
        val normalizedName = normalizeName(playerName)
        if (normalizedName.isBlank()) return null

        photoUrlCache[normalizedName]?.let { return it }
        playerIdCache[normalizedName]?.let { cachedId ->
            return cachedId.takeUnless { it == NO_PLAYER }?.let(::buildHeadshotUrl)
        }

        return withContext(Dispatchers.IO) {
            try {
                ensureDirectoryLoaded()
                if (!directoryLoaded) return@withContext null

                val playerId = directoryByName[normalizedName]?.personId ?: NO_PLAYER
                playerIdCache[normalizedName] = playerId

                playerId.takeUnless { it == NO_PLAYER }?.let(::buildHeadshotUrl)?.also {
                    photoUrlCache[normalizedName] = it
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun ensureDirectoryLoaded() {
        if (directoryLoaded) return

        synchronized(loadLock) {
            if (directoryLoaded) return
            if (!shouldAttemptDirectoryLoad()) return
            lastDirectoryLoadAttemptAt = System.currentTimeMillis()

            try {
                val request = Request.Builder()
                    .url(DIRECTORY_URL)
                    .addHeader("Accept", "text/plain, */*")
                    .build()

                val responseBody = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return
                    response.body?.string() ?: return
                }

                directoryLineRegex.findAll(responseBody).forEach { match ->
                    val personId = match.groupValues[1].toIntOrNull() ?: return@forEach
                    val lastName = unescape(match.groupValues[2])
                    val firstName = unescape(match.groupValues[3])
                    val fullName = unescape(match.groupValues[4])
                    val isActive = match.groupValues[5].equals("True", ignoreCase = true)
                    val entry = NbaPlayerDirectoryEntry(
                        personId = personId,
                        isActive = isActive
                    )

                    if (fullName.isNotBlank()) {
                        registerEntry(fullName, entry)
                    }
                    val firstLast = listOf(firstName, lastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    if (firstLast.isNotBlank()) {
                        registerEntry(firstLast, entry)
                    }
                }

                if (directoryByName.isNotEmpty()) {
                    directoryLoaded = true
                }
            } catch (_: Exception) {
                // Fall back to initials if the player directory is temporarily unavailable.
            }
        }
    }

    private fun shouldAttemptDirectoryLoad(): Boolean {
        return System.currentTimeMillis() - lastDirectoryLoadAttemptAt >= DIRECTORY_RETRY_DELAY_MS
    }

    private fun registerEntry(rawName: String, newEntry: NbaPlayerDirectoryEntry) {
        val normalizedName = normalizeName(rawName)
        if (normalizedName.isBlank()) return

        val currentEntry = directoryByName[normalizedName]
        if (currentEntry == null || isBetterMatch(newEntry, currentEntry)) {
            directoryByName[normalizedName] = newEntry
        }
    }

    private fun isBetterMatch(
        candidate: NbaPlayerDirectoryEntry,
        current: NbaPlayerDirectoryEntry
    ): Boolean {
        if (candidate.isActive != current.isActive) {
            return candidate.isActive
        }
        return candidate.personId > current.personId
    }

    private fun buildHeadshotUrl(playerId: Int): String {
        return "https://cdn.nba.com/headshots/nba/latest/260x190/$playerId.png"
    }

    private fun unescape(value: String): String {
        return value
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace(Regex("""\\u([0-9a-fA-F]{4})""")) {
                it.groupValues[1].toInt(16).toChar().toString()
            }
    }

    private fun normalizeName(name: String): String {
        val normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        return normalized
            .replace(" iii", " 3")
            .replace(" ii", " 2")
            .replace(" iv", " 4")
            .replace(" jr", " jr")
            .replace(" sr", " sr")
    }
}
