package com.splitmate.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * Singleton Application class configuring Coil's ImageLoader with SvgDecoder.Factory()
 * and indefinite DiskCache so DiceBear Open-Peeps SVGs (`https://api.dicebear.com/9.x/open-peeps/svg?seed=...`)
 * decode natively in Jetpack Compose and persist offline indefinitely.
 */
class SplitMateApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("dicebear_svg_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L) // 50 MB persistent offline SVG cache
                    .build()
            }
            .respectCacheHeaders(false) // Persist cached SVGs indefinitely offline
            .crossfade(true)
            .build()
    }
}
