package pl.dekrate.kofeino.tracker.util

import android.content.Context
import android.content.res.Configuration
import pl.dekrate.kofeino.tracker.KofeinoTrackerApplication
import java.util.Locale

/**
 * Utility for applying saved language preference to a Context.
 */
object LocaleHelper {

    /** Wraps [context] with the saved language locale. */
    fun applyLocale(context: Context): Context {
        val lang = KofeinoTrackerApplication.getLanguage(context)
        return setLocale(context, lang)
    }

    /** Wraps [context] with a specific language code (e.g. "en", "pl"). */
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale.of(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
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
