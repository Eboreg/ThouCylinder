package us.huseli.thoucylinder.externalcontent

import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.interfaces.IExternalAlbum

interface IExternalImportBackend<T : IExternalAlbum> {
    val albumImportHolder: AbstractAlbumImportHolder<T>
    val canImport: StateFlow<Boolean>
}
