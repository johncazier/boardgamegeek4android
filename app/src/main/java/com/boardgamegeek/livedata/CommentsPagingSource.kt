package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.io.model.GameRemote
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameComment
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import retrofit2.HttpException
import timber.log.Timber

class CommentsPagingSource(val gameId: Int, private val sortByRating: Boolean = false, val repository: GameRepository) :
    PagingSource<Int, GameComment>() {
    override fun getRefreshKey(state: PagingState<Int, GameComment>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GameComment> {
        return try {
            if (gameId == BggContract.INVALID_ID) return LoadResult.Error(Exception("Invalid ID"))

            val pageKey = params.key ?: if (sortByRating) FIRST_UNRATED_COMMENTS_PAGE else FIRST_PAGE
            if (sortByRating) {
                loadRatingsAndUnratedComments(pageKey)
            } else {
                loadComments(pageKey)
            }
        } catch (e: Exception) {
            if (e is HttpException) {
                Timber.w("Error code: ${e.code()}\n${e.response()?.body()}")
            } else {
                Timber.w(e)
            }
            LoadResult.Error(e)
        }
    }

    private suspend fun loadComments(page: Int): LoadResult.Page<Int, GameComment> {
        val result = repository.loadComments(gameId, page)
        return LoadResult.Page(
            data = result?.ratings.orEmpty(),
            prevKey = null,
            nextKey = nextPage(page, result?.numberOfRatings),
        )
    }

    private suspend fun loadRatingsAndUnratedComments(pageKey: Int): LoadResult.Page<Int, GameComment> {
        // BGG sorts unrated comments before rated comments. Negative keys page through that prefix of
        // the comments feed; positive keys then page through the complete ratings feed.
        return if (pageKey > 0) {
            loadRatings(pageKey)
        } else {
            val page = -pageKey
            val result = repository.loadComments(gameId, page)
            val comments = result?.ratings.orEmpty()
            val unratedComments = comments.filter { it.rating == Game.UNRATED }
            val nextCommentsPage = nextPage(page, result?.numberOfRatings)

            if (unratedComments.isEmpty()) {
                loadRatings(FIRST_PAGE)
            } else {
                LoadResult.Page(
                    data = unratedComments,
                    prevKey = null,
                    nextKey = if (unratedComments.size == comments.size && nextCommentsPage != null) {
                        -nextCommentsPage
                    } else {
                        FIRST_PAGE
                    },
                )
            }
        }
    }

    private suspend fun loadRatings(page: Int): LoadResult.Page<Int, GameComment> {
        val result = repository.loadRatings(gameId, page)
        return LoadResult.Page(
            data = result?.ratings.orEmpty(),
            prevKey = null,
            nextKey = nextPage(page, result?.numberOfRatings),
        )
    }

    private fun nextPage(page: Int, totalItems: Int?): Int? {
        return if (page * GameRemote.PAGE_SIZE < (totalItems ?: 0)) page + 1 else null
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val FIRST_UNRATED_COMMENTS_PAGE = -1
    }
}
