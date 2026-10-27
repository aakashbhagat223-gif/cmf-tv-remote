package com.cmf.tvremote.data.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

data class MediaSessionTelemetry(
    val title: String = "Nothing reported",
    val artist: String = "Playback idle or restricted",
    val packageName: String = "",
    val isPlaying: Boolean = false
)

object TvMediaScraperService {
    private val _sessionState = MutableStateFlow(MediaSessionTelemetry())
    val sessionState = _sessionState.asStateFlow()

    private var pollerJob: Job? = null

    fun startPolling(scope: CoroutineScope) {
        pollerJob?.cancel()
        pollerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                pollMediaSession()
                delay(3000) // Poll cadence
            }
        }
    }

    fun stopPolling() {
        pollerJob?.cancel()
        pollerJob = null
    }

    private suspend fun pollMediaSession() {
        runCatching {
            val dumpOutput = AdbBridge.executeShellCommand("dumpsys media_session") ?: return
            val parsed = extractMetadata(dumpOutput)
            _sessionState.value = parsed
        }
    }

    private fun extractMetadata(raw: String): MediaSessionTelemetry {
        var mediaTitle = "Nothing reported"
        var mediaArtist = "This TV link doesn't share media info yet"
        var activePkg = ""
        var playing = false

        // Extract active sessions
        val lines = raw.lines()
        for (i in lines.indices) {
            val line = lines[i].trim()

            if (line.startsWith("package=")) {
                activePkg = line.substringAfter("package=")
            }

            if (line.contains("state=PlaybackState") || line.contains("state=3")) {
                playing = line.contains("state=3") // 3 is PlaybackState.STATE_PLAYING
            }

            if (line.startsWith("description=")) {
                val desc = line.substringAfter("description=")
                if (desc.isNotBlank() && desc != "null") {
                    val parts = desc.split(",")
                    if (parts.isNotEmpty()) mediaTitle = parts[0].trim()
                    if (parts.size > 1) mediaArtist = parts[1].trim()
                }
            }
        }

        return MediaSessionTelemetry(
            title = mediaTitle,
            artist = mediaArtist,
            packageName = activePkg,
            isPlaying = playing
        )
    }
}
