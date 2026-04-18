package com.echotube.iad1tya.innertube.pages

import com.echotube.iad1tya.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
