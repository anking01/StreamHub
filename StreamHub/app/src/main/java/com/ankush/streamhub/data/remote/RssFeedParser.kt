package com.ankush.streamhub.data.remote

import android.util.Log
import android.util.Xml
import com.ankush.streamhub.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "RssFeedParser"

// ─────────────────────────────────────────────────────────────────────────────
// HTTP Client (shared singleton)
// ─────────────────────────────────────────────────────────────────────────────

object HttpClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "StreamHub/1.0 (Android RSS Reader)")
                .header("Accept", "application/rss+xml,application/xml,text/xml,*/*")
                .build()
            chain.proceed(request)
        }
        .build()
}

// ─────────────────────────────────────────────────────────────────────────────
// RSS / Atom parser using XmlPullParser (no external libraries needed)
// ─────────────────────────────────────────────────────────────────────────────

class RssFeedParser {

    suspend fun parse(feedConfig: FeedConfig): List<ContentItem> = withContext(Dispatchers.IO) {
        try {
            val xml = fetchXml(feedConfig.url) ?: return@withContext emptyList()
            val items = parseXml(xml, feedConfig)
            Log.d(TAG, "Parsed ${items.size} items from ${feedConfig.name}")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse feed ${feedConfig.name}: ${e.message}")
            emptyList()
        }
    }

    private fun fetchXml(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            HttpClient.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error for $url: ${e.message}")
            null
        }
    }

    private fun sanitizeXml(xml: String): String {
        // Replace bare & that aren't valid XML entities to prevent parse failures
        return xml.replace(Regex("&(?!(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)"), "&amp;")
    }

    private fun parseXml(xml: String, feedConfig: FeedConfig): List<ContentItem> {
        val items = mutableListOf<ContentItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(sanitizeXml(xml).reader())

        var inItem = false
        var inEntry = false  // Atom feeds use <entry> instead of <item>
        var title = ""
        var link = ""
        var description = ""
        var pubDate = ""
        var thumbnail = ""
        var author = ""
        var duration = ""
        var currentTag = ""

        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    when (currentTag) {
                        "item", "entry" -> {
                            inItem = true
                            title = ""; link = ""; description = ""
                            pubDate = ""; thumbnail = ""; author = ""
                        }
                        "enclosure", "media:content", "media:thumbnail" -> {
                            if (inItem) {
                                val urlAttr = parser.getAttributeValue(null, "url")
                                    ?: parser.getAttributeValue(null, "href")
                                if (!urlAttr.isNullOrBlank() && thumbnail.isEmpty()) {
                                    thumbnail = urlAttr
                                }
                            }
                        }
                        "link" -> {
                            if (inItem) {
                                // Atom uses <link href="..." /> attribute
                                val href = parser.getAttributeValue(null, "href")
                                if (!href.isNullOrBlank()) link = href
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (inItem && text.isNotEmpty()) {
                        when (currentTag) {
                            "title"              -> title = title.ifEmpty { text }
                            "link"               -> link = link.ifEmpty { text }
                            "description", "summary", "content:encoded" ->
                                description = description.ifEmpty { stripHtml(text).take(300) }
                            "pubDate", "published", "updated", "dc:date" ->
                                pubDate = pubDate.ifEmpty { text }
                            "author", "dc:creator", "itunes:author" ->
                                author = author.ifEmpty { text }
                            "itunes:duration"    -> duration = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" || parser.name == "entry") {
                        if (inItem && title.isNotEmpty()) {
                            val id = "${feedConfig.id}_${(link + title).hashCode()}"
                            // For YouTube channel RSS feeds, construct thumbnail from video ID
                            if (thumbnail.isEmpty() && feedConfig.source == FeedSource.YOUTUBE) {
                                val videoId = link.substringAfter("watch?v=", "")
                                    .substringBefore("&").trim()
                                if (videoId.isNotEmpty()) {
                                    thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                }
                            }
                            items.add(
                                ContentItem(
                                    id          = id,
                                    title       = title.trim(),
                                    description = description.trim(),
                                    thumbnailUrl = thumbnail,
                                    contentUrl  = link.trim(),
                                    sourceUrl   = link.trim(),
                                    sourceName  = feedConfig.name,
                                    source      = feedConfig.source,
                                    category    = feedConfig.category,
                                    type        = when (feedConfig.source) {
                                        FeedSource.PODCAST -> ContentType.AUDIO
                                        FeedSource.YOUTUBE -> ContentType.VIDEO
                                        else               -> ContentType.ARTICLE
                                    },
                                    publishedAt = parseDate(pubDate),
                                    duration    = parseDuration(duration),
                                    author      = author.ifEmpty { null }
                                )
                            )
                        }
                        inItem = false
                        title = ""; link = ""; description = ""
                        pubDate = ""; thumbnail = ""; author = ""; duration = ""
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return items.take(30)  // max 30 items per feed
    }

    // ── Helpers ───────────────────────────────────────────────

    private val DATE_FORMATS = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss"
    )

    private fun parseDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        for (fmt in DATE_FORMATS) {
            try {
                return SimpleDateFormat(fmt, Locale.ENGLISH)
                    .parse(raw)?.time ?: continue
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }

    private fun parseDuration(raw: String): Int? {
        if (raw.isBlank()) return null
        return try {
            val parts = raw.split(":").map { it.trim().toInt() }
            when (parts.size) {
                3    -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2    -> parts[0] * 60 + parts[1]
                1    -> parts[0]
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
