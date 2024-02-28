package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.SpotifyOAuth2
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.filterBySearchTerm
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

    /*
    fun addSelectedAlbums(context: Context, onFinish: (importedIds: List<UUID>) -> Unit) = launchOnIOThread {
        val selectedAlbums = selectedExternalAlbums.value
        var progressMultiplier = 0

        _progress.value = ProgressData(text = context.getString(R.string.matching_albums), progress = 0.0)

        val comboMap = selectedAlbums.associateWith { spotifyAlbum ->
            spotifyAlbum.toAlbumWithTracks(getArtist = { repos.artist.artistCache.get(it) })
        }

        addAlbumCombos(
            combos = comboMap.values,
            updateProgress = {
                _progress.value = ProgressData(
                    text = context.getString(it),
                    progress = (1.0 / 6) * progressMultiplier++,
                )
            },
        )

        _importedAlbumIds.value += comboMap.map { it.key.id to it.value.album.albumId }
        _selectedExternalAlbums.value -= selectedAlbums
        _progress.value = null

        /*
        selectedAlbums.forEach { spotifyAlbum ->
            val combo = spotifyAlbum.toAlbumWithTracks(
                isInLibrary = true,
                getArtist = { repos.artist.artistCache.get(it) },
            )
            repos.album.insertAlbumAndTags(combo)
            if (combo.trackCombos.isNotEmpty()) repos.track.insertTracks(combo.trackCombos.map { it.track })
            repos.artist.insertAlbumArtists(combo.artists.toAlbumArtists())
            repos.artist.insertTrackArtists(combo.trackCombos.flatMap { it.artists.toTrackArtists() })
            repos.artist.updateArtists(
                combo.artists.toArtists().plus(combo.trackCombos.flatMap { it.artists.toArtists() }).toSet()
            )
            _importedAlbumIds.value += spotifyAlbum.id to combo.album.albumId
            importedIds.add(combo.album.albumId)
            _selectedExternalAlbums.value -= spotifyAlbum
        }
         */

        if (comboMap.isNotEmpty()) onFinish(comboMap.values.map { it.album.albumId })
    }
     */

    fun getAuthUrl() = repos.spotify.oauth2.getAuthUrl()

    fun handleIntent(intent: Intent, context: Context) = launchOnIOThread {
        try {
            repos.spotify.oauth2.handleIntent(intent)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "handleIntent: $e", e)
            SnackbarEngine.addError(context.getString(R.string.spotify_authorization_failed, e).umlautify())
        }
    }

    override suspend fun convertExternalAlbum(
        externalAlbum: SpotifyAlbum,
        progressCallback: (Double) -> Unit,
    ): AlbumWithTracksCombo = externalAlbum.toAlbumWithTracks(getArtist = { repos.artist.artistCache.get(it) })

    override suspend fun fetchExternalAlbums(): Boolean = repos.spotify.fetchNextUserAlbums()

    override suspend fun getThumbnail(externalAlbum: SpotifyAlbum): ImageBitmap? =
        repos.spotify.getThumbnail(externalAlbum)
}
