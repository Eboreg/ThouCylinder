package us.huseli.thoucylinder.interfaces

import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.IUnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.enums.AlbumType
import kotlin.time.Duration

interface IExternalAlbum : IStringIdItem {
    override val id: String
    val title: String
    val artistName: String?
    val thumbnailUrl: String?
    val trackCount: Int?
    val year: Int?
    val duration: Duration?
    val playCount: Int?
    val albumType: AlbumType?

    fun getMediaStoreImage(): MediaStoreImage? = thumbnailUrl?.toMediaStoreImage()
    fun toAlbumCombo(isLocal: Boolean, isInLibrary: Boolean, albumId: String? = null): IUnsavedAlbumCombo
}
