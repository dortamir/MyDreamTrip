package com.example.mydreamtrip.data.remote.wiki

import android.net.Uri

class WikiRepository {

    suspend fun fetchDestinationInfo(query: String): DestinationInfo {
        // encode for URL path
        val encoded = Uri.encode(query.trim())

        val res = WikiClient.api.getSummary(encoded)

        return DestinationInfo(
            wikiTitle = res.title ?: query,
            wikiExtract = res.extract ?: "",
            wikiUrl = res.contentUrls?.desktop?.page,
            wikiImageUrl = res.thumbnail?.source
        )
    }
}
