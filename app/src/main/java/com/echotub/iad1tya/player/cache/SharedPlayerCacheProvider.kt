package com.echotube.iad1tya.player.cache

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.echotube.iad1tya.player.config.PlayerConfig
import java.io.File

/**
 * Singleton that owns the single shared [SimpleCache] used by both the video player
 * ([PlayerCacheManager]) and the music player (DownloadUtil / Media3MusicService).
 *
 * Sharing the cache means:
 *  - Audio segments fetched while watching a video are reused when switching to audio-only.
 *  - The configurable cache size applies to both players at once.
 */
@UnstableApi
object SharedPlayerCacheProvider {

    private const val TAG = "SharedPlayerCache"

    @Volatile private var cache: SimpleCache? = null
    @Volatile private var standaloneDb: DatabaseProvider? = null
    @Volatile private var released: Boolean = false

    /**
     * Returns the shared [SimpleCache], creating it on first call or after an explicit release.
     *
     * All callers share the same instance regardless of which [databaseProvider] or
     * [maxCacheSizeBytes] they pass — only the first call's values are used.
     */
    @Synchronized
    fun getOrCreate(
        context: Context,
        databaseProvider: DatabaseProvider? = null,
        maxCacheSizeBytes: Long = PlayerConfig.CACHE_SIZE_BYTES
    ): SimpleCache {
        // If cache exists and was NOT released, return it immediately
        val existing = cache
        if (existing != null && !released) return existing

        // Either first call or the cache was explicitly released — create a fresh one
        released = false
        val db = databaseProvider ?: run {
            standaloneDb = StandaloneDatabaseProvider(context.applicationContext)
            standaloneDb!!
        }
        val cacheDir = File(context.applicationContext.cacheDir, PlayerConfig.CACHE_DIR_NAME)
        val evictor = if (maxCacheSizeBytes <= 0) {
            NoOpCacheEvictor()
        } else {
            LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes)
        }
        Log.d(TAG, "Creating shared SimpleCache: dir=$cacheDir, maxBytes=$maxCacheSizeBytes")
        return SimpleCache(cacheDir, evictor, db).also { cache = it }
    }

    /** Release the cache (call only on full app shutdown / tests). */
    @Synchronized
    fun release() {
        cache?.release()
        cache = null
        standaloneDb = null
        released = true
        Log.d(TAG, "Shared SimpleCache released")
    }
}
