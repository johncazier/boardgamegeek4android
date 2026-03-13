package com.boardgamegeek.ui.forums

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ForumsViewModel @Inject constructor(
    private val application: Application,
    private val repository: ForumRepository,
) : ViewModel() {
    private enum class ForumType {
        GAME,
        REGION,
        PERSON,
        COMPANY,
    }

    private val _forums = MutableStateFlow<RefreshableResource<List<Forum>>?>(null)
    val forums: StateFlow<RefreshableResource<List<Forum>>?> = _forums

    fun setRegion() {
        loadForums(ForumType.REGION, BggService.ForumRegion.BOARDGAME.id)
    }

    fun setGameId(gameId: Int) {
        loadForums(ForumType.GAME, gameId)
    }

    fun setPersonId(personId: Int) {
        loadForums(ForumType.PERSON, personId)
    }

    fun setCompanyId(companyId: Int) {
        loadForums(ForumType.COMPANY, companyId)
    }

    private fun loadForums(type: ForumType, id: Int) {
        viewModelScope.launch {
            _forums.value = RefreshableResource.refreshing(_forums.value?.data)
            _forums.value = try {
                if (id == BggContract.INVALID_ID) {
                    RefreshableResource.error("Invalid ID!")
                } else {
                    RefreshableResource.success(
                        when (type) {
                            ForumType.REGION -> repository.loadForRegion()
                            ForumType.COMPANY -> repository.loadForCompany(id)
                            ForumType.GAME -> repository.loadForGame(id)
                            ForumType.PERSON -> repository.loadForPerson(id)
                        }
                    )
                }
            } catch (e: Exception) {
                RefreshableResource.error(e, application)
            }
        }
    }
}
