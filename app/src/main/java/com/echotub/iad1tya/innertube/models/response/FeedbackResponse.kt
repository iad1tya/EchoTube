package com.echotube.iad1tya.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackResponse(
    val feedbackResponses: List<Status>,
) {
    @Serializable
    data class Status(
        val isProcessed: Boolean,
    )
}
