package us.huseli.thoucylinder.managers

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.album.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.ImportableAlbumUiState
import us.huseli.thoucylinder.dataclasses.album.TrackMergeStrategy
import us.huseli.thoucylinder.externalcontent.IExternalSearchBackend
import us.huseli.thoucylinder.externalcontent.ImportBackend
import us.huseli.thoucylinder.externalcontent.LastFmBackend
import us.huseli.thoucylinder.externalcontent.LocalBackend
import us.huseli.thoucylinder.externalcontent.MusicBrainzBackend
import us.huseli.thoucylinder.externalcontent.SearchBackend
import us.huseli.thoucylinder.externalcontent.SpotifyBackend
import us.huseli.thoucylinder.externalcontent.YoutubeBackend
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ExternalContentManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
    private val database: Database,
) : AbstractScopeHolder(), ILogger {
    data class AlbumImportData(
        val state: ImportableAlbumUiState,
        val matchYoutube: Boolean,
        val isFinished: Boolean = false,
        val holder: AbstractAlbumImportHolder<out IExternalAlbum>,
        val error: String? = null,
    )

    inner class Backends {
        val spotify = SpotifyBackend(repos)
        val musicBrainz = MusicBrainzBackend(repos)
        val youtube = YoutubeBackend(repos)
        val lastFm = LastFmBackend(repos)
        val local = LocalBackend(repos, context)
    }

    interface Callback {
        fun onAlbumImportFinished(data: List<AlbumImportData>)
    }

    private val backends = Backends()
    private val callbacks = mutableListOf<Callback>()
    private val albumImportQueue = MutableStateFlow<List<AlbumImportData>>(emptyList())
    private val nextAlbumImport = albumImportQueue
        .map { dataList -> dataList.firstOrNull { !it.isFinished } }
        .filterNotNull()
        .distinctUntilChanged()
    private val currentAlbumImportProgress = MutableStateFlow<Double>(0.0)
    private val isAlbumImportActive = MutableStateFlow(false)
    private val albumImportProgressText = MutableStateFlow("")
    private val totalAlbumImportProgress =
        combine(albumImportQueue, currentAlbumImportProgress) { queue, currentProgress ->
            if (queue.isEmpty()) 0.0
            else (queue.filter { it.isFinished }.size.toDouble() / queue.size) + (currentProgress / queue.size)
        }

    val albumImportProgress =
        combine(isAlbumImportActive, albumImportProgressText, totalAlbumImportProgress) { isActive, text, progress ->
            ProgressData(text = text, progress = progress, isActive = isActive)
        }

    init {
        launchOnIOThread {
            nextAlbumImport.collect { data ->
                var error: String? = null

                isAlbumImportActive.value = true
                albumImportProgressText.value = context.getString(
                    if (data.matchYoutube) R.string.matching_x else R.string.importing_x,
                    data.state.title,
                )

                val combo = try {
                    convertStateToAlbumWithTracks(data = data)
                } catch (e: Throwable) {
                    error = e.toString()
                    null
                }?.let {
                    albumImportProgressText.value = context.getString(R.string.importing_x, it.album.title)
                    currentAlbumImportProgress.value = 0.9
                    upsertAlbumWithTracks(it)
                }

                if (combo != null) {
                    data.holder.onItemImportFinished(data.state.id)
                    updateAlbumComboFromRemotes(combo)
                } else {
                    error = error ?: context.getString(R.string.no_match_found)
                    data.holder.onItemImportError(itemId = data.state.id, error = error)
                }

                albumImportQueue.value = albumImportQueue.value.toMutableList().apply {
                    val index = indexOfFirst { it.state.id == data.state.id }
                    if (index > -1) this[index] = data.copy(isFinished = true, error = error)
                }
            }
        }

        launchOnIOThread {
            albumImportQueue.collect { queue ->
                if (queue.isNotEmpty() && queue.all { it.isFinished }) {
                    isAlbumImportActive.value = false
                    for (callback in callbacks) {
                        callback.onAlbumImportFinished(queue)
                    }
                    albumImportQueue.value = emptyList()
                }
            }
        }
    }

    fun addCallback(callback: Callback) {
        if (!callbacks.contains(callback)) callbacks.add(callback)
    }

    fun enqueueAlbumImport(
        state: ImportableAlbumUiState,
        holder: AbstractAlbumImportHolder<out IExternalAlbum>,
        matchYoutube: Boolean = true,
    ) {
        val data = AlbumImportData(state = state, holder = holder, matchYoutube = matchYoutube)
        albumImportQueue.value = albumImportQueue.value.plus(data)
    }

    fun getImportBackend(key: ImportBackend) = when (key) {
        ImportBackend.LOCAL -> backends.local
        ImportBackend.SPOTIFY -> backends.spotify
        ImportBackend.LAST_FM -> backends.lastFm
    }

    fun getSearchBackend(key: SearchBackend): IExternalSearchBackend<out IExternalAlbum> = when (key) {
        SearchBackend.YOUTUBE -> backends.youtube
        SearchBackend.SPOTIFY -> backends.spotify
        SearchBackend.MUSICBRAINZ -> backends.musicBrainz
    }

    fun setLocalImportUri(uri: Uri) = backends.local.setLocalImportUri(uri)


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun convertStateToAlbumWithTracks(data: AlbumImportData): IAlbumWithTracksCombo<IAlbum>? {
        val matchedCombo = data.holder.convertToAlbumWithTracks(data.state)

        if (!data.matchYoutube) return matchedCombo
        if (matchedCombo == null) return null

        currentAlbumImportProgress.value = 0.0

        val youtubeMatch = repos.youtube.getBestAlbumMatch(
            combo = matchedCombo,
            progressCallback = { currentAlbumImportProgress.value = it * 0.8 },
        ) ?: return null

        // If imported & converted album already exists, use that instead:
        repos.album.getAlbumWithTracksByPlaylistId(youtubeMatch.playlistCombo.playlist.id)?.let { combo ->
            combo.copy(
                album = combo.album.copy(isInLibrary = true, isHidden = false),
                trackCombos = combo.trackCombos.map { trackCombo ->
                    trackCombo.copy(track = trackCombo.track.copy(isInLibrary = true))
                },
            )
        }?.also { return it }

        return youtubeMatch.albumCombo
    }

    private fun updateAlbumComboFromRemotes(combo: IAlbumWithTracksCombo<IAlbum>) = launchOnIOThread {
        val spotifyCombo = if (combo.album.spotifyId == null) repos.spotify.matchAlbumWithTracks(
            combo = combo,
            trackMergeStrategy = TrackMergeStrategy.KEEP_SELF,
        ) else null
        val mbCombo = (spotifyCombo ?: combo).let {
            if (it.album.musicBrainzReleaseId == null) repos.musicBrainz.matchAlbumWithTracks(
                combo = it,
                trackMergeStrategy = TrackMergeStrategy.KEEP_SELF,
            ) else null
        }

        upsertAlbumWithTracks(mbCombo ?: spotifyCombo ?: combo)
    }

    private suspend fun upsertAlbumWithTracks(combo: IAlbumWithTracksCombo<IAlbum>): AlbumWithTracksCombo? {
        return database.withTransaction {
            repos.album.upsertAlbumCombo(combo)
            repos.track.setAlbumComboTracks(combo)
            repos.artist.setAlbumComboArtists(combo)

            return@withTransaction repos.album.getAlbumWithTracks(combo.album.albumId)
        }
    }
}
