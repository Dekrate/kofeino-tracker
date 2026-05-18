package pl.dekrate.kofeino.tracker.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Language preferences DI module (retained for Hilt binding scope).
 *
 * [DataStorePreferences] is automatically provided by Hilt; this module
 * exists only as an extension point and documentation anchor.
 *
 * The old [pl.dekrate.kofeino.tracker.data.local.LanguagePreferences] (SharedPreferences)
 * is retained only for pre-Hilt static access (attachBaseContext).
 */
@Module
@InstallIn(SingletonComponent::class)
object LanguageModule
