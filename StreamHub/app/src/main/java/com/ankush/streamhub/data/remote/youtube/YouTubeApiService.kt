package com.ankush.streamhub.data.remote.youtube

import retrofit2.http.GET
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────────────────────
// YouTube Data API v3 — Retrofit interface
// Base URL: https://www.googleapis.com/youtube/v3/
// ─────────────────────────────────────────────────────────────────────────────

interface YouTubeApiService {

    // Trending / most-popular videos (optionally filtered by category)
    @GET("videos")
    suspend fun getTrendingVideos(
        @Query("part")            part: String = "snippet,statistics,contentDetails",
        @Query("chart")           chart: String = "mostPopular",
        @Query("regionCode")      regionCode: String = "IN",
        @Query("maxResults")      maxResults: Int = 30,
        @Query("videoCategoryId") categoryId: String? = null,
        @Query("key")             apiKey: String
    ): YouTubeVideoListResponse

    // Live streams currently broadcasting
    @GET("search")
    suspend fun getLiveStreams(
        @Query("part")       part: String = "snippet",
        @Query("eventType")  eventType: String = "live",
        @Query("type")       type: String = "video",
        @Query("order")      order: String = "viewCount",
        @Query("maxResults") maxResults: Int = 30,
        @Query("key")        apiKey: String
    ): YouTubeSearchResponse

    // Videos from a specific channel
    @GET("search")
    suspend fun getChannelVideos(
        @Query("part")       part: String = "snippet",
        @Query("channelId")  channelId: String,
        @Query("type")       type: String = "video",
        @Query("order")      order: String = "date",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key")        apiKey: String
    ): YouTubeSearchResponse

    // General keyword search
    @GET("search")
    suspend fun searchVideos(
        @Query("part")       part: String = "snippet",
        @Query("q")          query: String,
        @Query("type")       type: String = "video",
        @Query("order")      order: String = "relevance",
        @Query("maxResults") maxResults: Int = 30,
        @Query("key")        apiKey: String
    ): YouTubeSearchResponse
}
