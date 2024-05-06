package us.huseli.thoucylinder.dataclasses

import androidx.compose.runtime.Immutable

@Immutable
data class UnsavedArtist(
    val name: String,
    val spotifyId: String? = null,
    val musicBrainzId: String? = null,
    val image: MediaStoreImage? = null,
) {
    override fun equals(other: Any?) = other is UnsavedArtist && other.name == name

    override fun hashCode(): Int = name.hashCode()
}
