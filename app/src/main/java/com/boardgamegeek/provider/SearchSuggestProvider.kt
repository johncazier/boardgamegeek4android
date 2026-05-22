package com.boardgamegeek.provider

import android.app.SearchManager
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import java.util.*

/**
 * Called from the search widget to populate the drop down list.
 * content://com.boardgamegeek/search_suggest_query/keyword?limit=50
 */
open class SearchSuggestProvider : BaseProvider() {
    override fun getType(uri: Uri) = SearchManager.SUGGEST_MIME_TYPE

    override val path = "${SearchManager.SUGGEST_URI_PATH_QUERY}/*"

    override fun query(
        db: SupportSQLiteDatabase,
        uri: Uri, // content://com.boardgamegeek/search_suggest_query/foo?limit=50
        projection: Array<String>?, // null
        selection: String?, // null
        selectionArgs: Array<String>?, // null
        sortOrder: String?, // null
    ): Cursor? {
        val searchTerm = uri.lastPathSegment?.lowercase(Locale.getDefault()).orEmpty()
        val limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
        val limitClause = limit?.let { "LIMIT $it" }.orEmpty()
        val query = SimpleSQLiteQuery(
            """
            SELECT games.${BaseColumns._ID} AS ${BaseColumns._ID},
                ${Collection.Columns.COLLECTION_NAME} AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
                IFNULL(CASE WHEN ${Collection.Columns.COLLECTION_YEAR_PUBLISHED}=0 THEN NULL ELSE ${Collection.Columns.COLLECTION_YEAR_PUBLISHED} END, '?') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
                '${Games.CONTENT_URI}/' || games.${Games.Columns.GAME_ID} || '/$PATH_THUMBNAILS' AS ${SearchManager.SUGGEST_COLUMN_ICON_2},
                games.${Games.Columns.GAME_ID} AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID},
                ${Collection.Columns.COLLECTION_NAME} AS ${SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA},
                ${Games.Columns.LAST_VIEWED} AS ${SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT}
            FROM collection
            LEFT OUTER JOIN games ON collection.${Games.Columns.GAME_ID}=games.${Games.Columns.GAME_ID}
            WHERE ${Collection.Columns.COLLECTION_NAME} LIKE ?
                OR ${Collection.Columns.COLLECTION_NAME} LIKE ?
            GROUP BY $GROUP_BY
            ORDER BY $SORT_BY
            $limitClause
            """.trimIndent(),
            arrayOf("$searchTerm%", "% $searchTerm%")
        )
        return db.query(query)
    }

    companion object {
        private const val SORT_BY = "${Collection.Columns.COLLECTION_SORT_NAME}${BggContract.COLLATE_NOCASE} ASC"
        private const val GROUP_BY = "${Collection.Columns.COLLECTION_NAME}, ${Collection.Columns.COLLECTION_YEAR_PUBLISHED}"
    }
}
