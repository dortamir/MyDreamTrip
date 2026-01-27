package com.example.mydreamtrip.model

data class Destination(
    val id: String,
    val title: String,
    val location: String,
    val ratingText: String,
    val author: String,
    val imageRes: Int,
    val localImageUri: String? = null,

    // Wikipedia fields
    val wikiTitle: String = "",
    val wikiExtract: String = "",
    val wikiUrl: String = "",
    val wikiImageUrl: String = ""
)
