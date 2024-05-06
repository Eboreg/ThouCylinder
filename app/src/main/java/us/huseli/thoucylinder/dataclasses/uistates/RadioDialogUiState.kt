package us.huseli.thoucylinder.dataclasses.uistates

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtist
import us.huseli.thoucylinder.dataclasses.views.RadioCombo

@Immutable
data class RadioDialogUiState(
    val activeRadio: RadioCombo? = null,
    val activeTrack: Track? = null,
    val activeAlbum: Album? = null,
    val activeArtist: AbstractArtist? = null,
    val libraryRadioNovelty: Float,
) {
    data class Album(
        val albumId: String,
        val title: String,
        val artists: ImmutableList<AbstractArtist> = persistentListOf(),
    )

    data class Track(
        val trackId: String,
        val title: String,
        val album: Album? = null,
        val artists: ImmutableList<AbstractArtist> = persistentListOf(),
    )

    val albums: ImmutableList<Album>
        get() = setOfNotNull(activeAlbum, activeTrack?.album)
            .filter { activeRadio?.album?.albumId != it.albumId }
            .toImmutableList()

    val artists: ImmutableList<AbstractArtist>
        get() = setOfNotNull(
            activeArtist,
            *(activeTrack?.artists ?: emptyList()).toTypedArray(),
            *(activeAlbum?.artists ?: emptyList()).toTypedArray(),
        ).filter { activeRadio?.artist != it }.toImmutableList()
}
