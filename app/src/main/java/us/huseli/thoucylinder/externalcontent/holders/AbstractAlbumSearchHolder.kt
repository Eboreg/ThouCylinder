package us.huseli.thoucylinder.externalcontent.holders

import kotlinx.coroutines.channels.Channel
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.ImportableAlbumUiState
import us.huseli.thoucylinder.externalcontent.SearchParams
import us.huseli.thoucylinder.interfaces.IExternalAlbum

abstract class AbstractAlbumSearchHolder<T : IExternalAlbum> : AbstractSearchHolder<ImportableAlbumUiState>() {
    private val _externalAlbums = mutableMapOf<String, T>()

    protected abstract fun getExternalAlbumChannel(searchParams: SearchParams): Channel<T>
    protected abstract suspend fun convertToAlbumWithTracks(
        externalAlbum: T,
        albumId: String,
    ): IAlbumWithTracksCombo<IAlbum>?

    suspend fun convertToAlbumWithTracks(albumId: String): IAlbumWithTracksCombo<IAlbum>? =
        _externalAlbums[albumId]?.let { convertToAlbumWithTracks(it, albumId) }

    open suspend fun externalAlbumToAlbumCombo(externalAlbum: T): IAlbumCombo<IAlbum> =
        externalAlbum.toAlbumCombo(isLocal = false, isInLibrary = false)

    override fun getResultChannel(searchParams: SearchParams) = Channel<ImportableAlbumUiState>().also { channel ->
        launchOnIOThread {
            for (externalAlbum in getExternalAlbumChannel(searchParams)) {
                val combo = externalAlbumToAlbumCombo(externalAlbum)

                _externalAlbums[combo.id] = externalAlbum
                channel.send(combo.toImportableUiState(playCount = externalAlbum.playCount))
            }
            channel.close()
        }
    }
}
