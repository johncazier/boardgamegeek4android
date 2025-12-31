package com.boardgamegeek.ui.geeklists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.livedata.GeekListsPagingSource
import com.boardgamegeek.repository.GeekListRepository
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class GeekListsViewModel @Inject constructor(
    application: Application,
    private val repository: GeekListRepository,
) : AndroidViewModel(application) {
    private val _sort = MutableStateFlow(BggAjaxApi.GeekListSort.HOT)

    fun setSort(sort: SortType) {
        val sortString = when (sort) {
            SortType.HOT -> BggAjaxApi.GeekListSort.HOT
            SortType.RECENT -> BggAjaxApi.GeekListSort.RECENT
            SortType.ACTIVE -> BggAjaxApi.GeekListSort.ACTIVE
        }
        if (_sort.value != sortString) {
            _sort.value = sortString
            Firebase.analytics.logEvent("Sort") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekLists")
                param("SortBy", sort.toString())
            }
        }
    }

    val geekLists = _sort.flatMapLatest { sort ->
        Pager(PagingConfig(GeekListsResponse.PAGE_SIZE)) {
            GeekListsPagingSource(sort, repository)
        }.flow
    }.cachedIn(viewModelScope)


    enum class SortType {
        HOT, RECENT, ACTIVE
    }
}