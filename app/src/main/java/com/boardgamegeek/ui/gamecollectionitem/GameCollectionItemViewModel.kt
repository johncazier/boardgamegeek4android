package com.boardgamegeek.ui.gamecollectionitem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@HiltViewModel
class GameCollectionItemViewModel @Inject constructor(
    private val gameCollectionRepository: GameCollectionRepository,
) : ViewModel() {
    private val refreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)
    private val internalId = MutableStateFlow<Long?>(null)
    private val isEditModeState = MutableStateFlow(false)
    private val isEditedState = MutableStateFlow(false)
    private val isRefreshingState = MutableStateFlow(false)
    private val errorState = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val isEditMode: StateFlow<Boolean> = isEditModeState
    val isEdited: StateFlow<Boolean> = isEditedState
    val isRefreshing: StateFlow<Boolean> = isRefreshingState
    val error: SharedFlow<String> = errorState.asSharedFlow()

    val item: StateFlow<CollectionItem?> = internalId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else gameCollectionRepository.loadCollectionItemFlow(id).distinctUntilChanged()
        }
        .stateInWhileSubscribed(viewModelScope, null)

    fun setInternalId(id: Long) {
        if (internalId.value != id) {
            internalId.value = id
        }
    }

    fun enableEditMode() {
        isEditModeState.value = true
    }

    fun disableEditMode() {
        isEditModeState.value = false
    }

    fun markEdited() {
        isEditedState.value = true
    }

    fun refresh(force: Boolean = true) {
        val collectionItem = item.value ?: return
        viewModelScope.launch {
            if (!force && !collectionItem.syncTimestamp.isOlderThan(refreshMinutes.minutes)) return@launch
            isRefreshingState.value = true
            try {
                gameCollectionRepository.refreshCollectionItem(collectionItem.gameId, collectionItem.collectionId, collectionItem.subtype)?.let {
                    errorState.tryEmit(it)
                }
            } catch (e: Exception) {
                errorState.tryEmit(e.localizedMessage.orEmpty())
            } finally {
                isRefreshingState.value = false
            }
        }
    }

    fun delete() {
        val collectionItem = item.value ?: return
        viewModelScope.launch {
            gameCollectionRepository.markAsDeleted(collectionItem.internalId)
            gameCollectionRepository.enqueueUploadRequest(collectionItem.gameId)
            isEditedState.value = false
        }
    }

    fun reset() {
        val id = internalId.value ?: return
        viewModelScope.launch {
            gameCollectionRepository.resetTimestamps(id)
            isEditedState.value = false
            refresh(force = true)
        }
    }

    fun saveChanges(original: CollectionItem, edited: CollectionItemDraft) {
        viewModelScope.launch {
            val id = original.internalId
            var hasChanges = false

            if (edited.rating != original.rating) {
                gameCollectionRepository.updateRating(id, edited.rating)
                hasChanges = true
            }
            if (edited.comment != original.comment) {
                gameCollectionRepository.updateComment(id, edited.comment)
                hasChanges = true
            }
            if (edited.privateComment != original.privateComment) {
                gameCollectionRepository.updatePrivateComment(id, edited.privateComment)
                hasChanges = true
            }
            if (edited.wishlistComment != original.wishListComment) {
                gameCollectionRepository.updateWishlistComment(id, edited.wishlistComment)
                hasChanges = true
            }
            if (edited.conditionText != original.conditionText) {
                gameCollectionRepository.updateCondition(id, edited.conditionText)
                hasChanges = true
            }
            if (edited.wantPartsList != original.wantPartsList) {
                gameCollectionRepository.updateWantParts(id, edited.wantPartsList)
                hasChanges = true
            }
            if (edited.hasPartsList != original.hasPartsList) {
                gameCollectionRepository.updateHasParts(id, edited.hasPartsList)
                hasChanges = true
            }

            val statusChanged =
                edited.own != original.own ||
                    edited.preordered != original.preOrdered ||
                    edited.previouslyOwned != original.previouslyOwned ||
                    edited.wantToBuy != original.wantToBuy ||
                    edited.wantToPlay != original.wantToPlay ||
                    edited.forTrade != original.forTrade ||
                    edited.wantInTrade != original.wantInTrade ||
                    edited.wishlist != original.wishList ||
                    edited.wishlistPriority != original.wishListPriority

            if (statusChanged) {
                gameCollectionRepository.updateStatus(
                    internalId = id,
                    statusOwn = edited.own,
                    statusPreordered = edited.preordered,
                    statusPreviouslyOwned = edited.previouslyOwned,
                    statusForTrade = edited.forTrade,
                    statusWant = edited.wantInTrade,
                    statusWantToPlay = edited.wantToPlay,
                    statusWantToBuy = edited.wantToBuy,
                    statusWishlist = edited.wishlist,
                    statusWishlistPriority = edited.wishlistPriority,
                )
                hasChanges = true
            }

            val privateInfoChanged =
                edited.pricePaidCurrency != original.pricePaidCurrency ||
                    edited.pricePaid != original.pricePaid ||
                    edited.currentValueCurrency != original.currentValueCurrency ||
                    edited.currentValue != original.currentValue ||
                    edited.quantity != original.quantity ||
                    edited.acquisitionDate != original.acquisitionDate ||
                    edited.acquiredFrom != original.acquiredFrom ||
                    edited.inventoryLocation != original.inventoryLocation

            if (privateInfoChanged) {
                gameCollectionRepository.updatePrivateInfo(
                    internalId = id,
                    priceCurrency = edited.pricePaidCurrency,
                    price = edited.pricePaid,
                    currentValueCurrency = edited.currentValueCurrency,
                    currentValue = edited.currentValue,
                    quantity = edited.quantity,
                    acquisitionDate = edited.acquisitionDate,
                    acquiredFrom = edited.acquiredFrom,
                    inventoryLocation = edited.inventoryLocation,
                )
                hasChanges = true
            }

            isEditModeState.value = false
            if (hasChanges) {
                gameCollectionRepository.enqueueUploadRequest(original.gameId)
            }
            isEditedState.value = false
        }
    }
}

data class CollectionItemDraft(
    val own: Boolean,
    val preordered: Boolean,
    val previouslyOwned: Boolean,
    val wantToBuy: Boolean,
    val wantToPlay: Boolean,
    val forTrade: Boolean,
    val wantInTrade: Boolean,
    val wishlist: Boolean,
    val wishlistPriority: Int,
    val rating: Double,
    val comment: String,
    val privateComment: String,
    val wishlistComment: String,
    val conditionText: String,
    val wantPartsList: String,
    val hasPartsList: String,
    val pricePaidCurrency: String?,
    val pricePaid: Double?,
    val currentValueCurrency: String?,
    val currentValue: Double?,
    val quantity: Int?,
    val acquisitionDate: Long?,
    val acquiredFrom: String?,
    val inventoryLocation: String?,
)
