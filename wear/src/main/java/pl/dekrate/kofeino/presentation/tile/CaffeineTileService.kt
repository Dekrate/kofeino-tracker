package pl.dekrate.kofeino.presentation.tile

import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.common.domain.model.ColorScheme
import pl.dekrate.kofeino.common.domain.model.DisplayOption
import pl.dekrate.kofeino.common.domain.model.TileConfig
import pl.dekrate.kofeino.data.local.TileDataStorePreferences
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * A Wear OS Tile that displays the user's daily caffeine intake.
 *
 * Uses the ProtoLayout Material3 API for rendering. The Tile reads its display
 * configuration from [TileDataStorePreferences], which is synced from the phone
 * companion app via the Wearable Data Layer. When no phone config is available
 * (standalone mode), the tile falls back to default [TileConfig] values.
 *
 * ## Lifecycle
 * - [onTileRequest] is called by the system to get the current tile state.
 * - [onTileResourcesRequest] is called to provide shared resources (if any).
 *
 * ## Design
 * - **Standalone-safe**: Reads from local DataStore only.
 * - **Reactive**: Configuration changes apply on next tile refresh (no polling).
 */
class CaffeineTileService : TileService() {

    private lateinit var tileDataStorePreferences: TileDataStorePreferences

    override fun onCreate() {
        super.onCreate()
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            TileServiceHiltEntryPoint::class.java
        )
        tileDataStorePreferences = hiltEntryPoint.tileDataStorePreferences()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        return GlobalScope.future {
            val config = tileDataStorePreferences.getTileConfig()

            val caffeineTotalStr = getString(R.string.tile_caffeine_total)
            val drinkCountStr = getString(R.string.tile_drink_count)
            val limitStatusStr = getString(R.string.tile_limit_status)
            val combinedStr = getString(R.string.tile_combined)
            val titleCaffeineTotal = getString(R.string.tile_title_caffeine_total)
            val titleDrinkCount = getString(R.string.tile_title_drink_count)
            val titleLimitStatus = getString(R.string.tile_title_limit_status)
            val titleCombined = getString(R.string.tile_title_combined)

            val colors = CustomTileColors.fromColorScheme(config.colorScheme, config.caffeineLimitColor)

            val tileLayout = materialScope(this@CaffeineTileService, requestParams.deviceConfiguration) {
                primaryLayout(
                    titleSlot = {
                        text(
                            text = when (config.displayOption) {
                                DisplayOption.CAFFEINE_TOTAL -> titleCaffeineTotal
                                DisplayOption.DRINK_COUNT -> titleDrinkCount
                                DisplayOption.LIMIT_STATUS -> titleLimitStatus
                                DisplayOption.BOTH -> titleCombined
                            }.layoutString,
                            color = LayoutColor(colors.accentColor)
                        )
                    },
                    mainSlot = {
                        text(
                            text = when (config.displayOption) {
                                DisplayOption.CAFFEINE_TOTAL -> caffeineTotalStr
                                DisplayOption.DRINK_COUNT -> drinkCountStr
                                DisplayOption.LIMIT_STATUS -> limitStatusStr
                                DisplayOption.BOTH -> combinedStr
                            }.layoutString,
                            color = LayoutColor(colors.primaryColor),
                            typography = Typography.BODY_LARGE
                        )
                    }
                )
            }

            Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(config.refreshIntervalMinutes.minutes * 60_000L)
                .setTileTimeline(Timeline.fromLayoutElement(tileLayout))
                .build()
        }.asListenableFuture()
    }

    /**
     * Converts a [CompletableFuture] to a [ListenableFuture].
     *
     * The listener registered via [addListener] is invoked when the future completes
     * (normally, exceptionally, or by cancellation).
     */
    private fun <T> CompletableFuture<T>.asListenableFuture(): ListenableFuture<T> {
        return object : ListenableFuture<T> {
            override fun get(): T = this@asListenableFuture.get()
            override fun get(timeout: Long, unit: TimeUnit): T = this@asListenableFuture.get(timeout, unit)
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean =
                this@asListenableFuture.cancel(mayInterruptIfRunning)

            override fun isCancelled(): Boolean = this@asListenableFuture.isCancelled()
            override fun isDone(): Boolean = this@asListenableFuture.isDone()
            override fun addListener(listener: Runnable, executor: Executor) {
                this@asListenableFuture.whenCompleteAsync({ _, _ -> listener.run() }, executor)
            }
        }
    }
}

/**
 * Colour schemes for the Caffeine Tile.
 * These are simple colour values used in the tile layout.
 */
data class CustomTileColors(
    val primaryColor: Int,
    val backgroundColor: Int,
    val accentColor: Int
) {
    companion object {
        val DEFAULT = CustomTileColors(
            primaryColor = 0xFF6D4C41.toInt(),    // Brown (espresso)
            backgroundColor = 0xFF1A1A1A.toInt(),  // Near black
            accentColor = 0xFF8D6E63.toInt()       // Light brown
        )
        val MONOCHROME = CustomTileColors(
            primaryColor = 0xFFFFFFFF.toInt(),     // White
            backgroundColor = 0xFF000000.toInt(),   // Black
            accentColor = 0xFF888888.toInt()        // Gray
        )
        val AMBER = CustomTileColors(
            primaryColor = 0xFFFFC107.toInt(),     // Amber
            backgroundColor = 0xFF1A1A1A.toInt(),  // Near black
            accentColor = 0xFFFFA000.toInt()        // Dark amber
        )
        val GREEN = CustomTileColors(
            primaryColor = 0xFF4CAF50.toInt(),     // Green
            backgroundColor = 0xFF1A1A1A.toInt(),  // Near black
            accentColor = 0xFF66BB6A.toInt()        // Light green
        )

        fun fromColorScheme(
            scheme: ColorScheme,
            limitColor: Boolean = false
        ): CustomTileColors {
            val base = when (scheme) {
                ColorScheme.MONOCHROME -> MONOCHROME
                ColorScheme.AMBER -> AMBER
                ColorScheme.GREEN -> GREEN
                ColorScheme.DEFAULT -> DEFAULT
            }
            return if (limitColor) {
                base.copy(primaryColor = 0xFFE53935.toInt()) // Red tint for limit exceeded
            } else {
                base
            }
        }
    }
}
