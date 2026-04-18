package com.echotube.iad1tya.innertube.pages

import com.echotube.iad1tya.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
