package com.example.musicplayer

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

data class TrackData(
    val artistName:String,
    val trackTitle: String,
    @RawRes
    val track:Int,
    @DrawableRes
    val trackImage:Int,
)
