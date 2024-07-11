package us.huseli.thoucylinder.dataclasses.radio

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.dataclasses.artist.Artist
import us.huseli.thoucylinder.dataclasses.artist.IArtist

@Immutable
data class RadioDialogUiState(
    val activeRadio: RadioUiState? = null,
    val activeTrack: Track? = null,
    val activeAlbum: Album? = null,
    val activeArtist: Artist? = null,
    val libraryRadioNovelty: Float,
) {
    data class Album(
        val albumId: String,
        val title: String,
        val artists: ImmutableList<IArtist> = persistentListOf(),
    )

    data class Track(
        val trackId: String,
        val title: String,
        val album: Album? = null,
        val artists: ImmutableList<IArtist> = persistentListOf(),
    )

    val albums: ImmutableList<Album>
        get() = setOfNotNull(activeAlbum, activeTrack?.album).toImmutableList()

    val artists: ImmutableList<IArtist>
        get() = setOfNotNull(
            activeArtist,
            *(activeTrack?.artists ?: emptyList()).toTypedArray(),
            *(activeAlbum?.artists ?: emptyList()).toTypedArray(),
        ).toImmutableList()
}
