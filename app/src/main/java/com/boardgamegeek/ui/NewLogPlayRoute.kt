package com.boardgamegeek.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewLogPlayRoute(
    val gameId: Int,
    val gameName: String,
    val gameImageUrl: String
): Parcelable
