package com.example.mydreamtrip.model

data class Destination(
    val id: String,
    val title: String,
    val location: String,
    val ratingText: String,
    val author: String,
    val imageRes: Int = android.R.drawable.ic_menu_gallery,
    val imageUrl: String? = null
)
