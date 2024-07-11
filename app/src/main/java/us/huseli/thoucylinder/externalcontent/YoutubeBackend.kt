package us.huseli.thoucylinder.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.YoutubeAndroidClient
import us.huseli.thoucylinder.YoutubeWebClient
import us.huseli.thoucylinder.dataclasses.album.AlbumCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.thoucylinder.externalcontent.holders.AbstractSearchHolder
import us.huseli.thoucylinder.playlistSearchChannel
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.videoSearchChannel

class YoutubeBackend(repos: Repositories) : IExternalSearchBackend<YoutubePlaylist>, AbstractScopeHolder() {
    private val _playlistClient: StateFlow<YoutubeWebClient> = repos.youtube.region
        .map { YoutubeWebClient(region = it) }
        .stateEagerly(YoutubeWebClient(region = repos.youtube.region.value))
    private val _videoClient: StateFlow<YoutubeAndroidClient> = repos.youtube.region
        .map { YoutubeAndroidClient(region = it) }
        .stateEagerly(YoutubeAndroidClient(region = repos.youtube.region.value))

    override val albumSearchHolder = object : AbstractAlbumSearchHolder<YoutubePlaylist>() {
        private var _existingAlbumCombos: Map<String, AlbumCombo>? = null

        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> = listOf(SearchCapability.FREE_TEXT)

        private suspend fun getExistingAlbumCombos(): Map<String, AlbumCombo> =
            _existingAlbumCombos ?: repos.album.listYoutubeAlbumCombos().also { _existingAlbumCombos = it }

        override suspend fun convertToAlbumWithTracks(
            externalAlbum: YoutubePlaylist,
            albumId: String,
        ): UnsavedAlbumWithTracksCombo? = _playlistClient.value
            .getPlaylistComboFromPlaylistId(playlistId = externalAlbum.id, artist = externalAlbum.artist)
            ?.toAlbumWithTracks(isLocal = false, isInLibrary = false, albumId = albumId)

        override suspend fun externalAlbumToAlbumCombo(externalAlbum: YoutubePlaylist): IAlbumCombo<IAlbum> =
            getExistingAlbumCombos()[externalAlbum.id] ?: super.externalAlbumToAlbumCombo(externalAlbum)

        override fun getExternalAlbumChannel(searchParams: SearchParams): Channel<YoutubePlaylist> =
            playlistSearchChannel(params = searchParams, client = _playlistClient.value, scope = scope)
    }

    override val trackSearchHolder = object : AbstractSearchHolder<TrackUiState>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> = listOf(SearchCapability.FREE_TEXT)

        override fun getResultChannel(searchParams: SearchParams) = Channel<TrackUiState>().also { channel ->
            launchOnIOThread {
                for (video in videoSearchChannel(params = searchParams, client = _videoClient.value, scope = scope)) {
                    channel.send(video.toTrackCombo(isInLibrary = false).toUiState())
                }
                channel.close()
            }
        }

    }
}
