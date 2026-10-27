package com.cmf.tvremote.data.network

import android.view.KeyEvent
import com.cmf.tvremote.domain.model.RemoteCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object InputTransmissionManager {

    /**
     * Dispatches direct text or character entries to Google TV.
     * Uses Protocol v2 RemoteImeBatchEdit or falls back to ADB shell input text.
     */
    suspend fun sendText(text: String, isAdbActive: Boolean, outStream: OutputStream?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (isAdbActive) {
                sendAdbText(text)
            } else {
                sendProtocolV2Text(text, outStream)
            }
        }
    }

    /**
     * Android TV Remote Service v2: RemoteImeBatchEdit payload.
     * Wire Tag: 0x5a (Field 11, Length-delimited)
     * Subfield 1: sequence_number (varint)
     * Subfield 2: text string edit (string, tag 0x12)
     */
    private fun sendProtocolV2Text(text: String, outStream: OutputStream?) {
        val out = outStream ?: throw IllegalStateException("Control stream disconnected")
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        
        // Build RemoteImeBatchEdit: [0x12, textBytes.length, ...textBytes]
        val imeEdit = byteArrayOf(0x12, textBytes.size.toByte()) + textBytes
        
        // Envelope RemoteMessage: Field 11 (0x5a)
        val remoteMsg = byteArrayOf(0x5a, imeEdit.size.toByte()) + imeEdit
        
        // Prefix with standard length-delimited wire varint
        out.write(byteArrayOf(remoteMsg.size.toByte()) + remoteMsg)
        out.flush()
    }

    /**
     * Fallback execution using ADB shell 'input text' with character escape handling
     */
    private fun sendAdbText(text: String) {
        // Replace spaces with %s for ADB shell text parser compatibility
        val sanitized = text.replace(" ", "%s")
            .replace("&", "\\&")
            .replace("<", "\\<")
            .replace(">", "\\>")
            .replace("(", "\\(")
            .replace(")", "\\)")
        AdbBridge.executeShellCommand("input text $sanitized")
    }

    /**
     * Resolves individual keyboard action events (Backspace, Enter/Search, Space)
     */
    suspend fun sendKeyAction(actionKey: KeypadAction, isAdbActive: Boolean, outStream: OutputStream?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val keyCode = when (actionKey) {
                KeypadAction.BACKSPACE -> KeyEvent.KEYCODE_DEL
                KeypadAction.SEARCH_ENTER -> KeyEvent.KEYCODE_ENTER
                KeypadAction.SPACE -> KeyEvent.KEYCODE_SPACE
            }

            if (isAdbActive) {
                AdbBridge.executeShellCommand("input keyevent $keyCode")
            } else {
                // Key press (direction = 1) then release (direction = 2) or short press (direction = 3)
                val inject = byteArrayOf(0x08, keyCode.toByte(), 0x10, 0x03)
                val packet = byteArrayOf(0x72, inject.size.toByte()) + inject
                val out = outStream ?: throw IllegalStateException("Control stream disconnected")
                out.write(byteArrayOf(packet.size.toByte()) + packet)
                out.flush()
            }
        }
    }
}

enum class KeypadAction {
    BACKSPACE,
    SEARCH_ENTER,
    SPACE
}
