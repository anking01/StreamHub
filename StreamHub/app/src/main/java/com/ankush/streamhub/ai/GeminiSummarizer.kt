package com.ankush.streamhub.ai

import android.text.Html
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.ankush.streamhub.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiSummarizer"

class GeminiSummarizer {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun summarize(title: String, rawDescription: String): SummaryState {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            Log.e(TAG, "Gemini API key not set")
            return SummaryState.Error("Gemini API key not configured")
        }

        return try {
            val description = Html.fromHtml(rawDescription, Html.FROM_HTML_MODE_COMPACT)
                .toString()
                .trim()

            val prompt = buildString {
                appendLine("Summarize this article in 3 to 5 short bullet points.")
                appendLine("Each bullet must start with '•'. Plain text only, no markdown.")
                appendLine()
                appendLine("Title: $title")
                if (description.isNotBlank()) appendLine("Content: ${description.take(2000)}")
            }

            Log.d(TAG, "Summarizing: $title")
            val response = withContext(Dispatchers.IO) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()

            if (text.isBlank()) {
                Log.w(TAG, "Empty response from Gemini")
                SummaryState.Error("No summary returned")
            } else {
                Log.d(TAG, "Summary ready for: $title")
                SummaryState.Success(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Summarization failed: ${e.javaClass.simpleName}: ${e.message}")
            val msg = e.message ?: ""
            val userMessage = when {
                "quota" in msg.lowercase() -> {
                    val seconds = Regex("retry in ([\\d.]+)s").find(msg)
                        ?.groupValues?.get(1)?.toDoubleOrNull()?.toInt()
                    if (seconds != null) "Rate limit hit. Try again in ${seconds}s"
                    else "Rate limit hit. Try again later"
                }
                else -> "Failed to summarize"
            }
            SummaryState.Error(userMessage)
        }
    }
}
