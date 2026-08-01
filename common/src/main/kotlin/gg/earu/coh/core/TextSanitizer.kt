package gg.earu.coh.core

object TextSanitizer {
    /** Same opt-out prefixes as the GMod original (rtchat). */
    private val HIDE_PREFIXES = listOf("-- hide", "// hide", "# hide")

    fun isHidden(text: String): Boolean = HIDE_PREFIXES.any { text.startsWith(it) }

    fun isCommand(text: String): Boolean = text.startsWith("/")

    /** True when the text must never be shown to others (checked on both sides). */
    fun shouldConceal(text: String): Boolean = isHidden(text) || isCommand(text)

    /** Strips § formatting pairs and control characters, clamps length. */
    fun sanitize(raw: String, maxChars: Int): String {
        val sb = StringBuilder(minOf(raw.length, maxChars))
        var skipNext = false
        for (ch in raw) {
            if (skipNext) {
                skipNext = false
                continue
            }
            if (ch == '§') {
                skipNext = true
                continue
            }
            if (ch.code < 0x20) continue
            sb.append(ch)
            if (sb.length >= maxChars) break
        }
        return sb.toString()
    }
}
