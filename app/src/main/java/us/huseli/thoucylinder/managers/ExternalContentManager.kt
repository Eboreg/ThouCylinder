package us.huseli.thoucylinder.managers

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
import us.huseli.thoucylinder.getUmlautifiedString
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton


@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ExternalContentManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
    private val database: Database,
    private val notificationManager: NotificationManager,
) : AbstractScopeHolder(), ILogger {
    data class AlbumImportData(
        val state: ImportableAlbumUiState,
        val matchYoutube: Boolean,
        val isFinished: Boolean = false,
        val holder: AbstractAlbumImportHolder<out IExternalAlbum>,
        var id: String = state.id,
        val error: String? = null,
    ) {
        val progress = MutableStateFlow(if (isFinished) 1.0 else 0.0)
    }

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
    private val isAlbumImportActive = MutableStateFlow(false)
    private val albumImportProgressText = MutableStateFlow("")
    private val totalAlbumImportProgress = albumImportQueue.flatMapLatest { queue ->
        combine(queue.map { it.progress }) { progresses ->
            if (progresses.isEmpty()) 0.0
            else progresses.average()
        }
    }

    val albumImportProgress =
        combine(isAlbumImportActive, albumImportProgressText, totalAlbumImportProgress) { isActive, text, progress ->
            ProgressData(text = text, progress = progress, isActive = isActive)
        }

    init {
        launchOnIOThread {
            nextAlbumImport.collect { data ->
                importAlbum(data)
                data.progress.value = 1.0
            }
        }

        launchOnIOThread {
            albumImportProgress.collect { progress ->
                if (progress.isActive) {
                    notificationManager.showNotification(
                        id = NotificationManager.ID_IMPORT_ALBUMS,
                        title = context.getUmlautifiedString(R.string.importing_albums),
                        text = progress.text,
                        ongoing = true,
                        progress = progress.progress,
                    )
                } else notificationManager.cancelNotification(NotificationManager.ID_IMPORT_ALBUMS)
            }
        }

        launchOnIOThread {
            albumImportQueue.collect { queue ->
                if (queue.all { it.isFinished }) {
                    isAlbumImportActive.value = false
                    notificationManager.cancelNotification(NotificationManager.ID_IMPORT_ALBUMS)

                    if (queue.isNotEmpty()) {
                        for (callback in callbacks) {
                            callback.onAlbumImportFinished(queue)
                        }
                        albumImportQueue.value = emptyList()
                    }
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
        albumImportQueue.value = albumImportQueue.value.plus(
            AlbumImportData(state = state, holder = holder, matchYoutube = matchYoutube)
        )
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

        val youtubeMatch = repos.youtube.getBestAlbumMatch(
            combo = matchedCombo,
            progressCallback = { data.progress.value = it * 0.9 },
        ) ?: return null

        // If imported & converted album already exists, use that instead:
        repos.album.getAlbumWithTracksByPlaylistId(youtubeMatch.playlistId)?.let { combo ->
            combo.copy(
                album = combo.album.copy(isInLibrary = true, isHidden = false),
                trackCombos = combo.trackCombos.map { trackCombo ->
                    trackCombo.copy(track = trackCombo.track.copy(isInLibrary = true))
                },
            )
        }?.also {
            data.holder.updateItemId(data.id, it.id)
            data.id = it.id
            return it
        }

        return youtubeMatch.albumCombo
    }

    private suspend fun importAlbum(data: AlbumImportData) {
        var error: String? = null

        isAlbumImportActive.value = true
        albumImportProgressText.value = context.getUmlautifiedString(
            if (data.matchYoutube) R.string.matching_x else R.string.importing_x,
            data.state.title,
        )

        val combo = try {
            convertStateToAlbumWithTracks(data = data)
        } catch (e: Throwable) {
            error = e.toString()
            null
        }?.let {
            albumImportProgressText.value = context.getUmlautifiedString(R.string.importing_x, it.album.title)
            data.progress.value = 0.9
            upsertAlbumWithTracks(it)
        }

        if (combo != null) {
            data.holder.onItemImportFinished(data.id)
            updateAlbumComboFromRemotes(combo)
        } else {
            error = error ?: context.getString(R.string.no_match_found)
            data.holder.onItemImportError(itemId = data.id, error = error)
        }

        albumImportQueue.value.indexOfFirst { it.id == data.id }.takeIf { it >= 0 }?.also { index ->
            albumImportQueue.value = albumImportQueue.value.toMutableList().apply {
                this[index] = data.copy(isFinished = true, error = error)
            }
        }
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
