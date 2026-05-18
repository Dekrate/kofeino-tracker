package pl.dekrate.kofeino.tracker.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Preferences DI module.
 *
 * [DataStorePreferences] is automatically provided by Hilt via its `@Inject constructor`
 * and `@Singleton` annotation — no `@Provides` method needed here.
 *
 * This module exists as a documentation anchor and extension point for any
 * future preference-related bindings.
 *
 * The old [pl.dekrate.kofeino.tracker.data.local.ThemePreferences] (SharedPreferences)
 * is retained only for pre-Hilt static access (attachBaseContext).
 */
@Module
@InstallIn(SingletonComponent::class)
object ThemeModule
