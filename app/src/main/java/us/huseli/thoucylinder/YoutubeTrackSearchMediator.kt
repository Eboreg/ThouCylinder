package us.huseli.thoucylinder

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.YoutubeSearchToken
import us.huseli.thoucylinder.repositories.YoutubeRepository

@OptIn(ExperimentalPagingApi::class)
class YoutubeTrackSearchMediator(
    private val query: String,
    private val repo: YoutubeRepository,
    private val database: Database,
) : RemoteMediator<Int, Track>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Track>): MediatorResult {
        return try {
            val key = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val token = database.youtubeSearchDao().getToken(query)

                    if (token?.nextKey == null) {
                        return MediatorResult.Success(endOfPaginationReached = token != null)
                    }
                    token.nextKey
                }
            }
            val result = repo.getTrackSearchResult(query, key)

            database.withTransaction {
                database.youtubeSearchDao().insertOrReplaceToken(
                    YoutubeSearchToken(query = query, prevKey = result.token, nextKey = result.nextToken)
                )
                database.youtubeSearchDao().insertTracksForQuery(query, result.tracks)
            }
            MediatorResult.Success(endOfPaginationReached = result.nextToken == null)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "load: $e", e)
            MediatorResult.Error(e)
        }
    }
}
