package pl.dekrate.kofeino.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.dekrate.kofeino.data.local.CaffeinePreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CaffeineModule {

    @Provides
    @Singleton
    fun provideCaffeinePreferences(
        @ApplicationContext context: Context
    ): CaffeinePreferences {
        return CaffeinePreferences(context)
    }
}
