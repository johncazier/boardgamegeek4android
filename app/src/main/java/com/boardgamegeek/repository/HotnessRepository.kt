package com.boardgamegeek.repository

import com.boardgamegeek.model.HotGame
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class HotnessRepository(
    private val api: BggService,
    private val gameRepository: GameRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getHotnessFlow(): Flow<List<HotGame>> = flow {
        // Perform the API call to get the initial list of hot games
        val response = api.getHotness(BggService.HotnessType.BOARDGAME)
        // Emit the basic hot game data without ratings first
        emit(response.games?.map { it.mapToModel() }.orEmpty())
    }.flatMapLatest { hotGamesWithoutRatings ->
        // Create a list of Flows, one for each game's details
        val gameDetailFlows = hotGamesWithoutRatings.map { hotGame ->
            gameRepository.loadGameFlow(hotGame.id).map { game ->
                // When the game details load (or update), update the HotGame model
                hotGame.copy(rating = game?.rating)
            }
        }

        // If there are no hot games, emit an empty list
        if (gameDetailFlows.isEmpty()) {
            flow { emit(emptyList()) }
        } else {
            // Combine the latest emissions from all game detail flows
            // This will re-emit the entire list of HotGames whenever any single game's details change
            combine(gameDetailFlows) { arrayOfHotGamesWithDetails ->
                arrayOfHotGamesWithDetails.toList()
            }
        }
    }.flowOn(Dispatchers.IO) // Ensure network and database operations run on the IO dispatcher
}