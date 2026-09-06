package com.boardgamegeek.model

data class GeekBuddyAnalysis(
    val sections: List<Section>,
    val message: String = "",
) {
    data class Section(
        val title: String,
        val buddies: List<Buddy>,
    )

    data class Buddy(
        val username: String,
        val details: List<Detail>,
    )

    data class Detail(
        val label: String,
        val value: String,
    )
}
