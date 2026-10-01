package com.winzone.companion.util

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class RingBufferTree(private val capacity: Int = 500) : Timber.Tree() {
    private val buffer = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val priorityChar = when (priority) {
            2 -> 'V'
            3 -> 'D'
            4 -> 'I'
            5 -> 'W'
            6 -> 'E'
            7 -> 'A'
            else -> '?'
        }
        val timestamp = dateFormat.format(Date())
        val sanitizedTag = tag ?: "WinZone"
        val sanitizedMessage = sanitize(message)
        val entry = "[$timestamp] $priorityChar/$sanitizedTag: $sanitizedMessage" +
                (t?.let { "\n" + it.stackTraceToString() } ?: "")

        buffer.add(entry)
        while (buffer.size > capacity) {
            buffer.poll()
        }
    }

    fun getLogs(): List<String> = buffer.toList()

    fun clear() {
        buffer.clear()
    }

    private fun sanitize(message: String): String {
        // Mask emails: u***@d***
        val emailRegex = Regex("([a-zA-Z0-9_.+-])[a-zA-Z0-9_.+-]*@([a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)")
        var result = emailRegex.replace(message) { match ->
            val first = match.groupValues[1]
            val domain = match.groupValues[2]
            "$first***@$domain"
        }
        // Mask jwt / Bearer tokens
        result = result.replace(Regex("Bearer\\s+([A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+)"), "Bearer [MASKED]")
        return result
    }
}
