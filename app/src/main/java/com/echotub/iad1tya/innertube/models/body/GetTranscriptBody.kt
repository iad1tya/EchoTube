package com.echotube.iad1tya.innertube.models.body

import com.echotube.iad1tya.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
