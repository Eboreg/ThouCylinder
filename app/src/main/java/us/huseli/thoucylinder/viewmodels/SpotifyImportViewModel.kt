package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.SpotifyOAuth2
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.filterBySearchTerm
import us.huseli.thoucylinder.interfaces.SpotifyOAuth2Listener
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(private val repos: Repositories) :
    AbstractImportViewModel<SpotifyAlbum>(repos) {
    override val externalAlbums: Flow<List<SpotifyAlbum>> =
        combine(repos.spotify.userAlbums, searchTerm) { albums, term ->
            albums.filterBySearchTerm(term).filter { !pastImportedAlbumIds.contains(it.id) }
        }
    override val offsetExternalAlbums = combine(externalAlbums, localOffset) { albums, offset ->
        albums.subList(min(offset, max(albums.lastIndex, 0)), min(offset + 50, albums.size))
    }
    override val isAllSelected: Flow<Boolean> =
        combine(offsetExternalAlbums, selectedExternalAlbumIds) { userAlbums, selectedIds ->
            userAlbums.isNotEmpty() && selectedIds.containsAll(userAlbums.map { it.id })
        }
    override val totalAlbumCount: StateFlow<Int?> = repos.spotify.totalUserAlbumCount

    val authorizationStatus: Flow<SpotifyOAuth2.AuthorizationStatus> = repos.spotify.oauth2.authorizationStatus
    val filteredAlbumCount: Flow<Int?> = combine(
        searchTerm,
        externalAlbums,
        repos.spotify.totalUserAlbumCount,
    ) { term, filteredAlbums, totalCount ->
        if (term == "") totalCount?.minus(pastImportedAlbumIds.size)
        else filteredAlbums.size
    }
    val isAlbumCountExact: Flow<Boolean> = combine(searchTerm, repos.spotify.allUserAlbumsFetched) { term, allFetched ->
        term == "" || allFetched
    }

    override val hasNext: Flow<Boolean> =
        combine(filteredAlbumCount, localOffset) { total, offset -> total == null || total > offset + 50 }

    init {
        launchOnIOThread {
            pastImportedAlbumIds.addAll(repos.spotify.listImportedAlbumIds())
        }
    }

    fun getAuthUrl() = repos.spotify.oauth2.getAuthUrl()

    fun handleIntent(intent: Intent, context: Context) = launchOnIOThread {
        try {
            repos.spotify.oauth2.handleIntent(intent)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "handleIntent: $e", e)
            SnackbarEngine.addError(context.getString(R.string.spotify_authorization_failed, e).umlautify())
        }
    }

    fun registerOAuth2Listener(listener: SpotifyOAuth2Listener) = repos.spotify.registerOAuth2Listener(listener)

    override suspend fun convertExternalAlbum(
        externalAlbum: SpotifyAlbum,
        progressCallback: (Double) -> Unit,
    ): AlbumWithTracksCombo = externalAlbum.toAlbumWithTracks(getArtist = { repos.artist.artistCache.get(it) })

    override suspend fun fetchExternalAlbums(): Boolean = repos.spotify.fetchNextUserAlbums()

    override suspend fun updateArtists(artists: Iterable<Artist>) {
        artists.forEach { artist ->
            artist.spotifyId?.also { repos.artist.setArtistSpotifyId(artist.id, it) }
        }
    }

    override suspend fun getThumbnail(externalAlbum: SpotifyAlbum): ImageBitmap? =
        withContext(Dispatchers.IO) { repos.spotify.getThumbnail(externalAlbum) }
}
