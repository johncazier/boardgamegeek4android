package com.boardgamegeek.ui.geeklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.GeekListRepository
import com.boardgamegeek.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GeekListViewModel @Inject constructor(
    application: Application,
    private val geekListRepository: GeekListRepository,
    private val imageRepository: ImageRepository,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    private val _geekListId = MutableLiveData<Int>()

    fun setId(geekListId: Int) {
        if (_geekListId.value != geekListId) _geekListId.value = geekListId
    }

    val geekList: LiveData<RefreshableResource<GeekList>> = _geekListId.switchMap { id ->
        liveData {
            emit(RefreshableResource.Companion.refreshing(latestValue?.data))
            if (id == BggContract.Companion.INVALID_ID) {
                emit(RefreshableResource.Companion.error("Invalid ID!"))
            } else {
                try {
                    val geekList = geekListRepository.getGeekList(id)
                    emit(RefreshableResource.Companion.refreshing(geekList))
                    val itemsWithImages = mutableListOf<GeekListItem>()
                    geekList.items.forEach {
                        itemsWithImages += if (it.thumbnailUrls == null || it.heroImageUrls == null) {
                            val urlPair = if (it.imageId == 0) {
                                val url = gameRepository.fetchGameThumbnail(it.objectId)
                                listOf(url.orEmpty()) to listOf(url.orEmpty())
                            } else {
                                val urls = imageRepository.getImageUrls(it.imageId)
                                urls[ImageRepository.ImageType.THUMBNAIL] to urls[ImageRepository.ImageType.HERO]
                            }
                            it.copy(thumbnailUrls = urlPair.first, heroImageUrls = urlPair.second)
                        } else it
                    }
                    emit(RefreshableResource.Companion.success(geekList.copy(items = itemsWithImages)))
                } catch (e: Exception) {
                    emit(RefreshableResource.Companion.error(e, application))
                }
            }
        }
    }
}