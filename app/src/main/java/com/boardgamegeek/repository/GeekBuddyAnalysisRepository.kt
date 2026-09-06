package com.boardgamegeek.repository

import com.boardgamegeek.io.PhpApi
import com.boardgamegeek.model.GeekBuddyAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.inject.Inject

class GeekBuddyAnalysisRepository @Inject constructor(
    private val phpApi: PhpApi,
) {
    suspend fun load(gameId: Int): GeekBuddyAnalysis = withContext(Dispatchers.IO) {
        require(gameId > 0) { "Invalid game ID." }
        val url = "$BASE_URL/geekbuddy/analyze/thing/$gameId"
        val document = Jsoup.parse(phpApi.browsePage(url).string(), url)

        if (document.isLoginPage()) {
            throw IllegalStateException("Sign in to view GeekBuddy analysis.")
        }

        val sections = document.select("table.forum_table")
            .mapNotNull(::parseTable)

        GeekBuddyAnalysis(
            sections = sections,
            message = if (sections.isEmpty()) document.analysisMessage() else "",
        )
    }

    private fun parseTable(table: Element): GeekBuddyAnalysis.Section? {
        val rows = table.select("tr")
        val headers = rows.firstOrNull { it.select("> th").isNotEmpty() }
            ?.select("> th")
            ?.map { it.text().normalized() }
            .orEmpty()

        val buddies = rows.mapNotNull { row ->
            // User cells contain a nested profile-tools table. Only use cells that
            // belong directly to the analysis row so those nested rows are ignored.
            val cells = row.select("> td")
            if (cells.isEmpty()) return@mapNotNull null

            val userLink = cells.first()!!
                .selectFirst("a[href^=/profile/], a[href*=boardgamegeek.com/profile/]")
                ?: return@mapNotNull null
            val username = userLink.text().normalized().ifBlank {
                PROFILE_PATH_REGEX.find(userLink.attr("href"))
                    ?.groupValues
                    ?.get(1)
                    ?.normalized()
                    .orEmpty()
            }.ifBlank { return@mapNotNull null }

            val details = cells.mapIndexedNotNull { index, cell ->
                if (index == 0) return@mapIndexedNotNull null
                val value = cell.text().normalized()
                if (value.isBlank()) return@mapIndexedNotNull null
                val label = headers.getOrNull(index).orEmpty().ifBlank {
                    cell.classNames()
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                        .replace('_', ' ')
                        .replace('-', ' ')
                        .normalized()
                }
                if (label.equals("Status", ignoreCase = true)) return@mapIndexedNotNull null
                GeekBuddyAnalysis.Detail(label = label, value = value)
            }

            GeekBuddyAnalysis.Buddy(username = username, details = details)
        }

        if (buddies.isEmpty()) return null
        return GeekBuddyAnalysis.Section(
            title = table.findSectionTitle(),
            buddies = buddies,
        )
    }

    private fun Element.findSectionTitle(): String {
        var element: Element? = this
        repeat(MAX_HEADING_SEARCH_DEPTH) {
            element = element?.previousElementSibling()
            val heading = element?.takeIf { it.tagName() in HEADING_TAGS }
                ?: element?.selectFirst("h1, h2, h3, h4, h5, h6")
            heading?.text()?.normalized()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return "GeekBuddies"
    }

    private fun Document.isLoginPage(): Boolean =
        selectFirst("form[action*=login], input[name=username], input[name=password]") != null ||
            location().contains("/login")

    private fun Document.analysisMessage(): String =
        select("#maincontent .messagebox, #maincontent .alert, #maincontent p, main .alert, main p")
            .map { it.text().normalized() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

    private fun String.normalized(): String = replace(WHITESPACE_REGEX, " ").trim()

    private companion object {
        const val BASE_URL = "https://boardgamegeek.com"
        const val MAX_HEADING_SEARCH_DEPTH = 5
        val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
        val PROFILE_PATH_REGEX = Regex("/profile/([^/?#]+)", RegexOption.IGNORE_CASE)
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
