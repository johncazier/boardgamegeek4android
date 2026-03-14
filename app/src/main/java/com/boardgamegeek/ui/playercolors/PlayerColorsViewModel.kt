package com.boardgamegeek.ui.playercolors

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.repository.PlayRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PlayerColorsViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : ViewModel() {
    private data class PlayerTarget(val name: String?, val type: PlayRepository.PlayerType)

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(application)
    private val user = MutableStateFlow<PlayerTarget?>(null)

    private val _colors = MutableStateFlow<List<String>>(emptyList())
    val colors: StateFlow<List<String>> = _colors.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            user.flatMapLatest { target ->
                if (target == null || target.name.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    flow {
                        val loadedColors = playRepository.loadPlayerColors(target.name, target.type)
                        emit(loadedColors.map { it.description })
                    }
                }
            }.collect { loaded ->
                _colors.value = loaded
                _isLoading.value = false
            }
        }
    }

    fun setUsername(name: String?) {
        if (user.value?.name != name || user.value?.type != PlayRepository.PlayerType.USER) {
            _isLoading.value = true
            user.value = PlayerTarget(name, PlayRepository.PlayerType.USER)
        }
    }

    fun setPlayerName(name: String?) {
        if (user.value?.name != name || user.value?.type != PlayRepository.PlayerType.NON_USER) {
            _isLoading.value = true
            user.value = PlayerTarget(name, PlayRepository.PlayerType.NON_USER)
        }
    }

    fun generate() {
        viewModelScope.launch {
            val generated = withContext(Dispatchers.Default) {
                val availableColors = BggColors.standardColorList.map { it.first }.toMutableList()
                val rankedColors = mutableListOf<String>()
                val target = user.value

                if (target != null) {
                    val playedColors = playRepository.loadPlayerUsedColors(target.name, target.type)
                    val sortedColors = playedColors.asSequence()
                        .filter { availableColors.contains(it) }
                        .groupBy { it }
                        .map { it.key to it.value.size }
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .toMutableList()
                    while (sortedColors.isNotEmpty()) {
                        val description = sortedColors.removeAt(0)
                        availableColors.remove(availableColors.find { it == description })
                        rankedColors.add(description)
                    }
                }

                if (availableColors.isNotEmpty()) {
                    rankedColors.addAll(availableColors.shuffled())
                }
                rankedColors
            }
            _colors.value = generated
            logEvent("Generate")
        }
    }

    fun clear() {
        _colors.value = emptyList()
        logEvent("Clear")
    }

    fun add(color: String, index: Int? = null) {
        val list = _colors.value.toMutableList()
        if (!list.contains(color)) {
            if (index == null || index < 0 || index > list.size) {
                list.add(color)
            } else {
                list.add(index, color)
            }
            _colors.value = list
            logEvent(if (index == null) "Add" else "AddAtIndex", color)
        }
    }

    fun remove(color: String): Int {
        val list = _colors.value.toMutableList()
        val index = list.indexOf(color)
        if (index >= 0) {
            list.removeAt(index)
            _colors.value = list
            logEvent("Delete", color)
        }
        return index
    }

    fun moveUp(index: Int) {
        if (index <= 0 || index >= _colors.value.size) return
        val list = _colors.value.toMutableList()
        val movingColor = list.removeAt(index)
        list.add(index - 1, movingColor)
        _colors.value = list
    }

    fun moveDown(index: Int) {
        if (index < 0 || index >= _colors.value.lastIndex) return
        val list = _colors.value.toMutableList()
        val movingColor = list.removeAt(index)
        list.add(index + 1, movingColor)
        _colors.value = list
    }

    fun save() {
        viewModelScope.launch {
            user.value?.let { target ->
                playRepository.savePlayerColors(target.name, target.type, colors.value)
            }
        }
    }

    private fun logEvent(action: String, color: String? = null) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
            param("Action", action)
            color?.let { param("Color", it) }
        }
    }
}
