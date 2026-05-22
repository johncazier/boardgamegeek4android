package com.boardgamegeek.provider

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.FileNotFoundException

abstract class BaseProvider {
    abstract val path: String

    open fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Unknown uri getting type: $uri")
    }

    open fun query(
        db: SupportSQLiteDatabase,
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        throw UnsupportedOperationException("Unknown uri: $uri")
    }

    @Throws(FileNotFoundException::class)
    open fun openFile(context: Context, db: SupportSQLiteDatabase, uri: Uri, mode: String): ParcelFileDescriptor? {
        throw FileNotFoundException("Unknown uri opening file: $uri")
    }
}
