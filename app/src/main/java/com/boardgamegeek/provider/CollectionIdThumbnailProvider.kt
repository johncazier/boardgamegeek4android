package com.boardgamegeek.provider

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import com.boardgamegeek.util.FileUtils
import java.io.File
import java.io.FileNotFoundException

/**
 * content://com.boardgamegeek/collection/123456/thumbnails
 */
class CollectionIdThumbnailProvider : BaseProvider() {
    override val path = "$PATH_COLLECTION/#/$PATH_THUMBNAILS"

    @Throws(FileNotFoundException::class)
    override fun openFile(context: Context, db: SupportSQLiteDatabase, uri: Uri, mode: String): ParcelFileDescriptor? {
        val file = getFile(context, db, uri)
        if (file?.exists() != true) {
            throw FileNotFoundException("Couldn't get the file at the specified path.")
        }
        val parcelMode = calculateParcelMode(uri, mode)
        return ParcelFileDescriptor.open(file, parcelMode)
    }

    /**
     * Get a `File` representing the URI. E.g. content://com.boardgamegeek/collection/123456/thumbnails
     * becomes the file from /storage/emulated/0/Android/data/com.boardgamegeek/files/thumbnails/pic1115825.jpg
     */
    private fun getFile(context: Context, db: SupportSQLiteDatabase, uri: Uri): File? {
        val fileName = generateFileName(db, uri)
        return FileUtils.getFile(context, PATH_THUMBNAILS, fileName)
    }

    /**
     * Generates a file name based on the URI. E.g. content://com.boardgamegeek/games/110308/thumbnails
     * becomes pic1115825.jpg
     */
    private fun generateFileName(db: SupportSQLiteDatabase, uri: Uri): String? {
        val collectionId = Collection.getCollectionId(uri)
        return db.query(
            SimpleSQLiteQuery(
                """
                SELECT collection.${Collection.Columns.COLLECTION_THUMBNAIL_URL}, games.${Games.Columns.THUMBNAIL_URL}
                FROM collection
                LEFT OUTER JOIN games ON collection.${Games.Columns.GAME_ID}=games.${Games.Columns.GAME_ID}
                WHERE collection.${Collection.Columns.COLLECTION_ID} = ?
                """.trimIndent(),
                arrayOf(collectionId)
            )
        ).use { cursor ->
            if (cursor.count == 1 && cursor.moveToFirst()) {
                cursor.getString(0) ?: cursor.getString(1)
            } else {
                null
            }
        }
    }

    companion object {
        // from Android ContentResolver.modeToMode
        @Throws(FileNotFoundException::class)
        fun calculateParcelMode(uri: Uri, mode: String): Int {
            return when (mode) {
                "r" -> ParcelFileDescriptor.MODE_READ_ONLY
                "w", "wt" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                "wa" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_APPEND
                "rw" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                "rwt" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                "" -> throw FileNotFoundException("Missing mode for $uri")
                else -> throw FileNotFoundException("Bad mode for $uri: $mode")
            }
        }
    }
}
