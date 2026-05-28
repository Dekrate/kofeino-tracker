package pl.dekrate.kofeino.tracker.di

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.dekrate.kofeino.tracker.data.sync.adb.AdbCapabilityClient
import pl.dekrate.kofeino.tracker.data.sync.adb.AdbDataClient
import pl.dekrate.kofeino.tracker.data.sync.adb.AdbMessageClient
import javax.inject.Singleton

/**
 * Debug Hilt module replacing [WearableModule] in debug builds.
 * Provides ADB TCP implementations of Wearable Data Layer clients
 * for cross-device sync testing without Wear OS app pairing.
 */
@Module
@InstallIn(SingletonComponent::class)
object AdbWearableModule {

    @Provides @Singleton
    fun provideMessageClient(client: AdbMessageClient): MessageClient = client

    @Provides @Singleton
    fun provideCapabilityClient(client: AdbCapabilityClient): CapabilityClient = client

    @Provides @Singleton
    fun provideDataClient(client: AdbDataClient): DataClient = client

}
