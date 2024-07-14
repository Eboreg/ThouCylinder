package us.huseli.thoucylinder.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.lastfm.LastFmAlbum
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.repositories.Repositories

class LastFmBackend(private val repos: Repositories) : IExternalImportBackend<LastFmAlbum> {
    override val albumImportHolder: AbstractAlbumImportHolder<LastFmAlbum> =
        object : AbstractAlbumImportHolder<LastFmAlbum>() {
            override val isTotalCountExact: Flow<Boolean> = flowOf(false)
            override val canImport: Flow<Boolean> = repos.lastFm.username.map { it != null }

            override suspend fun convertToAlbumWithTracks(
                externalAlbum: LastFmAlbum,
                albumId: String,
            ): UnsavedAlbumWithTracksCombo? {
                return repos.musicBrainz.getRelease(externalAlbum.mbid)?.toAlbumWithTracks(
                    isInLibrary = true,
                    isLocal = false,
                    albumArt = externalAlbum.getMediaStoreImage(),
                    albumId = albumId,
                )
            }

            override fun getExternalAlbumChannel(): Channel<LastFmAlbum> = Channel<LastFmAlbum>().also { channel ->
                launchOnIOThread {
                    repos.lastFm.username.collectLatest { username ->
                        _items.value = emptyList()
                        _allItemsFetched.value = false
                        if (username != null) {
                            for (lastFmAlbum in repos.lastFm.topAlbumsChannel()) {
                                channel.send(lastFmAlbum)
                            }
                        }
                        _allItemsFetched.value = true
                    }
                }
            }

            override suspend fun getPreviouslyImportedIds(): List<String> = repos.lastFm.listMusicBrainzReleaseIds()
        }
}
