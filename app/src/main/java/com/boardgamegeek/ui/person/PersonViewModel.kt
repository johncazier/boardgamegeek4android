package com.boardgamegeek.ui.person

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.mappers.mapToPerson
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Person
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository
import com.boardgamegeek.repository.PublisherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val application: Application,
    private val artistRepository: ArtistRepository,
    private val designerRepository: DesignerRepository,
    private val publisherRepository: PublisherRepository,
) : ViewModel() {
    private data class PersonInfo(
        val type: PersonType = PersonType.DESIGNER,
        val id: Int = BggContract.INVALID_ID,
        val sort: CollectionItem.SortType = CollectionItem.SortType.RATING,
    )

    private val personInfo = MutableStateFlow(PersonInfo())

    val type: StateFlow<PersonType> = personInfo
        .map { it.type }
        .stateInWhileSubscribed(viewModelScope, PersonType.DESIGNER)

    val id: StateFlow<Int> = personInfo
        .map { it.id }
        .stateInWhileSubscribed(viewModelScope, BggContract.INVALID_ID)

    val collectionSort: StateFlow<CollectionItem.SortType> = personInfo
        .map { it.sort }
        .stateInWhileSubscribed(viewModelScope, CollectionItem.SortType.RATING)

    val details: StateFlow<RefreshableResource<Person>?> = personInfo
        .flatMapLatest { person ->
            if (person.id == BggContract.INVALID_ID) {
                flow { emit(RefreshableResource.success(null)) }
            } else {
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadArtistFlow(person.id)
                        .distinctUntilChanged()
                        .map { RefreshableResource.success(it) }
                    PersonType.DESIGNER -> designerRepository.loadDesignerFlow(person.id)
                        .distinctUntilChanged()
                        .map { RefreshableResource.success(it) }
                    PersonType.PUBLISHER -> publisherRepository.loadPublisherFlow(person.id)
                        .distinctUntilChanged()
                        .map { RefreshableResource.success(it?.mapToPerson()) }
                }
                    .onStart { emit(RefreshableResource.refreshing(null)) }
                    .catch { emit(RefreshableResource.error(it, application)) }
            }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val collection: StateFlow<List<CollectionItem>?> = personInfo
        .flatMapLatest { person ->
            if (person.id == BggContract.INVALID_ID) {
                flow { emit(emptyList()) }
            } else {
                when (person.type) {
                    PersonType.ARTIST -> artistRepository.loadCollectionFlow(person.id, person.sort)
                    PersonType.DESIGNER -> designerRepository.loadCollectionFlow(person.id, person.sort)
                    PersonType.PUBLISHER -> publisherRepository.loadCollectionFlow(person.id, person.sort)
                }
            }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val stats: StateFlow<PersonStats?> = personInfo
        .flatMapLatest { person ->
            if (person.id == BggContract.INVALID_ID) {
                emptyFlow()
            } else {
                flow {
                    emit(
                        when (person.type) {
                            PersonType.ARTIST -> artistRepository.calculateStats(person.id)
                            PersonType.DESIGNER -> designerRepository.calculateStats(person.id)
                            PersonType.PUBLISHER -> publisherRepository.calculateStats(person.id)
                        }
                    )
                }
            }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    fun setPerson(personType: PersonType, personId: Int) {
        personInfo.update { current ->
            if (current.type == personType && current.id == personId) current
            else current.copy(type = personType, id = personId)
        }
    }

    fun sort(sortType: CollectionItem.SortType) {
        personInfo.update { current ->
            if (current.sort == sortType) current else current.copy(sort = sortType)
        }
    }

    fun refresh() {
        val current = personInfo.value
        if (current.id == BggContract.INVALID_ID) return
        viewModelScope.launch {
            when (current.type) {
                PersonType.ARTIST -> artistRepository.refreshArtist(current.id)
                PersonType.DESIGNER -> designerRepository.refreshDesigner(current.id)
                PersonType.PUBLISHER -> publisherRepository.refreshPublisher(current.id)
            }
        }
    }
}
