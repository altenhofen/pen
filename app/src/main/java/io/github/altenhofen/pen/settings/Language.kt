package io.github.altenhofen.pen.settings

/** A UI language the app ships strings for and lists in `res/xml/locales_config.xml`. */
enum class AppLanguage(val tag: String) {
    English("en"),
    Portuguese("pt-BR"),
    Spanish("es"),
    ;

    companion object {
        fun ofTag(tag: String?): AppLanguage? {
            if (tag.isNullOrBlank()) return null
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                ?: entries.firstOrNull { it.tag.primaryLanguage() == tag.primaryLanguage() }
        }
    }
}

/** A handwriting language the picker offers, named by the ML Kit Digital Ink model tag it selects. */
enum class InkLanguage(val tag: String) {
    English("en-US"),
    Portuguese("pt-BR"),
    Spanish("es-ES"),
    French("fr-FR"),
    German("de-DE"),
    Italian("it-IT"),
    ;

    companion object {
        fun ofTag(tag: String?): InkLanguage? = entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
    }
}

/** What handwriting recognition runs in. */
sealed interface HandwritingLanguage {
    data object FollowApp : HandwritingLanguage

    data class Explicit(val language: InkLanguage) : HandwritingLanguage

    /** The persisted form. Null means [FollowApp], so a profile written before this setting existed reads back as [FollowApp]. */
    fun stored(): String? = (this as? Explicit)?.language?.tag

    companion object {
        fun parse(stored: String?): HandwritingLanguage =
            InkLanguage.ofTag(stored)?.let(::Explicit) ?: FollowApp
    }
}

const val INK_FALLBACK_TAG = "en-US"

/**
 * The ML Kit tag to recognize with. Tries the explicit choice, then the app locale, then the system
 * locale, widening each candidate to its bare language and then to a curated [InkLanguage] before
 * moving to the next one.
 */
fun resolveInkLanguage(
    choice: HandwritingLanguage,
    appLocale: String?,
    systemLocale: String?,
    supports: (String) -> Boolean,
): String {
    val candidates = buildList {
        if (choice is HandwritingLanguage.Explicit) add(choice.language.tag)
        appLocale?.takeIf { it.isNotBlank() }?.let(::add)
        systemLocale?.takeIf { it.isNotBlank() }?.let(::add)
    }
    return candidates.firstNotNullOfOrNull { locale -> widen(locale).firstOrNull(supports) } ?: INK_FALLBACK_TAG
}

private fun widen(locale: String): List<String> {
    val bare = locale.primaryLanguage()
    val curated = InkLanguage.entries.firstOrNull { it.tag.primaryLanguage() == bare }?.tag
    return listOfNotNull(locale, bare, curated).distinct()
}

private fun String.primaryLanguage(): String = substringBefore('-').substringBefore('_').lowercase()
