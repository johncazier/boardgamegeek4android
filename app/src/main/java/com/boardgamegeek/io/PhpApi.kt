@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.io

import com.boardgamegeek.io.model.CollectionPostResponse
import com.boardgamegeek.io.model.PlayPostResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface PhpApi {
    @POST("geekcollection.php")
    suspend fun collection(@Body play: RequestBody): CollectionPostResponse

    @POST("geekplay.php")
    suspend fun play(@Body play: RequestBody): PlayPostResponse

    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language: en-US,en;q=0.9",
        "Cache-Control: no-cache",
        "Referer: https://boardgamegeek.com/",
    )
    @GET
    suspend fun browsePage(@Url url: String): ResponseBody
}
