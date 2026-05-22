package com.boardgamegeek.provider

import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Games

class GamesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Games.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#"

    override fun query(
        db: SupportSQLiteDatabase,
        uri: Uri, // content://com.boardgamegeek/games/13
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val gameId = Games.getGameId(uri)
        val clauses = mutableListOf("${Games.Columns.GAME_ID} = ?")
        val args = mutableListOf<Any>(gameId)
        if (!selection.isNullOrBlank()) {
            clauses += "($selection)"
            selectionArgs?.let(args::addAll)
        }
        val query = SupportSQLiteQueryBuilder.builder("games")
            .columns(projection)
            .selection(clauses.joinToString(" AND "), args.toTypedArray())
            .orderBy(sortOrder)
            .create()
        return db.query(query)
    }
}
