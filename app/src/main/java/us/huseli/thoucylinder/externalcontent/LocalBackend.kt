package us.huseli.thoucylinder.externalcontent

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.dataclasses.album.LocalImportableAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.repositories.Repositories

class LocalBackend(
    private val repos: Repositories,
    private val context: Context,
) : IExternalImportBackend<LocalImportableAlbum> {
    private val _localImportUri = MutableStateFlow<Uri?>(null)

    override val albumImportHolder: AbstractAlbumImportHolder<LocalImportableAlbum> =
        object : AbstractAlbumImportHolder<LocalImportableAlbum>() {
            private val _importDirectoryFile =
                _localImportUri.map { uri -> uri?.let { DocumentFile.fromTreeUri(context, it) } }

            override val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
            override val isTotalCountExact: Flow<Boolean> = flowOf(true)
            override val canImport = _importDirectoryFile.map { it != null }

            override suspend fun convertToAlbumWithTracks(
                externalAlbum: LocalImportableAlbum,
                albumId: String,
            ): UnsavedAlbumWithTracksCombo =
                externalAlbum.toAlbumWithTracks(isLocal = true, isInLibrary = true, albumId = albumId)

            override fun getExternalAlbumChannel(): Channel<LocalImportableAlbum> =
                Channel<LocalImportableAlbum>().also { channel ->
                    launchOnIOThread {
                        _importDirectoryFile.filterNotNull().collectLatest { documentFile ->
                            val existingTrackUris = repos.track.listTrackLocalUris()
                            val albumChannel = repos.localMedia.importableAlbumsChannel(documentFile, existingTrackUris)

                            _items.value = emptyList()
                            _allItemsFetched.value = false
                            for (localAlbum in albumChannel) {
                                channel.send(localAlbum)
                            }
                            _allItemsFetched.value = true
                        }
                    }
                }

            override suspend fun getPreviouslyImportedIds(): List<String> = emptyList()
        }

    fun setLocalImportUri(uri: Uri) {
        _localImportUri.value = uri
    }
}
