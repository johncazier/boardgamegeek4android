package com.boardgamegeek.di

import android.content.Context
import com.boardgamegeek.db.BggDatabase
import com.boardgamegeek.db.BggDatabaseFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun providesBggDatabase(
        @ApplicationContext context: Context,
    ): BggDatabase = BggDatabaseFactory.build(context)
}
