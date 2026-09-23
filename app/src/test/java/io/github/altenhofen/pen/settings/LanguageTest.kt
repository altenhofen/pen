package io.github.altenhofen.pen.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTest {
    private val mlKit = setOf("en-US", "pt-BR", "es-ES", "fr-FR", "de-DE", "it-IT", "ja")

    private fun resolve(
        choice: HandwritingLanguage = HandwritingLanguage.FollowApp,
        appLocale: String? = null,
        systemLocale: String? = null,
        supported: Set<String> = mlKit,
    ) = resolveInkLanguage(choice, appLocale, systemLocale, supported::contains)

    @Test
    fun explicitChoiceBeatsAppAndSystemLocales() {
        assertEquals(
            "de-DE",
            resolve(HandwritingLanguage.Explicit(InkLanguage.German), appLocale = "pt-BR", systemLocale = "en-US"),
        )
    }

    @Test
    fun appLocaleBeatsSystemLocale() {
        assertEquals("pt-BR", resolve(appLocale = "pt-BR", systemLocale = "en-US"))
    }

    @Test
    fun systemLocaleAnswersWhenTheAppHasNoOverride() {
        assertEquals("it-IT", resolve(appLocale = null, systemLocale = "it-IT"))
    }

    @Test
    fun bareLanguageWidensToTheCuratedTag() {
        assertEquals("pt-BR", resolve(appLocale = "pt"))
        assertEquals("en-US", resolve(appLocale = "en-CA"))
    }

    @Test
    fun bareTagWinsWhenMlKitShipsIt() {
        assertEquals("ja", resolve(appLocale = "ja-JP"))
    }

    @Test
    fun unsupportedLocalesFallBackToEnglish() {
        assertEquals("en-US", resolve(appLocale = "cy-GB", systemLocale = "mi-NZ"))
    }

    @Test
    fun unsupportedExplicitChoiceFallsThroughToTheAppLocale() {
        assertEquals(
            "pt-BR",
            resolve(
                HandwritingLanguage.Explicit(InkLanguage.French),
                appLocale = "pt-BR",
                supported = setOf("en-US", "pt-BR"),
            ),
        )
    }

    @Test
    fun followAppStoresAsNullAndParsesBack() {
        assertEquals(null, HandwritingLanguage.FollowApp.stored())
        assertEquals(HandwritingLanguage.FollowApp, HandwritingLanguage.parse(null))
        assertEquals(HandwritingLanguage.FollowApp, HandwritingLanguage.parse(""))
        assertEquals(HandwritingLanguage.FollowApp, HandwritingLanguage.parse("kl-GL"))
    }

    @Test
    fun explicitChoiceStoresAsItsTag() {
        assertEquals("fr-FR", HandwritingLanguage.Explicit(InkLanguage.French).stored())
        assertEquals(HandwritingLanguage.Explicit(InkLanguage.French), HandwritingLanguage.parse("fr-FR"))
    }

    @Test
    fun appLanguageMatchesARegionalTag() {
        assertEquals(AppLanguage.Portuguese, AppLanguage.ofTag("pt-BR"))
        assertEquals(AppLanguage.Portuguese, AppLanguage.ofTag("pt-PT"))
        assertEquals(AppLanguage.English, AppLanguage.ofTag("en-GB"))
        assertEquals(null, AppLanguage.ofTag(""))
        assertEquals(null, AppLanguage.ofTag("ja-JP"))
    }
}
