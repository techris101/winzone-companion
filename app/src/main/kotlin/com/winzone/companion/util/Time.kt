package com.winzone.companion.util

import java.util.Locale

object Time {
    fun formatClockSeconds(seconds: Int?): String {
        if (seconds == null || seconds < 0) return "--:--"
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)
    }

    fun parseClockString(rawText: String): Int? {
        val trimmed = rawText.trim()
        // Format MM:SS or M:SS
        val mmSsRegex = Regex("^(\\d{1,3}):(\\d{2})$")
        val mmSsMatch = mmSsRegex.find(trimmed)
        if (mmSsMatch != null) {
            val mins = mmSsMatch.groupValues[1].toIntOrNull() ?: return null
            val secs = mmSsMatch.groupValues[2].toIntOrNull() ?: return null
            return (mins * 60) + secs
        }

        // Format MM'SS or M'SS
        val apostropheRegex = Regex("^(\\d{1,3})['’](\\d{2})$")
        val apostropheMatch = apostropheRegex.find(trimmed)
        if (apostropheMatch != null) {
            val mins = apostropheMatch.groupValues[1].toIntOrNull() ?: return null
            val secs = apostropheMatch.groupValues[2].toIntOrNull() ?: return null
            return (mins * 60) + secs
        }

        // Format MM' (just minutes with tick)
        val minsOnlyRegex = Regex("^(\\d{1,3})['’]$")
        val minsOnlyMatch = minsOnlyRegex.find(trimmed)
        if (minsOnlyMatch != null) {
            val mins = minsOnlyMatch.groupValues[1].toIntOrNull() ?: return null
            return mins * 60
        }

        // Format just seconds or raw number
        val rawNumber = trimmed.toIntOrNull()
        if (rawNumber != null && rawNumber in 0..7200) {
            return rawNumber
        }

        return null
    }
}
