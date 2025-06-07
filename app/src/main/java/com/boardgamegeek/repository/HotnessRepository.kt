package com.boardgamegeek.repository

import com.boardgamegeek.model.HotGame
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class HotnessRepository(private val api: BggService) {

    fun getHotnessFlow(): Flow<List<HotGame>> = flow {
        // Perform the API call
        val response = api.getHotness(BggService.HotnessType.BOARDGAME)

        // Map the response to your domain model
        val hotGames = response.games?.map { it.mapToModel() }.orEmpty()

        // Emit the result
        emit(hotGames)
    }.flowOn(Dispatchers.IO) // Ensure the upstream operations (API call) run on the IO dispatcher
}