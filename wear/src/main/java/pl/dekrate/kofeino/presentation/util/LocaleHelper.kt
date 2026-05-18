package pl.dekrate.kofeino.presentation.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import pl.dekrate.kofeino.data.local.LanguagePreferences
import java.util.Locale

/**
 * Utility for applying saved language preference to a Context.
 *
 * Uses the **Decorator pattern** via [Context.createConfigurationContext] to wrap a
 * [Context] with a locale-aware configuration. This is the modern replacement for
 * the deprecated [Configuration.setLocale] / [android.content.res.Resources.updateConfiguration]
 * APIs.
 */
object LocaleHelper {

    /** Wraps [context] with the saved language locale. */
    fun applyLocale(context: Context): Context {
        val lang = LanguagePreferences.getLanguage(context)
        return wrapContext(context, lang)
    }

    /**
     * Wraps [context] with a specific language code.
     *
     * @param language BCP-47 language tag (e.g. `"en"`, `"pl"`) or empty string
     *                 for system default.
     */
    fun wrapContext(context: Context, language: String): Context {
        if (language.isEmpty()) return context  // No forced locale → system default
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(config)
    }

    /** Gets the current effective language code from a context. */
    fun getCurrentLanguage(context: Context): String {
        val config = context.resources.configuration
        return if (config.locales.size() > 0) {
            config.locales[0].language
        } else {
            Locale.getDefault().language
        }
    }
}
