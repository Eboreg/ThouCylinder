package us.huseli.thoucylinder.interfaces

import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.track.ITrackCombo

interface IExternalTrack : IStringIdItem {
    override val id: String
    val title: String

    fun toTrackCombo(isInLibrary: Boolean, album: IAlbum? = null): ITrackCombo
}
