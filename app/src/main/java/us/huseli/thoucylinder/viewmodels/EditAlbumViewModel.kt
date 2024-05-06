package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.dataclasses.uistates.EditAlbumUiState
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.abstr.toArtists
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.toAlbumArtistCredits
import us.huseli.thoucylinder.dataclasses.entities.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.entities.toTrackArtistCredits
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class EditAlbumViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    data class AlbumArt(
        val mediaStoreImage: MediaStoreImage,
        val imageBitmap: ImageBitmap,
        val isCurrent: Boolean = false,
    ) {
        override fun equals(other: Any?) = other is AlbumArt && other.mediaStoreImage == mediaStoreImage
        override fun hashCode() = mediaStoreImage.hashCode()
    }

    private val _isLoadingAlbumArt = MutableStateFlow(false)
    private val albumArtMutex = Mutex()
    private val albumArtFetchJobs = mutableMapOf<String, List<Job>>()

    val allTags = repos.album.flowTags().stateLazily(emptyList())
    val isLoadingAlbumArt = _isLoadingAlbumArt.asStateFlow()

    fun cancelAlbumArtFetch(albumId: String) {
        albumArtFetchJobs.remove(albumId)?.forEach { it.cancel() }
    }

    fun clearAlbumArt(albumId: String) {
        launchOnIOThread { managers.image.clearAlbumArt(albumId) }
    }

    fun flowAlbumArt(albumId: String, context: Context) =
        MutableStateFlow<Set<AlbumArt>>(emptySet()).also { flow ->
            _isLoadingAlbumArt.value = true

            launchOnIOThread {
                repos.album.getAlbumWithTracks(albumId)?.also { combo ->
                    combo.album.albumArt?.also { addAlbumArt(it, flow, true) }

                    val jobs = listOf(
                        launch {
                            repos.spotify.searchAlbumArt(
                                combo = combo,
                                getArtist = { repos.artist.artistCache.get(it) }
                            ).forEach { addAlbumArt(it, flow) }
                        },
                        launch {
                            combo.album.youtubePlaylist?.fullImage?.url?.toMediaStoreImage()?.also {
                                addAlbumArt(it, flow)
                            }
                        },
                        launch {
                            managers.image.collectNewLocalAlbumArtUris(combo).forEach { uri ->
                                uri.toMediaStoreImage(context)?.also { addAlbumArt(it, flow) }
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
                                    ?.forEach { image ->
                                        image.uri.toMediaStoreImage()?.also { addAlbumArt(it, flow) }
                                    }
                            }
                        },
                        launch {
                            repos.musicBrainz.getReleaseId(combo)?.also { releaseId ->
                                repos.musicBrainz.getSiblingReleaseIds(releaseId).forEach { siblingId ->
                                    repos.musicBrainz.getAllCoverArtArchiveImages(siblingId).forEach { image ->
                                        image.image.toMediaStoreImage()?.also {
                                            addAlbumArt(it, flow)
                                        }
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
        }.asStateFlow()

    fun flowAlbumWithTracks(albumId: String) =
        repos.album.flowAlbumWithTracks(albumId).distinctUntilChanged().stateLazily()

    fun getArtistNameSuggestions(name: String, limit: Int = 10) =
        repos.artist.getArtistNameSuggestions(name, limit)

    suspend fun getUiState(albumId: String): EditAlbumUiState {
        val combo = repos.album.getAlbumCombo(albumId) ?: throw Exception("Album $albumId not found")

        return EditAlbumUiState(
            albumId = albumId,
            title = combo.album.title,
            artistNames = combo.artists.map { it.name }.toImmutableList(),
            year = combo.album.year,
        )
    }

    suspend fun listTags(albumId: String): ImmutableList<Tag> = repos.album.listTags(albumId).toImmutableList()

    fun saveAlbumArtFromUri(albumId: String, uri: Uri, onSuccess: () -> Unit, onFail: () -> Unit) {
        launchOnIOThread { managers.image.saveAlbumArtFromUri(albumId, uri, onSuccess, onFail) }
    }

    fun saveAlbumArt(albumId: String, albumArt: MediaStoreImage) {
        launchOnIOThread { managers.image.saveAlbumArt(albumId, albumArt) }
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
            val artists = artistNames.filter { it.isNotEmpty() }.map { repos.artist.artistCache.getByName(it) }
            val album = combo.album.copy(title = title, year = year)
            val albumArtists = artists.toAlbumArtistCredits(album.albumId)

            repos.artist.setAlbumArtists(album.albumId, artists.toAlbumArtists(album.albumId))
            repos.album.updateAlbum(album)
            repos.album.setAlbumTags(combo.album.albumId, tags)

            if (updateMatchingTrackArtists) {
                for (trackCombo in combo.trackCombos) {
                    val trackArtists =
                        if (trackCombo.artists.toArtists() == combo.artists.toArtists())
                            artists.toTrackArtistCredits(trackCombo.track.trackId).also {
                                repos.artist.setTrackArtists(trackCombo.track.trackId, it.toTrackArtists())
                            }
                        else trackCombo.artists

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

    fun updateTrackCombo(
        combo: TrackCombo,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        albumCombo: AbstractAlbumCombo,
    ) {
        launchOnIOThread {
            managers.library.updateTrack(
                track = combo.track,
                trackArtists = combo.artists,
                title = title,
                year = year,
                artistNames = artistNames,
                albumCombo = albumCombo,
            )
        }
    }

    private suspend fun addAlbumArt(
        mediaStoreImage: MediaStoreImage,
        flow: MutableStateFlow<Set<AlbumArt>>,
        isCurrent: Boolean = false,
    ) {
        managers.image.getFullImageBitmap(mediaStoreImage.fullUri)?.also {
            albumArtMutex.withLock { flow.value += AlbumArt(mediaStoreImage, it, isCurrent) }
        }
    }
}
