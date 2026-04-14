package com.ankush.streamhub.ai

sealed class SummaryState {
    object Idle : SummaryState()
    object Loading : SummaryState()
    data class Success(val bullets: String) : SummaryState()
    data class Error(val message: String) : SummaryState()
}
