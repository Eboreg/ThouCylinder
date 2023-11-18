package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Ignore
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style


data class AlbumPojo(
    @Embedded override val album: Album,
    override val durationMs: Long? = null,
    override val minYear: Int? = null,
    override val maxYear: Int? = null,
    override val trackCount: Int,
    @Ignore override val genres: List<Genre> = emptyList(),
    @Ignore override val styles: List<Style> = emptyList(),
    @Ignore override val spotifyAlbum: SpotifyAlbum? = null,
) : AbstractAlbumPojo() {
    constructor(
        album: Album,
        durationMs: Long?,
        minYear: Int?,
        maxYear: Int?,
        trackCount: Int,
    ) : this(
        album = album,
        durationMs = durationMs,
        minYear = minYear,
        maxYear = maxYear,
        trackCount = trackCount,
        spotifyAlbum = null,
        genres = emptyList(),
        styles = emptyList(),
    )

    override fun toString() = album.toString()
}
