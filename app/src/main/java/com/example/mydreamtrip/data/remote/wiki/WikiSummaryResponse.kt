package com.example.mydreamtrip.data.remote.wiki

import com.google.gson.annotations.SerializedName

data class WikiSummaryResponse(
    @SerializedName("title") val title: String? = null,
    @SerializedName("extract") val extract: String? = null,
    @SerializedName("content_urls") val contentUrls: ContentUrls? = null,
    @SerializedName("thumbnail") val thumbnail: Thumbnail? = null
) {
    data class ContentUrls(
        @SerializedName("desktop") val desktop: Desktop? = null
    ) {
        data class Desktop(
            @SerializedName("page") val page: String? = null
        )
    }

    data class Thumbnail(
        @SerializedName("source") val source: String? = null
    )
}
