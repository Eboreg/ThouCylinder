package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.launchOnIOThread

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    val totalAreaSize: Flow<DpSize> =
        combine(repos.settings.contentAreaSize, repos.settings.innerPadding) { size, padding -> // including menu
            size.plus(
                DpSize(
                    padding.calculateLeftPadding(LayoutDirection.Ltr) + padding.calculateRightPadding(LayoutDirection.Ltr),
                    padding.calculateTopPadding() + padding.calculateBottomPadding(),
                )
            )
        }

    suspend fun ensureTrackMetadata(track: Track, forceReload: Boolean = false, commit: Boolean = true): Track =
        repos.youtube.ensureTrackMetadata(track, forceReload) { if (commit) repos.track.updateTrack(it) }

    fun ensureTrackMetadataAsync(track: Track, forceReload: Boolean = false) = launchOnIOThread {
        ensureTrackMetadata(track, forceReload)
    }

    suspend fun getAlbumThumbnail(albumCombo: AbstractAlbumCombo, context: Context): ImageBitmap? {
        val albumArt = albumCombo.album.albumArt

        if (albumArt != null && albumCombo.album.isLocal && !albumArt.isLocal) launchOnIOThread {
            repos.localMedia.saveInternalAlbumArtFiles(albumArt, albumCombo.album)?.also { localAlbumArt ->
                repos.settings.getAlbumDirectory(albumCombo)?.also { albumDirectory ->
                    repos.localMedia.saveAlbumDirectoryAlbumArtFiles(localAlbumArt, albumDirectory)
                }
                repos.album.updateAlbumArt(albumCombo.album.albumId, localAlbumArt)
            }
        }

        return albumArt?.getThumbnailImageBitmap(context)
    }

    fun getArtistNameSuggestions(name: String, limit: Int = 10) =
        repos.artist.getArtistNameSuggestions(name, limit)

    suspend fun getTrackThumbnail(trackCombo: AbstractTrackCombo, context: Context): ImageBitmap? =
        trackCombo.track.image?.getThumbnailImageBitmap(context)
            ?: trackCombo.album?.albumArt?.getThumbnailImageBitmap(context)

    fun importNewLocalAlbumsAsync(context: Context) = launchOnIOThread { importNewLocalAlbums(context) }

    fun saveTrack(track: Track) = launchOnIOThread { repos.track.updateTrack(track) }

    suspend fun importNewLocalAlbums(context: Context) {
        if (!repos.localMedia.isImportingLocalMedia.value) {
            val localMusicDirectory =
                repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

            if (localMusicDirectory != null) {
                repos.localMedia.setIsImporting(true)

                repos.localMedia.importNewLocalAlbums(
                    treeDocumentFile = localMusicDirectory,
                    existingTrackUris = repos.track.listTrackLocalUris(),
                    onEach = { combo -> updateFromMusicBrainz(combo, TrackMergeStrategy.KEEP_SELF) },
                    getArtist = { repos.artist.artistCache.get(it) },
                    existingAlbumsCombos = repos.album.listAlbumCombos(),
                )
                repos.localMedia.setIsImporting(false)
            }
        }
    }

    suspend fun updateFromMusicBrainz(
        combo: AlbumWithTracksCombo,
        strategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
    ) {
        if (combo.album.musicBrainzReleaseId == null) {
            val match = repos.musicBrainz.matchAlbumWithTracks(
                combo = combo,
                strategy = strategy,
                getArtist = { repos.artist.artistCache.get(it) },
            )

            if (match != null) {
                repos.album.updateAlbum(match.album)
                repos.album.setAlbumTags(match.album.albumId, match.tags)
                repos.artist.setAlbumArtists(match.artists.toAlbumArtists())
                repos.track.setAlbumTracks(match.album.albumId, match.trackCombos.map { it.track })
                repos.artist.setTrackArtists(match.trackCombos.flatMap { it.artists.toTrackArtists() })
                match.album.musicBrainzReleaseId?.also { releaseId ->
                    repos.musicBrainz.getReleaseCoverArt(releaseId)?.also {
                        repos.album.updateAlbumArt(match.album.albumId, it)
                    }
                }
            }
        }
    }
}
