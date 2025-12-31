package com.boardgamegeek.ui.geeklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.GeekListRepository
import com.boardgamegeek.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeekListViewModel @Inject constructor(
    application: Application,
    private val geekListRepository: GeekListRepository,
    private val imageRepository: ImageRepository,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    private val _geekListId = MutableStateFlow(BggContract.INVALID_ID)

    fun setId(geekListId: Int) {
        viewModelScope.launch {
            _geekListId.emit(geekListId)
        }
    }

    val geekList: Flow<RefreshableResource<GeekList>> = _geekListId.flatMapLatest { id ->
        flow {
            emit(RefreshableResource.refreshing())
            if (id == BggContract.INVALID_ID) {
                emit(RefreshableResource.error("Invalid ID!"))
            } else {
                val geekList = geekListRepository.getGeekList(id)
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
                emit(RefreshableResource.success(geekList.copy(items = itemsWithImages)))
            }
        }.catch { e ->
            emit(RefreshableResource.error(e, application))
        }.onStart {
            // Emit a refreshing state at the start of the flow
            // This is optional and depends on UI requirements
            emit(RefreshableResource.refreshing())
        }
    }
}