package io.github.altenhofen.pen.settings

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The per-app locale, which Android Settings edits under Apps > pen > Language and the in-app
 * picker edits here. Both sides read and write the same storage, so they stay in step.
 */
object AppLocales {
    /** The language the user picked for the app, or null when the app follows the device. */
    fun current(): AppLanguage? = AppLanguage.ofTag(overrideTag())

    fun choose(language: AppLanguage?) {
        AppCompatDelegate.setApplicationLocales(
            if (language == null) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.tag)
            },
        )
    }

    /** The override as a BCP-47 tag, or null when there is none. */
    fun overrideTag(): String? = AppCompatDelegate.getApplicationLocales()
        .toLanguageTags()
        .takeIf { it.isNotEmpty() }
        ?.substringBefore(',')

    /** The device locale, which the per-app override leaves alone. */
    fun systemTag(): String = Resources.getSystem().configuration.locales[0].toLanguageTag()
}
