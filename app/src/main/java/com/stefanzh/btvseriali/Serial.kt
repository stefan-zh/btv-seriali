package com.stefanzh.btvseriali

import android.graphics.Bitmap

data class SerialLink(
    val link: String,
    val imageUrl: String,
    val image: Bitmap,
    val title: String
)

data class Serial(
    val title: String,
    val imageUrl: String,
    val image: Bitmap,
    val description: String,
    val episodes: List<Epizod>
)

data class Epizod(
    val link: String,
    val imageUrl: String,
    val image: Bitmap,
    val name: String,
    val length: String,
    val available: Boolean
)