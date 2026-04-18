package com.echotube.iad1tya.innertube.models.body

import com.echotube.iad1tya.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
