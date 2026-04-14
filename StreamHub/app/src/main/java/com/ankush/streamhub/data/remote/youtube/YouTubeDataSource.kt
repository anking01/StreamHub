package com.ankush.streamhub.data.remote.youtube

import android.util.Log
import com.ankush.streamhub.BuildConfig
import com.ankush.streamhub.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

private const val TAG = "YouTubeDataSource"
private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

// ─────────────────────────────────────────────────────────────────────────────
// URL scheme used in FeedConfig.url for YouTube API feeds:
//   yt://trending            → most popular videos (all categories)
//   yt://trending/gaming     → most popular gaming videos
//   yt://trending/music      → most popular music videos
//   yt://trending/news       → most popular news videos
//   yt://trending/tech       → most popular science & tech videos
//   yt://trending/sports     → most popular sports videos
//   yt://trending/film       → most popular film & animation
//   yt://trending/comedy     → most popular comedy
//   yt://live                → currently live streams
//   yt://channel/CHANNEL_ID  → latest from a specific channel
// ─────────────────────────────────────────────────────────────────────────────

class YouTubeDataSource {

    private val apiKey = BuildConfig.YOUTUBE_API_KEY

    // Dedicated HTTP client for YouTube — no RSS headers, standard browser UA
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()
            chain.proceed(req)
        }
        .build()

    private val service: YouTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApiService::class.java)
    }

    val isConfigured: Boolean get() = apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY_HERE"

    suspend fun fetch(config: FeedConfig): List<ContentItem> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.e(TAG, "❌ YouTube API key not set! Add YOUTUBE_API_KEY to local.properties")
            return@withContext emptyList()
        }

        Log.d(TAG, "⏳ Fetching: ${config.name} (${config.url})")
        try {
            val items = when {
                config.url == "yt://trending" ->
                    fetchTrending(config, categoryId = null)

                config.url.startsWith("yt://trending/") -> {
                    val sub = config.url.removePrefix("yt://trending/")
                    fetchTrending(config, categoryId = categoryIdFor(sub).ifEmpty { null })
                }

                config.url == "yt://live" ->
                    fetchLive(config)

                config.url.startsWith("yt://channel/") -> {
                    val channelId = config.url.removePrefix("yt://channel/")
                    fetchChannel(config, channelId)
                }

                else -> emptyList()
            }
            Log.d(TAG, "✅ ${config.name}: got ${items.size} items")
            items
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e(TAG, "❌ HTTP ${e.code()} for ${config.name}: $errorBody")
            val userMsg = when (e.code()) {
                400 -> "YouTube API key invalid or restricted (${e.code()})"
                403 -> "YouTube API quota exceeded or key restricted (${e.code()})"
                else -> "YouTube API error: ${e.code()}"
            }
            throw Exception(userMsg)
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetch failed for ${config.name}: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    // ── Private fetchers ──────────────────────────────────────

    private suspend fun fetchTrending(config: FeedConfig, categoryId: String?): List<ContentItem> {
        val response = service.getTrendingVideos(categoryId = categoryId, apiKey = apiKey)
        return response.items.map { it.toContentItem(config) }
    }

    private suspend fun fetchLive(config: FeedConfig): List<ContentItem> {
        val response = service.getLiveStreams(apiKey = apiKey)
        return response.items.mapNotNull { it.toContentItem(config) }
    }

    private suspend fun fetchChannel(config: FeedConfig, channelId: String): List<ContentItem> {
        val response = service.getChannelVideos(channelId = channelId, apiKey = apiKey)
        return response.items.mapNotNull { it.toContentItem(config) }
    }

    // ── Mapping helpers ───────────────────────────────────────

    private fun YouTubeVideoItem.toContentItem(config: FeedConfig) = ContentItem(
        id           = "yt_$id",
        title        = snippet.title,
        description  = snippet.description.take(300),
        thumbnailUrl = snippet.thumbnails.best(),
        contentUrl   = "https://www.youtube.com/watch?v=$id",
        sourceUrl    = "https://www.youtube.com/watch?v=$id",
        sourceName   = snippet.channelTitle,
        source       = FeedSource.YOUTUBE,
        category     = config.category,
        type         = if (snippet.liveBroadcastContent == "live") ContentType.LIVE else ContentType.VIDEO,
        publishedAt  = parseYtDate(snippet.publishedAt),
        duration     = parseIsoDuration(contentDetails?.duration),
        viewCount    = statistics?.viewCount?.toLongOrNull(),
        isLive       = snippet.liveBroadcastContent == "live",
        author       = snippet.channelTitle
    )

    private fun YouTubeSearchItem.toContentItem(config: FeedConfig): ContentItem? {
        val videoId = id.videoId?.takeIf { it.isNotBlank() } ?: return null
        return ContentItem(
            id           = "yt_$videoId",
            title        = snippet.title,
            description  = snippet.description.take(300),
            thumbnailUrl = snippet.thumbnails.best()
                .ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
            contentUrl   = "https://www.youtube.com/watch?v=$videoId",
            sourceUrl    = "https://www.youtube.com/watch?v=$videoId",
            sourceName   = snippet.channelTitle,
            source       = FeedSource.YOUTUBE,
            category     = config.category,
            type         = if (snippet.liveBroadcastContent == "live") ContentType.LIVE else ContentType.VIDEO,
            publishedAt  = parseYtDate(snippet.publishedAt),
            isLive       = snippet.liveBroadcastContent == "live",
            author       = snippet.channelTitle
        )
    }

    // ── Utility ───────────────────────────────────────────────

    private val ytDateFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseYtDate(raw: String): Long = try {
        ytDateFmt.parse(raw)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }

    // Parses ISO 8601 duration: PT1H23M45S → seconds
    private val isoDurationPattern = Pattern.compile(
        "PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?"
    )

    private fun parseIsoDuration(iso: String?): Int? {
        if (iso.isNullOrBlank()) return null
        val m = isoDurationPattern.matcher(iso)
        if (!m.find()) return null
        val h = m.group(1)?.toIntOrNull() ?: 0
        val min = m.group(2)?.toIntOrNull() ?: 0
        val s = m.group(3)?.toIntOrNull() ?: 0
        val total = h * 3600 + min * 60 + s
        return if (total > 0) total else null
    }

    // Maps subcategory name → YouTube video category ID
    private fun categoryIdFor(name: String) = when (name) {
        "film"    -> "1"
        "music"   -> "10"
        "sports"  -> "17"
        "gaming"  -> "20"
        "comedy"  -> "23"
        "news"    -> "25"
        "tech"    -> "28"
        else      -> ""
    }
}
