package com.boardgamegeek.repository

import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.model.TopGame
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import retrofit2.HttpException
import javax.inject.Inject

class TopGameRepository @Inject constructor(
    private val phpApi: PhpApi,
) {
    suspend fun findTopGames(): List<TopGame> = withContext(Dispatchers.IO) {
        val doc = fetchBrowseDocument()
        val gameRows = doc.select("tr[id^=row_]")
        gameRows.mapIndexedNotNull { index, row ->
            val gameNameElement = row.selectFirst(
                "td.collection_objectname a.primary, " +
                    "td.collection_objectname a[href*=/boardgame/], " +
                    "td.collection_objectname a[href*=/boardgameexpansion/]",
            ) ?: return@mapIndexedNotNull null

            val id = row.extractId(gameNameElement.attr("href"))
            val name = gameNameElement.text().trim()
            if (name.isBlank()) return@mapIndexedNotNull null

            val rank = row.selectFirst("td.collection_rank")
                ?.text()
                .orEmpty()
                .filter(Char::isDigit)
                .toIntOrNull()
                ?: (index + 1)

            val yearPublishedText = YEAR_REGEX
                .find(row.selectFirst("td.collection_objectname .smallerfont.dull")?.text().orEmpty())
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?: TopGame.YEAR_UNKNOWN

            val thumbnailUrl = row.selectFirst("td.collection_thumbnail img")
                ?.extractImageUrl()
                .orEmpty()

            TopGame(
                id,
                name,
                rank,
                yearPublishedText,
                thumbnailUrl,
            )
        }
    }

    private fun Element.extractId(href: String): Int =
        GAME_ID_REGEX.find(href)?.groupValues?.get(1)?.toIntOrNull() ?: INVALID_ID

    private fun Element.extractImageUrl(): String {
        val src = attr("src").trim()
        if (src.isNotEmpty()) return normalizeUrl(src)

        val dataSrc = attr("data-src").trim()
        if (dataSrc.isNotEmpty()) return normalizeUrl(dataSrc)

        val srcSetCandidate = attr("srcset").trim().ifEmpty { attr("data-srcset").trim() }
        if (srcSetCandidate.isNotEmpty()) {
            val firstSrcSetUrl = srcSetCandidate
                .substringBefore(',')
                .trim()
                .substringBefore(' ')
                .trim()
            if (firstSrcSetUrl.isNotEmpty()) return normalizeUrl(firstSrcSetUrl)
        }

        return ""
    }

    private fun normalizeUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> "https://boardgamegeek.com$url"
        else -> url
    }

    private suspend fun fetchBrowseDocument() = Jsoup.parse(
        fetchBrowseHtml(),
        "https://boardgamegeek.com/",
    )

    private suspend fun fetchBrowseHtml(): String {
        var lastError: Throwable? = null
        BROWSE_URLS.forEach { url ->
            try {
                return phpApi.browsePage(url).string()
            } catch (e: HttpException) {
                lastError = e
                if (e.code() != 403) throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Unable to load boardgame browse page.")
    }

    private companion object {
        val BROWSE_URLS = listOf(
            "https://boardgamegeek.com/browse/boardgame",
            "https://www.boardgamegeek.com/browse/boardgame",
        )
        val GAME_ID_REGEX = Regex("/(?:boardgame|boardgameexpansion)/(\\d+)")
        val YEAR_REGEX = Regex("\\((\\d{4})\\)")
    }
}
