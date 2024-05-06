package us.huseli.thoucylinder.managers

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.listCoverImages
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.getCachedFullBitmap
import us.huseli.thoucylinder.getCachedThumbnailBitmap
import us.huseli.thoucylinder.getMutexCache
import us.huseli.thoucylinder.matchDirectoriesRecursive
import us.huseli.thoucylinder.matchFiles
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder() {
    private val artistImageUriCache = MutexCache<ArtistCombo, String, Uri?>(
        itemToKey = { it.artist.artistId },
        debugLabel = "ArtistRepository.artistImageUriCache",
        fetchMethod = { combo ->
            combo.artist.image?.fullUri
                ?: repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }
                    ?.matchDirectoriesRecursive(Regex("^${combo.artist.name}"))
                    ?.map { it.matchFiles(Regex("^artist\\..*", RegexOption.IGNORE_CASE), Regex("^image/.*")) }
                    ?.flatten()
                    ?.distinctBy { it.uri.path }
                    ?.firstOrNull()
                    ?.uri
                ?: combo.listAlbumArtUris().firstOrNull()
                ?: combo.listFullImageUrls().firstOrNull()?.toUri()
        },
    )

    private val thumbnailCache = getMutexCache<Uri, Bitmap> { uri ->
        uri.getCachedThumbnailBitmap(context)
    }

    suspend fun clearAlbumArt(albumId: String) {
        repos.album.getAlbumWithTracks(albumId)?.also { combo ->
            deleteLocalAlbumArt(combo)
        }
        repos.album.clearAlbumArt(albumId)
    }

    fun collectNewLocalAlbumArtUris(combo: AlbumWithTracksCombo): List<Uri> =
        combo.trackCombos.map { it.track }.listCoverImages(context)
            .map { it.uri }
            .filter { it != combo.album.albumArt?.fullUri }

    suspend fun getArtistThumbnailImageBitmap(combo: ArtistCombo): ImageBitmap? = withContext(Dispatchers.IO) {
        artistImageUriCache.getOrNull(combo)?.getCachedThumbnailBitmap(context)?.asImageBitmap()
    }

    suspend fun getFullImageBitmap(uri: Uri?): ImageBitmap? = getFullBitmap(uri)?.asImageBitmap()

    suspend fun getPlaylistThumbnailImageBitmap(playlistId: String): ImageBitmap? = withContext(Dispatchers.IO) {
        repos.playlist.listPlaylistAlbums(playlistId).firstNotNullOfOrNull { album ->
            album.albumArt?.thumbnailUri?.let { thumbnailCache.getOrNull(it)?.asImageBitmap() }
        } ?: repos.playlist.listPlaylistTrackCombos(playlistId).firstNotNullOfOrNull { combo ->
            combo.track.image?.thumbnailUri?.let { thumbnailCache.getOrNull(it)?.asImageBitmap() }
        }
    }

    suspend fun getThumbnailImageBitmap(uri: Uri?): ImageBitmap? = getThumbnailBitmap(uri)?.asImageBitmap()

    suspend fun getTrackComboFullBitmap(trackCombo: AbstractTrackCombo): Bitmap? =
        getFullBitmap(trackCombo.album?.albumArt?.fullUri)
            ?: getFullBitmap(trackCombo.track.image?.fullUri)

    suspend fun getTrackComboFullImageBitmap(trackCombo: AbstractTrackCombo): ImageBitmap? =
        getTrackComboFullBitmap(trackCombo)?.asImageBitmap()

    suspend fun getTrackUiStateThumbnailImageBitmap(uiState: TrackUiState): ImageBitmap? =
        getThumbnailImageBitmap(uiState.albumThumbnailUri)
            ?: getThumbnailImageBitmap(uiState.trackThumbnailUri)

    suspend fun saveAlbumArt(
        albumId: String,
        albumArt: MediaStoreImage,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {},
    ) {
        repos.album.getAlbumWithTracks(albumId)?.also { combo ->
            val localAlbumArt = albumArt.saveInternal(combo.album, context)

            if (localAlbumArt != null) {
                deleteLocalAlbumArt(combo)
                if (combo.album.isLocal) repos.settings.createAlbumDirectory(combo)?.also {
                    localAlbumArt.saveToDirectory(context, it)
                }
                repos.album.updateAlbumArt(albumId, localAlbumArt)
                onSuccess()
            } else onFail()
        }
    }

    suspend fun saveAlbumArtFromUri(albumId: String, uri: Uri, onSuccess: () -> Unit, onFail: () -> Unit) {
        val albumArt = uri.toMediaStoreImage(context)

        if (albumArt != null) saveAlbumArt(albumId, albumArt, onSuccess, onFail)
        else onFail()
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun deleteLocalAlbumArt(combo: AlbumWithTracksCombo) = repos.localMedia.deleteLocalAlbumArt(
        albumCombo = combo,
        albumDirectory = repos.settings.getAlbumDirectory(combo),
    )

    private suspend fun getFullBitmap(uri: Uri?): Bitmap? =
        withContext(Dispatchers.IO) { uri?.getCachedFullBitmap(context) }

    private suspend fun getThumbnailBitmap(uri: Uri?): Bitmap? =
        withContext(Dispatchers.IO) { uri?.let { thumbnailCache.getOrNull(it) } }
}
