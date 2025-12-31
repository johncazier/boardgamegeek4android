package com.boardgamegeek.ui.components

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml

@Composable
fun BggHtmlText(
    text: String?,
    modifier: Modifier = Modifier,
    fromHtmlFlags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY,
    useLinkMovementMethod: Boolean = true,
    tagHandler: Html.TagHandler? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                if (useLinkMovementMethod) {
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
        },
        update = { textView ->
            when {
                text == null -> textView.text = ""
                text.isBlank() -> textView.text = ""
                text.contains("<") && text.contains(">") || text.contains("&") && text.contains(";") -> {
                    var html = text.trim()
                    // Fix up problematic HTML
                    // replace DIVs with BR
                    html = html.replace("<div[^>]*>".toRegex(), "")
                    html = html.replace("</div>".toRegex(), "<br/>")
                    // remove all P tags
                    html = html.replace("<(/)?p>".toRegex(), "")
                    // remove trailing BRs
                    html = html.replace("(<br\\s?/>)+$".toRegex(), "")
                    // use BRs instead of &#10; (ASCII 10 = new line)
                    html = html.replace("&#10;".toRegex(), "<br/>")
                    // use BRs instead of new line character
                    html = html.replace("\n".toRegex(), "<br/>")
                    // replace 3+ BRs with a double
                    html = html.replace("(<br\\s?/>){3,}".toRegex(), "<br/><br/>")
                    html = fixInternalLinks(html)

                    val spanned = html.parseAsHtml(fromHtmlFlags, null, tagHandler)
                    textView.text = spanned.trim()
                }
                else -> textView.text = text
            }
        }
    )
}

private fun fixInternalLinks(html: String): String {
    // ensure internal, path-only links are complete with the hostname
    if (html.isBlank()) return ""
    var fixedText = html.replace("<a\\s+href=\"/".toRegex(), "<a href=\"https://www.boardgamegeek.com/")
    fixedText = fixedText.replace("<img\\s+src=\"//".toRegex(), "<img src=\"https://")
    return fixedText
}
