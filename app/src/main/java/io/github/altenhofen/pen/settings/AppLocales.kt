package io.github.altenhofen.pen.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

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

    /**
     * The locale the app is running in. Android 13 applies the override to every context in the
     * process, so the context answers even where appcompat holds no stored override of its own.
     */
    fun effectiveTag(context: Context): String = overrideTag() ?: context.resources.firstLocaleTag()

    /** The device locale, which the per-app override leaves alone. */
    fun systemTag(): String = Resources.getSystem().firstLocaleTag()

    private fun Resources.firstLocaleTag(): String = configuration.firstLocaleTag()

    private fun Configuration.firstLocaleTag(): String =
        locales.takeIf { !it.isEmpty }?.get(0)?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()
}
