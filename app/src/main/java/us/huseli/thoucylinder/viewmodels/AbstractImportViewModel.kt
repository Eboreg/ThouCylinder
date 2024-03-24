package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.abstr.toArtists
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.views.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.launchOnIOThread
import java.util.UUID

abstract class AbstractImportViewModel<A : IExternalAlbum>(private val repos: Repositories) :
    AbstractBaseViewModel(repos) {
    private val _importedAlbumIds = MutableStateFlow<Map<String, UUID>>(emptyMap())
    private val _isSearching = MutableStateFlow(false)
    private val _localOffset = MutableStateFlow(0)
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _progress = MutableStateFlow<ProgressData?>(null)
    private val _searchTerm = MutableStateFlow("")
    private val _selectedExternalAlbumIds = MutableStateFlow<List<String>>(emptyList())

    protected val pastImportedAlbumIds = mutableSetOf<String>()

    val importedAlbumIds = _importedAlbumIds.asStateFlow()
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val localOffset: StateFlow<Int> = _localOffset.asStateFlow()
    val notFoundAlbumIds: StateFlow<List<String>> = _notFoundAlbumIds.asStateFlow()
    val progress: StateFlow<ProgressData?> = _progress.asStateFlow()
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()
    val selectedExternalAlbumIds = _selectedExternalAlbumIds.asStateFlow()

    abstract val externalAlbums: Flow<List<A>>
    abstract val hasNext: Flow<Boolean>
    abstract val isAllSelected: Flow<Boolean>
    abstract val offsetExternalAlbums: Flow<List<A>>
    abstract val totalAlbumCount: Flow<Int?>

    abstract suspend fun convertExternalAlbum(
        externalAlbum: A,
        progressCallback: (Double) -> Unit,
    ): AlbumWithTracksCombo?

    abstract suspend fun fetchExternalAlbums(): Boolean

    abstract suspend fun getThumbnail(externalAlbum: A): ImageBitmap?

    abstract suspend fun updateArtists(artists: Iterable<Artist>)

    fun importSelectedAlbums(
        matchYoutube: Boolean,
        context: Context,
        onFinish: (importedIds: List<UUID>, notFoundCount: Int) -> Unit,
    ) = launchOnIOThread {
        var notFoundCount = 0
        val importedIds = mutableListOf<UUID>()
        val selectedExternalAlbums =
            _selectedExternalAlbumIds.value.mapNotNull { id -> externalAlbums.first().find { it.id == id } }

        selectedExternalAlbums.forEachIndexed { index, externalAlbum ->
            val progressBaseline = index.toDouble() / selectedExternalAlbums.size
            val progressMultiplier = 1.0 / selectedExternalAlbums.size

            updateProgress(
                progress = progressBaseline,
                text = context.getString(
                    if (matchYoutube) R.string.matching_x else R.string.importing_x,
                    externalAlbum.title,
                ),
            )

            val combo = convertExternalAlbum(
                externalAlbum = externalAlbum,
                matchYoutube = matchYoutube,
                progressCallback = { progress ->
                    updateProgress(progress = progressBaseline + (progress * progressMultiplier * 0.8))
                },
            )

            if (combo != null) {
                updateProgress(
                    text = context.getString(R.string.importing_x, combo.album.title),
                    progress = progressBaseline + (progressMultiplier * 0.9),
                )
                insertAlbumCombo(combo = combo)
                _importedAlbumIds.value += externalAlbum.id to combo.album.albumId
                importedIds.add(combo.album.albumId)
            } else {
                notFoundCount++
                _notFoundAlbumIds.value += externalAlbum.id
            }

            _selectedExternalAlbumIds.value -= externalAlbum.id
        }

        _progress.value = null
        if (selectedExternalAlbums.isNotEmpty()) onFinish(importedIds, notFoundCount)
    }

    fun selectFromLastSelected(toId: String, allIds: List<String>) {
        val ids = _selectedExternalAlbumIds.value.lastOrNull()
            ?.let { allIds.listItemsBetween(it, toId).plus(toId) }
            ?: listOf(toId)

        _selectedExternalAlbumIds.value += ids
    }

    fun setOffset(offset: Int) {
        _localOffset.value = offset
        _selectedExternalAlbumIds.value = emptyList()
        launchOnIOThread { fetchExternalAlbumsIfNeeded() }
    }

    fun setSearchTerm(value: String) {
        if (value != _searchTerm.value) {
            _searchTerm.value = value
            setOffset(0)
        }
    }

    fun setSelectAll(value: Boolean) = launchOnIOThread {
        if (value) _selectedExternalAlbumIds.value = offsetExternalAlbums.first().filter {
            !_importedAlbumIds.value.containsKey(it.id) && !_notFoundAlbumIds.value.contains(it.id)
        }.map { it.id }
        else _selectedExternalAlbumIds.value = emptyList()
    }

    fun toggleSelected(externalAlbum: A) {
        if (_selectedExternalAlbumIds.value.contains(externalAlbum.id))
            _selectedExternalAlbumIds.value -= externalAlbum.id
        else if (
            !_importedAlbumIds.value.containsKey(externalAlbum.id) &&
            !_notFoundAlbumIds.value.contains(externalAlbum.id)
        ) _selectedExternalAlbumIds.value += externalAlbum.id
    }


    /** PRIVATE METHODS *******************************************************/
    private suspend fun convertExternalAlbum(
        externalAlbum: A,
        matchYoutube: Boolean,
        progressCallback: (Double) -> Unit,
    ): AlbumWithTracksCombo? {
        val matchedCombo = convertExternalAlbum(
            externalAlbum = externalAlbum,
            progressCallback = { progressCallback(if (matchYoutube) it * 0.5 else it) },
        ) ?: return null
        if (!matchYoutube) return matchedCombo

        val youtubeMatch = repos.youtube.getBestAlbumMatch(
            combo = matchedCombo,
            progressCallback = { progressCallback(0.5 + (it * 0.5)) },
        ) ?: return null
        val existingYoutubeCombo = repos.album.getAlbumWithTracksByPlaylistId(youtubeMatch.playlistCombo.playlist.id)

        return existingYoutubeCombo ?: youtubeMatch.albumCombo
    }

    private suspend fun fetchExternalAlbumsIfNeeded() {
        while (externalAlbums.first().size <= localOffset.value + 100) {
            _isSearching.value = true
            if (!fetchExternalAlbums()) break
        }
        _isSearching.value = false
    }

    private suspend fun insertAlbumCombo(combo: AlbumWithTracksCombo) {
        repos.album.upsertAlbumAndTags(combo)
        repos.track.upsertTracks(combo.trackCombos.map { it.track })
        repos.artist.insertAlbumArtists(combo.artists.toAlbumArtists())
        repos.artist.insertTrackArtists(combo.trackCombos.flatMap { it.artists.toTrackArtists() })
        updateArtists(
            combo.artists.toArtists()
                .plus(combo.trackCombos.flatMap { it.artists.toArtists() })
                .toSet()
        )
    }

    private fun updateProgress(text: String? = null, progress: Double? = null) {
        _progress.value?.also {
            _progress.value = ProgressData(text = text ?: it.text, progress = progress ?: it.progress)
        } ?: run {
            if (text != null && progress != null) {
                _progress.value = ProgressData(text = text, progress = progress)
            }
        }
    }
}
