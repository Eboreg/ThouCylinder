package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.filterBySearchTerm
import us.huseli.thoucylinder.dataclasses.lastFm.toMediaStoreImage
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class LastFmViewModel @Inject constructor(private val repos: Repositories) :
    AbstractImportViewModel<LastFmTopAlbumsResponse.Album>(repos) {
    override val externalAlbums: Flow<List<LastFmTopAlbumsResponse.Album>> =
        combine(repos.lastFm.topAlbums, searchTerm) { albums, term ->
            albums.filterBySearchTerm(term).filter { !pastImportedAlbumIds.contains(it.mbid) }
        }
    override val hasNext: Flow<Boolean> =
        combine(externalAlbums, repos.lastFm.allTopAlbumsFetched, localOffset) { albums, allFetched, offset ->
            (!allFetched && albums.isNotEmpty()) || albums.size >= offset + 50
        }
    override val offsetExternalAlbums: Flow<List<LastFmTopAlbumsResponse.Album>> =
        combine(externalAlbums, localOffset) { albums, offset ->
            albums.subList(min(offset, max(albums.lastIndex, 0)), min(offset + 50, albums.size))
        }
    override val totalAlbumCount: Flow<Int> = externalAlbums.map { it.size }
    override val isAllSelected: Flow<Boolean> =
        combine(offsetExternalAlbums, selectedExternalAlbumIds) { userAlbums, selectedIds ->
            userAlbums.isNotEmpty() && selectedIds.containsAll(userAlbums.map { it.id })
        }

    val username: StateFlow<String?> = repos.lastFm.username

    init {
        launchOnIOThread {
            pastImportedAlbumIds.addAll(repos.lastFm.listImportedAlbumIds())
        }
    }

    fun handleIntent(intent: Intent, context: Context) = launchOnIOThread {
        intent.data?.getQueryParameter("token")?.also { authToken ->
            try {
                repos.lastFm.fetchSession(authToken)
            } catch (e: Exception) {
                repos.lastFm.setScrobble(false)
                Log.e(javaClass.simpleName, "handleIntent: $e", e)
                SnackbarEngine.addError(context.getString(R.string.last_fm_authorization_failed, e).umlautify())
            }
        }
    }

    override suspend fun convertExternalAlbum(
        externalAlbum: LastFmTopAlbumsResponse.Album,
        progressCallback: (Double) -> Unit,
    ): AlbumWithTracksCombo? {
        val release = repos.musicBrainz.getRelease(externalAlbum.id)

        if (release != null) {
            progressCallback(0.5)
            return release.toAlbumWithTracks(
                getArtist = { repos.artist.artistCache.get(it) },
                albumArt = externalAlbum.image.toMediaStoreImage(),
            )
        }

        return null
    }

    override suspend fun fetchExternalAlbums(): Boolean = repos.lastFm.fetchNextTopAlbums()

    override suspend fun getThumbnail(externalAlbum: LastFmTopAlbumsResponse.Album) =
        repos.lastFm.getThumbnail(externalAlbum)
}
