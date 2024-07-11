package us.huseli.thoucylinder.externalcontent

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import us.huseli.thoucylinder.dataclasses.album.LocalImportableAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.repositories.Repositories

@OptIn(ExperimentalCoroutinesApi::class)
class LocalBackend(
    private val repos: Repositories,
    private val context: Context,
) : IExternalImportBackend<LocalImportableAlbum> {
    private val _localImportUri = MutableStateFlow<Uri?>(null)

    override val canImport = MutableStateFlow(false)

    override val albumImportHolder: AbstractAlbumImportHolder<LocalImportableAlbum> =
        object : AbstractAlbumImportHolder<LocalImportableAlbum>() {
            override val isTotalCountExact: Flow<Boolean> = flowOf(true)

            override suspend fun convertToAlbumWithTracks(
                externalAlbum: LocalImportableAlbum,
                albumId: String,
            ): UnsavedAlbumWithTracksCombo {
                return externalAlbum.toAlbumWithTracks(isLocal = true, isInLibrary = true, albumId = albumId)
            }

            override fun getExternalAlbumChannel(): Channel<LocalImportableAlbum> =
                Channel<LocalImportableAlbum>().also { channel ->
                    launchOnIOThread {
                        _localImportUri.flatMapLatest { uri ->
                            val documentFile = uri?.let { DocumentFile.fromTreeUri(context, it) }

                            if (documentFile != null) {
                                val existingTrackUris = repos.track.listTrackLocalUris()

                                canImport.value = true
                                repos.localMedia.flowImportableAlbums(documentFile, existingTrackUris)
                            } else {
                                canImport.value = false
                                emptyFlow()
                            }
                        }.collect { album ->
                            channel.send(album)
                        }
                    }
                }
        }

    fun setLocalImportUri(uri: Uri) {
        _localImportUri.value = uri
    }
}
