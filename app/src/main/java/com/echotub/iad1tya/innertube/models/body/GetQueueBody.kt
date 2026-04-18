package com.echotube.iad1tya.innertube.models.body

import com.echotube.iad1tya.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetQueueBody(
    val context: Context,
    val videoIds: List<String>?,
    val playlistId: String?,
)
