package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

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

    suspend fun ensureTrackMetadata(track: Track, forceReload: Boolean = false): Track =
        repos.youtube.ensureTrackMetadata(track, forceReload) { repos.track.updateTrack(it) }

    suspend fun getAlbumThumbnail(album: Album): ImageBitmap? =
        album.albumArt?.let { repos.album.thumbnailCache.getOrNull(it) }

    suspend fun getTrackThumbnail(track: Track, album: Album? = null): ImageBitmap? =
        track.image?.let { repos.track.thumbnailCache.getOrNull(it) } ?: album?.let { getAlbumThumbnail(it) }

    fun importNewLocalAlbums(context: Context, existingTracks: List<Track>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!repos.localMedia.isImportingLocalMedia.value) {
                val localMusicDirectory =
                    repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

                if (localMusicDirectory != null) {
                    repos.localMedia.setIsImporting(true)

                    val existingTrackUris =
                        existingTracks?.mapNotNull { it.localUri } ?: repos.track.listTrackLocalUris()

                    repos.localMedia.importNewLocalAlbums(localMusicDirectory, existingTrackUris)
                    repos.localMedia.setIsImporting(false)
                }
            }
        }
    }

    fun saveTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) { repos.track.updateTrack(track) }
}
