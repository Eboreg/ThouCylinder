package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.abstr.toArtists
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.entities.toAlbumArtistCredits
import us.huseli.thoucylinder.dataclasses.entities.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.entities.toTrackArtistCredits
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.launchOnIOThread
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditAlbumViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
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
    private val albumArtFetchJobs = mutableMapOf<UUID, List<Job>>()

    val allTags = repos.album.flowTags()
    val isLoadingAlbumArt = _isLoadingAlbumArt.asStateFlow()

    fun cancelAlbumArtFetch(albumId: UUID) =
        albumArtFetchJobs.remove(albumId)?.forEach { it.cancel() }

    fun flowAlbumArt(albumId: UUID, context: Context) =
        MutableStateFlow<Set<AlbumArt>>(emptySet()).also { flow ->
            _isLoadingAlbumArt.value = true

            launchOnIOThread {
                repos.album.getAlbumWithTracks(albumId)?.also { combo ->
                    combo.album.albumArt?.also { addAlbumArt(it, context, flow, true) }

                    val jobs = listOf(
                        launch {
                            repos.spotify.searchAlbumArt(
                                combo = combo,
                                getArtist = { repos.artist.artistCache.get(it) }
                            ).forEach { addAlbumArt(it, context, flow) }
                        },
                        launch {
                            combo.album.youtubePlaylist?.fullImage?.url?.also { addAlbumArt(it, context, flow) }
                        },
                        launch {
                            repos.localMedia.collectNewLocalAlbumArtUris(combo).forEach {
                                addAlbumArt(it, context, flow)
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
                                    ?.forEach { addAlbumArt(it.uri, context, flow) }
                            }
                        },
                        launch {
                            repos.musicBrainz.getReleaseId(combo)?.also { releaseId ->
                                repos.musicBrainz.getSiblingReleaseIds(releaseId).forEach { siblingId ->
                                    repos.musicBrainz.getAllCoverArtArchiveImages(siblingId).forEach {
                                        addAlbumArt(it.image, context, flow)
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

    fun flowAlbumWithTracks(albumId: UUID) = repos.album.flowAlbumWithTracks(albumId)

    fun saveAlbumArtFromUri(albumId: UUID, uri: Uri, context: Context, onSuccess: () -> Unit, onFail: () -> Unit) =
        launchOnIOThread {
            saveMediaStoreImage(albumId, MediaStoreImage.fromUri(uri, context), context, onSuccess, onFail)
        }

    fun saveAlbumArt(albumId: UUID, albumArt: AlbumArt?, context: Context) = launchOnIOThread {
        saveMediaStoreImage(albumId, albumArt?.mediaStoreImage, context)
    }

    fun updateAlbumCombo(
        combo: AlbumWithTracksCombo,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        tags: Collection<Tag>,
        updateMatchingTrackArtists: Boolean,
    ) = launchOnIOThread {
        val artists = artistNames.filter { it.isNotEmpty() }.map { repos.artist.artistCache.getByName(it) }
        val album = combo.album.copy(title = title, year = year)
        val albumArtists = artists.toAlbumArtistCredits(album.albumId)
        val updatedTrackArtists = mutableListOf<TrackArtist>()
        val trackCombos = combo.trackCombos.map { trackCombo ->
            trackCombo.copy(
                track = ensureTrackMetadata(trackCombo.track),
                album = album,
                artists = if (updateMatchingTrackArtists && trackCombo.artists.toArtists() == combo.artists.toArtists())
                    artists.toTrackArtistCredits(trackCombo.track.trackId)
                        .also { updatedTrackArtists.addAll(it.toTrackArtists()) }
                else trackCombo.artists,
            )
        }

        repos.settings.getAlbumDirectory(combo)
        if (updatedTrackArtists.isNotEmpty()) repos.artist.setTrackArtists(updatedTrackArtists)
        repos.artist.setAlbumArtists(artists.toAlbumArtists(album.albumId))
        repos.album.updateAlbum(album)
        repos.album.setAlbumTags(combo.album.albumId, tags)
        trackCombos.forEach { trackCombo ->
            repos.localMedia.tagTrack(trackCombo = trackCombo, albumArtists = albumArtists)
        }
    }

    fun updateTrackCombo(
        combo: TrackCombo,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        albumCombo: AbstractAlbumCombo,
    ) = launchOnIOThread {
        var trackArtists = combo.artists
        val updatedTrack = ensureTrackMetadata(combo.track.copy(title = title, year = year))

        if (artistNames.filter { it.isNotEmpty() } != combo.artists.map { it.name }) {
            val artists = artistNames.filter { it.isNotEmpty() }.map { repos.artist.artistCache.getByName(it) }

            trackArtists = artists.map { TrackArtistCredit(artist = it, trackId = combo.track.trackId) }
            repos.artist.setTrackArtists(trackArtists.toTrackArtists())
        }
        repos.track.updateTrack(updatedTrack)
        repos.localMedia.tagTrack(
            trackCombo = combo.copy(track = updatedTrack, artists = trackArtists),
            albumArtists = albumCombo.artists,
        )
    }

    private suspend fun addAlbumArt(
        url: String,
        context: Context,
        flow: MutableStateFlow<Set<AlbumArt>>,
        isCurrent: Boolean = false,
    ) = addAlbumArt(MediaStoreImage.fromUrls(url), context, flow, isCurrent)

    private suspend fun addAlbumArt(
        uri: Uri,
        context: Context,
        flow: MutableStateFlow<Set<AlbumArt>>,
        isCurrent: Boolean = false,
    ) = addAlbumArt(MediaStoreImage.fromUri(uri, context), context, flow, isCurrent)

    private suspend fun addAlbumArt(
        mediaStoreImage: MediaStoreImage,
        context: Context,
        flow: MutableStateFlow<Set<AlbumArt>>,
        isCurrent: Boolean = false,
    ) {
        mediaStoreImage.getFullImageBitmap(context)?.also {
            albumArtMutex.withLock {
                flow.value += AlbumArt(mediaStoreImage, it, isCurrent)
            }
        }
    }

    private suspend fun saveMediaStoreImage(
        albumId: UUID,
        albumArt: MediaStoreImage?,
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {},
    ) {
        val combo = repos.album.getAlbumCombo(albumId)

        if (combo != null) {
            if (albumArt != null) {
                albumArt.saveInternal(combo.album, context)?.also { mediaStoreImage ->
                    if (combo.album.isLocal) {
                        repos.settings.createAlbumDirectory(combo)?.also {
                            mediaStoreImage.saveToDirectory(context, it)
                        }
                    }
                    repos.album.updateAlbumArt(albumId, mediaStoreImage)
                    onSuccess()
                } ?: run(onFail)
            } else {
                repos.album.updateAlbumArt(albumId, null)
                onSuccess()
            }
        } else onFail()
    }
}
