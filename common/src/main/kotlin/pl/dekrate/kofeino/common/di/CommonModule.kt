package pl.dekrate.kofeino.common.di

import dagger.Module

/**
 * Shared Dagger module for common repository interfaces and utilities.
 *
 * This module has no @InstallIn because :common is pure JVM (no Android/Hilt).
 * It must be included by @InstallIn-annotated modules in :app and :wear.
 *
 * Example:
 * ```
 * @Module(includes = [CommonModule::class])
 * @InstallIn(SingletonComponent::class)
 * ```
 *
 * Repository @Binds are defined in app/wear-specific RepositoryModules
 * because those bindings reference implementation classes that live
 * in the respective app/wear modules.
 */
@Module
abstract class CommonModule
