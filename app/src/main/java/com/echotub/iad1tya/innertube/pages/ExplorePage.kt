package com.echotube.iad1tya.innertube.pages

import com.echotube.iad1tya.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)
