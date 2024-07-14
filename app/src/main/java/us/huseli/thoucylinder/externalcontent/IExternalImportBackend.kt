package us.huseli.thoucylinder.externalcontent

import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.interfaces.IExternalAlbum

interface IExternalImportBackend<T : IExternalAlbum> {
    val albumImportHolder: AbstractAlbumImportHolder<T>
}
