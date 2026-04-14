package com.ankush.streamhub.ai

import android.text.Html
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private const val TAG = "GroqSummarizer"
private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

class GroqSummarizer(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun summarize(title: String, rawDescription: String): SummaryState {
        if (apiKey.isBlank()) {
            return SummaryState.Error("Groq API key nahi hai. Settings mein add karo.")
        }

        return withContext(Dispatchers.IO) {
            try {
                val description = Html.fromHtml(rawDescription, Html.FROM_HTML_MODE_COMPACT)
                    .toString().trim()

                val prompt = buildString {
                    appendLine("Summarize this article in 3 to 5 short bullet points.")
                    appendLine("Each bullet must start with '•'. Plain text only, no markdown.")
                    appendLine()
                    appendLine("Title: $title")
                    if (description.isNotBlank()) appendLine("Content: ${description.take(2000)}")
                }

                val reqBody = GroqRequest(
                    model    = "llama-3.1-8b-instant",
                    messages = listOf(GroqMessage(role = "user", content = prompt)),
                    maxTokens = 300
                )

                val request = Request.Builder()
                    .url(GROQ_API_URL)
                    .post(gson.toJson(reqBody).toRequestBody("application/json".toMediaType()))
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()

                Log.d(TAG, "Summarizing via Groq: $title")
                val response = client.newCall(request).execute()
                val bodyStr  = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Groq error ${response.code}: $bodyStr")
                    return@withContext when (response.code) {
                        401  -> SummaryState.Error("Invalid Groq API key. Settings mein check karo.")
                        429  -> SummaryState.Error("Rate limit hit. Thodi der baad try karo.")
                        else -> SummaryState.Error("Groq error: ${response.code}")
                    }
                }

                val parsed = gson.fromJson(bodyStr, GroqResponse::class.java)
                val text   = parsed.choices.firstOrNull()?.message?.content?.trim().orEmpty()

                if (text.isBlank()) {
                    Log.w(TAG, "Empty response from Groq")
                    SummaryState.Error("No summary returned")
                } else {
                    Log.d(TAG, "Groq summary ready for: $title")
                    SummaryState.Success(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Groq summarization failed: ${e.javaClass.simpleName}: ${e.message}")
                SummaryState.Error("Failed to summarize: ${e.message}")
            }
        }
    }

    // ── Request / Response models (private, only used here) ──────────────────

    private data class GroqRequest(
        val model: String,
        val messages: List<GroqMessage>,
        @SerializedName("max_tokens") val maxTokens: Int
    )

    private data class GroqMessage(
        val role: String,
        val content: String
    )

    private data class GroqResponse(
        val choices: List<GroqChoice>
    )

    private data class GroqChoice(
        val message: GroqMessage
    )
}
