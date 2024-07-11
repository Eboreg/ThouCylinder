package us.huseli.thoucylinder.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.lastfm.LastFmAlbum
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.repositories.Repositories

class LastFmBackend(private val repos: Repositories) : IExternalImportBackend<LastFmAlbum> {
    override val canImport = MutableStateFlow(repos.lastFm.username.value != null)

    override val albumImportHolder: AbstractAlbumImportHolder<LastFmAlbum> =
        object : AbstractAlbumImportHolder<LastFmAlbum>() {
            private val _previouslyImportedIds = mutableListOf<String>()

            override val isTotalCountExact: Flow<Boolean> = flowOf(false)

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

            override suspend fun doStart() {
                _previouslyImportedIds.addAll(repos.lastFm.listMusicBrainzReleaseIds())
                super.doStart()
            }

            override fun getExternalAlbumChannel(): Channel<LastFmAlbum> = Channel<LastFmAlbum>().also { channel ->
                launchOnIOThread {
                    repos.lastFm.username.collectLatest { username ->
                        canImport.value = username != null
                        if (username != null) {
                            for (lastFmAlbum in repos.lastFm.topAlbumsChannel()) {
                                if (!_previouslyImportedIds.contains(lastFmAlbum.mbid)) channel.send(lastFmAlbum)
                            }
                        }
                    }
                }
            }
        }
}
