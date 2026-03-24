package com.streamhub.app.data.model

import java.io.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Core domain model — used everywhere in the app
// ─────────────────────────────────────────────────────────────────────────────

data class ContentItem(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val contentUrl: String,         // URL loaded in WebView
    val sourceUrl: String,          // Original article/video link
    val sourceName: String,
    val source: FeedSource,
    val category: Category,
    val type: ContentType,
    val publishedAt: Long,          // Unix timestamp millis
    val duration: Int? = null,      // seconds
    val viewCount: Long? = null,
    val isLive: Boolean = false,
    val author: String? = null,
    val tags: List<String> = emptyList(),
    var isBookmarked: Boolean = false
) : Serializable

enum class FeedSource(val label: String, val color: String) {
    YOUTUBE("YouTube", "#FF0000"),
    TWITCH("Twitch", "#9147FF"),
    RSS("RSS", "#FF9500"),
    PODCAST("Podcast", "#872EC4"),
    NEWS("News", "#007AFF"),
    CUSTOM("Custom", "#6C63FF")
}

enum class ContentType(val label: String) {
    VIDEO("Video"),
    LIVE("Live"),
    ARTICLE("Article"),
    AUDIO("Podcast")
}

enum class Category(val label: String, val emoji: String) {
    ALL("All", "🌐"),
    VIDEOS("Videos", "▶️"),
    LIVE("Live", "🔴"),
    NEWS("News", "📰"),
    PODCASTS("Podcasts", "🎙️"),
    GAMING("Gaming", "🎮"),
    TECH("Tech", "💻"),
    SPORTS("Sports", "⚽"),
    MUSIC("Music", "🎵")
}

// ─────────────────────────────────────────────────────────────────────────────
// Feed source configuration model
// ─────────────────────────────────────────────────────────────────────────────

data class FeedConfig(
    val id: String,
    val name: String,
    val url: String,
    val source: FeedSource,
    val category: Category,
    val isActive: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// Default feeds seeded into the app
// ─────────────────────────────────────────────────────────────────────────────

object DefaultFeeds {
    val list = listOf(
        FeedConfig("yt-trending",  "YouTube Trending",  "https://www.youtube.com/feeds/videos.xml?chart=mostPopular",  FeedSource.YOUTUBE, Category.VIDEOS),
        FeedConfig("techcrunch",   "TechCrunch",        "https://techcrunch.com/feed/",                                FeedSource.RSS,     Category.TECH),
        FeedConfig("bbc-news",     "BBC News",          "http://feeds.bbci.co.uk/news/rss.xml",                        FeedSource.NEWS,    Category.NEWS),
        FeedConfig("the-verge",    "The Verge",         "https://www.theverge.com/rss/index.xml",                      FeedSource.RSS,     Category.TECH),
        FeedConfig("ars-technica", "Ars Technica",      "https://feeds.arstechnica.com/arstechnica/index",             FeedSource.RSS,     Category.TECH),
        FeedConfig("espn",         "ESPN",              "https://www.espn.com/espn/rss/news",                          FeedSource.RSS,     Category.SPORTS),
        FeedConfig("gamespot",     "GameSpot",          "https://www.gamespot.com/feeds/mashup/",                      FeedSource.RSS,     Category.GAMING),
        FeedConfig("wired",        "Wired",             "https://www.wired.com/feed/rss",                              FeedSource.RSS,     Category.TECH),
        FeedConfig("pitchfork",    "Pitchfork",         "https://pitchfork.com/rss/news/",                             FeedSource.RSS,     Category.MUSIC),
        FeedConfig("nasa",         "NASA Breaking News","https://www.nasa.gov/rss/dyn/breaking_news.rss",              FeedSource.NEWS,    Category.NEWS),
        FeedConfig("hacker-news",  "Hacker News",       "https://news.ycombinator.com/rss",                            FeedSource.RSS,     Category.TECH),
    )
}
