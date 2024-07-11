package us.huseli.thoucylinder.externalcontent

import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.thoucylinder.externalcontent.holders.AbstractSearchHolder
import us.huseli.thoucylinder.interfaces.IExternalAlbum

interface IExternalSearchBackend<T : IExternalAlbum> {
    val albumSearchHolder: AbstractAlbumSearchHolder<T>
    val trackSearchHolder: AbstractSearchHolder<TrackUiState>

    fun getSearchHolder(listType: ExternalListType) = when (listType) {
        ExternalListType.ALBUMS -> albumSearchHolder
        ExternalListType.TRACKS -> trackSearchHolder
    }
}
