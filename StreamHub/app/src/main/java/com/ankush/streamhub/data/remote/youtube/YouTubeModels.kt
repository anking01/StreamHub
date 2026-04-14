package com.ankush.streamhub.data.remote.youtube

// ─────────────────────────────────────────────────────────────────────────────
// YouTube Data API v3 — response models
// ─────────────────────────────────────────────────────────────────────────────

// videos.list response
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoItem> = emptyList()
)

data class YouTubeVideoItem(
    val id: String,
    val snippet: VideoSnippet,
    val statistics: VideoStatistics? = null,
    val contentDetails: VideoContentDetails? = null
)

data class VideoSnippet(
    val title: String = "",
    val description: String = "",
    val channelTitle: String = "",
    val publishedAt: String = "",
    val thumbnails: VideoThumbnails = VideoThumbnails(),
    val liveBroadcastContent: String = "none",   // "none" | "live" | "upcoming"
    val categoryId: String? = null
)

data class VideoThumbnails(
    val maxres: ThumbnailItem? = null,
    val high: ThumbnailItem? = null,
    val medium: ThumbnailItem? = null,
    val default: ThumbnailItem? = null
) {
    fun best(): String = maxres?.url ?: high?.url ?: medium?.url ?: default?.url ?: ""
}

data class ThumbnailItem(val url: String = "")

data class VideoStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null
)

data class VideoContentDetails(
    val duration: String? = null   // ISO 8601 e.g. "PT4M13S"
)

// ─────────────────────────────────────────────────────────────────────────────
// search.list response (for live streams / channel videos)
// ─────────────────────────────────────────────────────────────────────────────

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem> = emptyList()
)

data class YouTubeSearchItem(
    val id: SearchItemId = SearchItemId(),
    val snippet: SearchSnippet = SearchSnippet()
)

data class SearchItemId(
    val videoId: String? = null
)

data class SearchSnippet(
    val title: String = "",
    val description: String = "",
    val channelTitle: String = "",
    val publishedAt: String = "",
    val thumbnails: VideoThumbnails = VideoThumbnails(),
    val liveBroadcastContent: String = "none"
)
