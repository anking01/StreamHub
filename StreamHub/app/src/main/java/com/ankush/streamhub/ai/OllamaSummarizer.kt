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

private const val TAG = "OllamaSummarizer"
private const val OLLAMA_API_URL = "https://ollama.com/api/chat"

// ─────────────────────────────────────────────────────────────────────────────
// Ollama Cloud AI Summarizer
// Uses Ollama's OpenAI-compatible cloud API (llama models)
// Get your key at: https://ollama.ai
// ─────────────────────────────────────────────────────────────────────────────

class OllamaSummarizer(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun summarize(title: String, rawDescription: String): SummaryState {
        if (apiKey.isBlank()) {
            return SummaryState.Error("Ollama API key nahi hai. Settings mein add karo.")
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

                val reqBody = OllamaRequest(
                    model    = "gpt-oss:120b",
                    messages = listOf(OllamaMessage(role = "user", content = prompt)),
                    stream   = false,
                    options  = OllamaOptions(numPredict = 300)
                )

                val request = Request.Builder()
                    .url(OLLAMA_API_URL)
                    .post(gson.toJson(reqBody).toRequestBody("application/json".toMediaType()))
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()

                Log.d(TAG, "Summarizing via Ollama Cloud: $title")
                val response = client.newCall(request).execute()
                val bodyStr  = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Ollama error ${response.code}: $bodyStr")
                    return@withContext when (response.code) {
                        401  -> SummaryState.Error("Invalid Ollama API key. Settings mein check karo.")
                        429  -> SummaryState.Error("Rate limit hit. Thodi der baad try karo.")
                        503  -> SummaryState.Error("Ollama server temporarily unavailable.")
                        else -> SummaryState.Error("Ollama error: ${response.code}")
                    }
                }

                val parsed = gson.fromJson(bodyStr, OllamaResponse::class.java)
                val text   = parsed.message?.content?.trim().orEmpty()

                if (text.isBlank()) {
                    Log.w(TAG, "Empty response from Ollama")
                    SummaryState.Error("No summary returned")
                } else {
                    Log.d(TAG, "Ollama summary ready for: $title")
                    SummaryState.Success(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ollama summarization failed: ${e.javaClass.simpleName}: ${e.message}")
                SummaryState.Error("Failed to summarize: ${e.message}")
            }
        }
    }

    // ── Request / Response models ─────────────────────────────────────────────

    private data class OllamaRequest(
        val model: String,
        val messages: List<OllamaMessage>,
        val stream: Boolean,
        val options: OllamaOptions
    )

    private data class OllamaOptions(
        @SerializedName("num_predict") val numPredict: Int
    )

    private data class OllamaMessage(
        val role: String,
        val content: String
    )

    private data class OllamaResponse(
        val message: OllamaMessage?
    )
}
