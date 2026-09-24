package io.github.altenhofen.pen.settings

enum class SpaceAfterSuggestion {
    Off,
    On,
    Smart,
    ;

    fun stored(): String = name.lowercase()

    companion object {
        val Default = Off

        fun parse(stored: String?): SpaceAfterSuggestion =
            entries.find { it.name.equals(stored, ignoreCase = true) } ?: Default

        fun parseByte(value: Int?): SpaceAfterSuggestion = when (value) {
            0 -> Off
            1 -> On
            2 -> Smart
            else -> Default
        }

        fun toByte(mode: SpaceAfterSuggestion): Int = mode.ordinal
    }
}
