package us.huseli.thoucylinder.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import us.huseli.thoucylinder.dataclasses.album.AlbumCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySimplifiedAlbum
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.thoucylinder.externalcontent.holders.AbstractSearchHolder
import us.huseli.thoucylinder.repositories.Repositories

class SpotifyBackend(private val repos: Repositories) : IExternalSearchBackend<SpotifySimplifiedAlbum>, IExternalImportBackend<SpotifyAlbum> {
    override val canImport = MutableStateFlow(false)

    override val albumImportHolder: AbstractAlbumImportHolder<SpotifyAlbum> =
        object : AbstractAlbumImportHolder<SpotifyAlbum>() {
            private val _previouslyImportedIds = mutableListOf<String>()

            override val totalItemCount: Flow<Int> = combine(
                searchTerm,
                repos.spotify.totalUserAlbumCount,
                _filteredItems,
            ) { searchTerm, totalCount, items ->
                if (searchTerm.isBlank()) totalCount ?: items.size
                else items.size
            }
            override val isTotalCountExact: Flow<Boolean> =
                combine(searchTerm, repos.spotify.allUserAlbumsFetched) { term, allFetched ->
                    term == "" || allFetched
                }

            override suspend fun convertToAlbumWithTracks(
                externalAlbum: SpotifyAlbum,
                albumId: String,
            ): UnsavedAlbumWithTracksCombo = externalAlbum.toAlbumWithTracks(
                isLocal = false,
                isInLibrary = true,
                albumId = albumId,
            )

            override suspend fun doStart() {
                _previouslyImportedIds.addAll(repos.spotify.listSpotifyAlbumIds())
                super.doStart()
            }

            override fun getExternalAlbumChannel(): Channel<SpotifyAlbum> = Channel<SpotifyAlbum>().also { channel ->
                launchOnIOThread {
                    repos.spotify.oauth2PKCE.isAuthorized.collectLatest { authorized ->
                        if (authorized) {
                            canImport.value = true
                            for (spotifyAlbum in repos.spotify.userAlbumsChannel()) {
                                channel.send(spotifyAlbum)
                            }
                        } else {
                            canImport.value = false
                        }
                    }
                }
            }
        }

    override val albumSearchHolder = object : AbstractAlbumSearchHolder<SpotifySimplifiedAlbum>() {
        private var _existingAlbumCombos: Map<String, AlbumCombo>? = null

        override val isTotalCountExact: Flow<Boolean> = flowOf(true)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        private suspend fun getExistingAlbumCombos(): Map<String, AlbumCombo> =
            _existingAlbumCombos ?: repos.album.listSpotifyAlbumCombos().also { _existingAlbumCombos = it }

        override suspend fun convertToAlbumWithTracks(
            externalAlbum: SpotifySimplifiedAlbum,
            albumId: String,
        ): UnsavedAlbumWithTracksCombo? = repos.spotify.getAlbum(externalAlbum.id)?.toAlbumWithTracks(
            isLocal = false,
            isInLibrary = false,
            albumId = albumId,
        )

        override suspend fun externalAlbumToAlbumCombo(externalAlbum: SpotifySimplifiedAlbum): IAlbumCombo<IAlbum> =
            getExistingAlbumCombos()[externalAlbum.id] ?: super.externalAlbumToAlbumCombo(externalAlbum)

        override fun getExternalAlbumChannel(searchParams: SearchParams): Channel<SpotifySimplifiedAlbum> =
            repos.spotify.albumSearchChannel(searchParams)
    }

    override val trackSearchHolder = object : AbstractSearchHolder<TrackUiState>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(true)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.TRACK, SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        override fun getResultChannel(searchParams: SearchParams) = Channel<TrackUiState>().also { channel ->
            launchOnIOThread {
                for (track in repos.spotify.trackSearchChannel(searchParams)) {
                    channel.send(track.toTrackCombo(isInLibrary = false).toUiState())
                }
                channel.close()
            }
        }

    }
}
