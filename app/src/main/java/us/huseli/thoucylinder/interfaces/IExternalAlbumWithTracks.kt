package us.huseli.thoucylinder.interfaces

import us.huseli.thoucylinder.dataclasses.album.IUnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo

interface IExternalAlbumWithTracks : IExternalAlbum {
    fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String? = null,
    ): UnsavedAlbumWithTracksCombo

    override fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): IUnsavedAlbumCombo =
        toAlbumWithTracks(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId)
}
