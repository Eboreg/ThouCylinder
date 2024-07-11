package us.huseli.thoucylinder.viewmodels

import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.EditAlbumUiState
import us.huseli.thoucylinder.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.UnsavedTrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.tag.Tag
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditAlbumViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    data class AlbumArt(
        val mediaStoreImage: MediaStoreImage,
        val bitmap: Bitmap,
        val isCurrent: Boolean = false,
    ) {
        override fun equals(other: Any?) = other is AlbumArt && other.mediaStoreImage == mediaStoreImage
        override fun hashCode() = mediaStoreImage.hashCode()
    }

    private val _albumId = MutableStateFlow<String?>(null)
    private val _isLoadingAlbumArt = MutableStateFlow(false)
    private val albumArtFetchJobs = mutableMapOf<String, List<Job>>()

    val allTags = repos.album.flowTags().stateLazily(emptyList())
    val isLoadingAlbumArt = _isLoadingAlbumArt.asStateFlow()

    val uiState2: StateFlow<EditAlbumUiState?> = _albumId.filterNotNull().flatMapMerge { albumId ->
        repos.album.flowAlbumCombo(albumId)
    }.filterNotNull().map { combo ->
        EditAlbumUiState(
            albumId = combo.album.albumId,
            title = combo.album.title,
            artistNames = combo.artists.map { it.name }.toImmutableList(),
            year = combo.album.year,
            artistString = combo.artists.joined(),
        )
    }.stateLazily()

    val uiState: StateFlow<EditAlbumUiState?> = _albumId.filterNotNull().map { albumId ->
        repos.album.getAlbumCombo(albumId)?.let { combo ->
            EditAlbumUiState(
                albumId = combo.album.albumId,
                title = combo.album.title,
                artistNames = combo.artists.map { it.name }.toImmutableList(),
                year = combo.album.year,
                artistString = combo.artists.joined(),
            )
        }
    }.stateLazily()

    val trackUiStates: StateFlow<ImmutableList<TrackUiState>> = _albumId.filterNotNull().map { albumId ->
        repos.track.listTrackCombosByAlbumId(albumId).map { it.toUiState() }.toImmutableList()
    }.stateLazily(persistentListOf())

    fun cancelAlbumArtFetch(albumId: String) {
        albumArtFetchJobs.remove(albumId)?.forEach { it.cancel() }
    }

    fun clearAlbumArt(albumId: String) {
        launchOnIOThread { repos.album.clearAlbumArt(albumId) }
    }

    fun flowAlbumArt(albumId: String): StateFlow<List<AlbumArt>> =
        MutableStateFlow<Set<AlbumArt?>>(emptySet()).also { flow ->
            _isLoadingAlbumArt.value = true

            launchOnIOThread {
                repos.album.getAlbumWithTracks(albumId)?.also { combo ->
                    combo.album.albumArt?.also { flow.value += getAlbumArt(it, true) }

                    val jobs = listOf(
                        launch {
                            repos.spotify.searchAlbumArt(combo.album, combo.artists.joined())
                                .forEach { flow.value += getAlbumArt(it) }
                        },
                        launch {
                            combo.album.youtubePlaylist?.fullImage?.url?.toMediaStoreImage()?.also {
                                flow.value += getAlbumArt(it)
                            }
                        },
                        launch {
                            repos.image.collectNewLocalAlbumArtUris(combo).forEach { uri ->
                                flow.value += getAlbumArt(uri.toMediaStoreImage())
                            }
                        },
                        launch {
                            val response = repos.discogs.searchMasters(
                                query = combo.album.title,
                                artist = combo.artists.joined(),
                            )

                            response?.data?.results?.forEach { item ->
                                repos.discogs.getMaster(item.id)?.data?.images
                                    ?.filter { image -> image.type == "primary" }
                                    ?.forEach { image -> flow.value += getAlbumArt(image.uri.toMediaStoreImage()) }
                            }
                        },
                        launch {
                            val releaseId = combo.album.musicBrainzReleaseId ?: repos.musicBrainz.getReleaseId(combo)

                            if (releaseId != null) {
                                repos.musicBrainz.getSiblingReleaseIds(releaseId).forEach { siblingId ->
                                    repos.musicBrainz.listAllReleaseCoverArt(siblingId)?.forEach { image ->
                                        flow.value += getAlbumArt(image.image.toMediaStoreImage())
                                    }
                                }
                            }
                        },
                    )

                    albumArtFetchJobs[combo.album.albumId] = jobs
                    jobs.joinAll()
                    _isLoadingAlbumArt.value = false
                    albumArtFetchJobs.remove(combo.album.albumId)
                }
            }
        }.map { it.filterNotNull() }.stateLazily(emptyList())

    fun getArtistNameSuggestions(name: String, limit: Int = 10) =
        repos.artist.getArtistNameSuggestions(name, limit)

    suspend fun listTags(albumId: String): ImmutableList<Tag> = repos.album.listTags(albumId).toImmutableList()

    fun saveAlbumArt(albumId: String, albumArt: MediaStoreImage) {
        launchOnIOThread { repos.album.updateAlbumArt(albumId, albumArt) }
    }

    fun saveAlbumArtFromUri(albumId: String, uri: Uri, onSuccess: () -> Unit) {
        launchOnIOThread {
            try {
                val albumArt = uri.toMediaStoreImage()

                repos.image.getFullBitmap(uri) // Just to test so it doesn't fail.
                repos.album.updateAlbumArt(albumId, albumArt)
                repos.message.onSaveAlbumArtFromUri(true)
                onSuccess()
            } catch (e: Throwable) {
                repos.message.onSaveAlbumArtFromUri(false)
            }
        }
    }

    fun setAlbumId(value: String) {
        _albumId.value = value
    }

    fun updateAlbum(
        albumId: String,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        tags: Collection<Tag>,
        updateMatchingTrackArtists: Boolean,
    ) {
        launchOnIOThread {
            val combo = repos.album.getAlbumWithTracks(albumId) ?: throw Exception("Album $albumId not found")
            val album = combo.album.copy(title = title, year = year)
            val albumArtists = artistNames
                .filter { it.isNotEmpty() }
                .mapIndexed { index, name ->
                    UnsavedAlbumArtistCredit(name = name, albumId = albumId, position = index)
                }

            repos.artist.setAlbumArtists(album.albumId, albumArtists)
            repos.album.upsertAlbum(album)
            repos.album.setAlbumTags(combo.album.albumId, tags)

            if (updateMatchingTrackArtists) {
                for (trackCombo in combo.trackCombos) {
                    val trackArtists =
                        if (trackCombo.trackArtists.map { it.name } == combo.artists.map { it.name }) {
                            albumArtists.map {
                                UnsavedTrackArtistCredit(
                                    name = it.name,
                                    trackId = trackCombo.track.trackId,
                                    position = it.position,
                                )
                            }.also { repos.artist.setTrackArtists(trackCombo.track.trackId, it) }
                        } else trackCombo.trackArtists

                    repos.localMedia.tagTrack(
                        track = managers.library.ensureTrackMetadata(trackCombo.track),
                        album = album,
                        trackArtists = trackArtists,
                        albumArtists = albumArtists,
                    )
                }
            }
        }
    }

    fun updateTrack(
        trackId: String,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
    ) {
        launchOnIOThread {
            managers.library.updateTrack(
                trackId = trackId,
                title = title,
                year = year,
                artistNames = artistNames,
            )
        }
    }

    private suspend fun getAlbumArt(
        mediaStoreImage: MediaStoreImage,
        isCurrent: Boolean = false,
    ) = repos.image.getFullBitmap(mediaStoreImage.fullUri)?.let {
        AlbumArt(mediaStoreImage, it, isCurrent)
    }
}
