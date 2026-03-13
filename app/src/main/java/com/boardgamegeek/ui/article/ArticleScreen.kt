package com.boardgamegeek.ui.article

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.extensions.toFormattedString
import com.boardgamegeek.model.Article

@Composable
fun ArticleScreen(
    article: Article,
    paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(paddingValues)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.info_background))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = article.username,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = article.postTicks.formatTimestamp(context, isForumTimestamp = true).toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (article.numberOfEdits > 0) {
                val editsText = article.numberOfEdits.toFormattedString()
                Text(
                    text = pluralStringResource(
                        id = R.plurals.edit_timestamp,
                        count = article.numberOfEdits,
                        article.editTicks.formatTimestamp(context, isForumTimestamp = true).toString(),
                        editsText
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        AndroidView(
            factory = { ctx -> WebView(ctx) },
            update = { webView -> webView.setWebViewText(article.body) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        )
    }
}
