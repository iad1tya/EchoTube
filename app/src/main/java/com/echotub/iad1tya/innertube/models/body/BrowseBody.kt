package com.echotube.iad1tya.innertube.models.body

import com.echotube.iad1tya.innertube.models.Context
import com.echotube.iad1tya.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
