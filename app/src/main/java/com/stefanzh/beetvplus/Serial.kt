package com.stefanzh.beetvplus

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SerialLink(
    val link: String,
    val imageUrl: String,
    val image: Bitmap,
    val title: String
) : Parcelable

data class Serial(
    val title: String,
    val imageUrl: String,
    val image: Bitmap,
    val description: String,
    val episodes: List<Epizod>
)

@Parcelize
data class Epizod(
    val link: String,
    val imageUrl: String,
    val image: Bitmap,
    val name: String,
    val length: String,
    val isAvailable: Boolean
) : Parcelable

@Parcelize
data class Category(
    val title: String,
    val link: String
) : Parcelable