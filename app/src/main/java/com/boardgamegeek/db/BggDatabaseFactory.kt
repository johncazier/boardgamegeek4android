package com.boardgamegeek.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object BggDatabaseFactory {
    const val DATABASE_NAME = "bgg.db"

    private val LEGACY_SQLITE_VERSIONS = IntArray(60) { it + 1 }

    fun build(context: Context): BggDatabase =
        Room.databaseBuilder(
            context,
            BggDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(
            DatabaseMigrations.MIGRATION_61_62,
            DatabaseMigrations.MIGRATION_62_63,
            DatabaseMigrations.MIGRATION_63_64,
            DatabaseMigrations.MIGRATION_64_65,
        ).fallbackToDestructiveMigrationFrom(
            true,
            *LEGACY_SQLITE_VERSIONS,
        ).addCallback(
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA synchronous = NORMAL")
                }
            }
        ).build()
}
