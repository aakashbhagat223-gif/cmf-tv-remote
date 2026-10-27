package com.cmf.tvremote.domain.macro

import com.cmf.tvremote.domain.model.RemoteCommand
import kotlinx.coroutines.delay

data class MacroStep(
    val command: RemoteCommand? = null,
    val delayMs: Long = 0,
    val textInput: String? = null
)

object MacroRepository {
    // Exact preserved command sequences from active UI
    val STREMIO_MACRO = "HOME WAIT 500 DOWN DOWN DOWN RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT WAIT 500 OK"
    val HDMI_2_MACRO = "HOME WAIT 500 RIGHT RIGHT RIGHT RIGHT RIGHT RIGHT WAIT 500 OK WAIT 500 DOWN OK WAIT 500 DOWN DOWN DOWN DOWN DOWN WAIT 500 OK"
    val BACKLIGHT_OPEN_MACRO = "QUICK SETTINGS WAIT 200 DOWN DOWN LEFT WAIT 200 OK WAIT 200 DOWN WAIT 200 OK"
    val BACKLIGHT_CLOSE_MACRO = "BACK BACK BACK"

    fun parseMacroSequence(raw: String): List<MacroStep> {
        val tokens = raw.trim().split("\\s+".toRegex())
        val steps = mutableListOf<MacroStep>()
        var idx = 0

        while (idx < tokens.size) {
            when (val token = tokens[idx]) {
                "WAIT" -> {
                    val duration = tokens.getOrNull(idx + 1)?.toLongOrNull() ?: 200L
                    steps.add(MacroStep(delayMs = duration))
                    idx += 2
                }
                "HOME" -> { steps.add(MacroStep(command = RemoteCommand.HOME)); idx++ }
                "OK" -> { steps.add(MacroStep(command = RemoteCommand.SELECT)); idx++ }
                "BACK" -> { steps.add(MacroStep(command = RemoteCommand.BACK)); idx++ }
                "DOWN" -> { steps.add(MacroStep(command = RemoteCommand.DPAD_DOWN)); idx++ }
                "RIGHT" -> { steps.add(MacroStep(command = RemoteCommand.DPAD_RIGHT)); idx++ }
                "LEFT" -> { steps.add(MacroStep(command = RemoteCommand.DPAD_LEFT)); idx++ }
                "UP" -> { steps.add(MacroStep(command = RemoteCommand.DPAD_UP)); idx++ }
                "QUICK", "SETTINGS" -> {
                    // Normalize token variations
                    if (token == "QUICK" && tokens.getOrNull(idx + 1) == "SETTINGS") {
                        steps.add(MacroStep(command = RemoteCommand.SETTINGS))
                        idx += 2
                    } else {
                        steps.add(MacroStep(command = RemoteCommand.SETTINGS))
                        idx++
                    }
                }
                else -> idx++
            }
        }
        return steps
    }
}
