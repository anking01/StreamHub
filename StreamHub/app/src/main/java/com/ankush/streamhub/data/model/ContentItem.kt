package com.ankush.streamhub.data.model

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
// Adapter list item — header OR content card (used for sectioned search results)
// ─────────────────────────────────────────────────────────────────────────────

sealed class FeedListItem {
    data class Header(val title: String, val emoji: String, val count: Int) : FeedListItem()
    data class Item(val contentItem: ContentItem) : FeedListItem()
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
        // ── YouTube India (requires YOUTUBE_API_KEY) ──────────────────────────
        FeedConfig("yt-trending",  "YouTube India",         "yt://trending",         FeedSource.YOUTUBE, Category.VIDEOS),
        FeedConfig("yt-music",     "YouTube Music India",   "yt://trending/music",   FeedSource.YOUTUBE, Category.MUSIC),
        FeedConfig("yt-news",      "YouTube News India",    "yt://trending/news",    FeedSource.YOUTUBE, Category.NEWS),
        FeedConfig("yt-tech",      "YouTube Tech India",    "yt://trending/tech",    FeedSource.YOUTUBE, Category.TECH),
        FeedConfig("yt-sports",    "YouTube Sports India",  "yt://trending/sports",  FeedSource.YOUTUBE, Category.SPORTS),
        FeedConfig("yt-gaming",    "YouTube Gaming India",  "yt://trending/gaming",  FeedSource.YOUTUBE, Category.GAMING),
        FeedConfig("yt-live",      "YouTube Live India",    "yt://live",             FeedSource.YOUTUBE, Category.LIVE),

        // ── Indian News ───────────────────────────────────────────────────────
        FeedConfig("ndtv",         "NDTV",                  "https://feeds.feedburner.com/ndtvnews-top-stories",                    FeedSource.NEWS, Category.NEWS),
        FeedConfig("toi",          "Times of India",         "https://timesofindia.indiatimes.com/rssfeedstopstories.cms",          FeedSource.NEWS, Category.NEWS),
        FeedConfig("ht",           "Hindustan Times",        "https://www.hindustantimes.com/feeds/rss/india-news/rssfeed.xml",     FeedSource.NEWS, Category.NEWS),
        FeedConfig("india-today",  "India Today",            "https://www.indiatoday.in/rss/home",                                  FeedSource.NEWS, Category.NEWS),
        FeedConfig("the-hindu",    "The Hindu",              "https://www.thehindu.com/news/national/feeder/default.rss",           FeedSource.NEWS, Category.NEWS),
        FeedConfig("et-business",  "Economic Times",         "https://economictimes.indiatimes.com/rssfeedstopstories.cms",         FeedSource.NEWS, Category.NEWS),

        // ── Indian Tech ───────────────────────────────────────────────────────
        FeedConfig("gadgets360",   "Gadgets360",             "https://gadgets.ndtv.com/rss/feeds",                                  FeedSource.RSS,  Category.TECH),
        FeedConfig("digit",        "Digit India",            "https://www.digit.in/rss/news.xml",                                   FeedSource.RSS,  Category.TECH),
        FeedConfig("91mobiles",    "91mobiles",              "https://www.91mobiles.com/hub/feed/",                                 FeedSource.RSS,  Category.TECH),
        FeedConfig("techpp",       "TechPP",                 "https://techpp.com/feed/",                                            FeedSource.RSS,  Category.TECH),

        // ── Indian Sports (Cricket) ───────────────────────────────────────────
        FeedConfig("cricinfo",     "ESPNCricinfo",           "https://www.espncricinfo.com/rss/content/story/feeds/0.xml",          FeedSource.RSS,  Category.SPORTS),
        FeedConfig("khelnow",      "Khel Now",               "https://khelnow.com/feed",                                            FeedSource.RSS,  Category.SPORTS),
        FeedConfig("crictracker",  "CricTracker",            "https://www.crictracker.com/feed/",                                   FeedSource.RSS,  Category.SPORTS),

        // ── Bollywood / Entertainment ─────────────────────────────────────────
        FeedConfig("bw-hungama",   "Bollywood Hungama",      "https://www.bollywoodhungama.com/rss/news.xml",                       FeedSource.RSS,  Category.MUSIC),
        FeedConfig("pinkvilla",    "Pinkvilla",              "https://www.pinkvilla.com/rss.xml",                                   FeedSource.RSS,  Category.MUSIC),
        FeedConfig("koimoi",       "Koimoi",                 "https://www.koimoi.com/feed/",                                        FeedSource.RSS,  Category.MUSIC),
    )
}
