package com.boardgamegeek.io

import com.boardgamegeek.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.jvm.Throws

class BearerTokenInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        if (BuildConfig.BGG_BEARER_TOKEN.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${BuildConfig.BGG_BEARER_TOKEN}")
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
