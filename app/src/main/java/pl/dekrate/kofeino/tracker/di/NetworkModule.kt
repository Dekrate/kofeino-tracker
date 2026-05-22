package pl.dekrate.kofeino.tracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.dekrate.kofeino.tracker.data.remote.OpenFoodFactsApi
import pl.dekrate.kofeino.tracker.data.remote.OpenFoodFactsConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 15L
    private const val CACHE_SIZE_BYTES = 20L * 1024 * 1024

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val cacheDir = File(context.cacheDir, "okhttp_cache")
        val cache = Cache(cacheDir, CACHE_SIZE_BYTES)
        Timber.d("OkHttp cache at: %s (%d MB)", cacheDir.absolutePath, CACHE_SIZE_BYTES / 1024 / 1024)

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .cache(cache)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(okHttpClient: OkHttpClient): OpenFoodFactsApi {
        return Retrofit.Builder()
            .baseUrl(OpenFoodFactsConfig.ROOT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
