package us.huseli.thoucylinder.dataclasses.views

import androidx.compose.runtime.Immutable
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import us.huseli.retaintheme.extensions.stripCommonFixes
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

@DatabaseView(
    """
    SELECT Track.*, Album.*
    FROM Track LEFT JOIN Album ON Track_albumId = Album_albumId
    GROUP BY Track_trackId
    ORDER BY Track_discNumber, Track_albumPosition, Track_title
    """
)
@Immutable
data class TrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album? = null,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId")
    override val artists: List<TrackArtistCredit> = emptyList(),
    @Relation(parentColumn = "Track_albumId", entityColumn = "AlbumArtist_albumId")
    override val albumArtists: List<AlbumArtistCredit> = emptyList(),
    @Ignore val localPath: String? = null,
) : AbstractTrackCombo() {
    constructor(
        track: Track,
        album: Album? = null,
        artists: List<TrackArtistCredit> = emptyList(),
        albumArtists: List<AlbumArtistCredit> = emptyList(),
    ) : this(track, album, artists, albumArtists, null)

    fun toQueueTrackCombo(): QueueTrackCombo? = track.playUri?.let { uri ->
        QueueTrackCombo(
            track = track,
            uri = uri,
            album = album,
            albumArtists = albumArtists,
            artists = artists,
        )
    }
}

fun Iterable<TrackCombo>.stripTitleCommons(): List<TrackCombo> =
    zip(map { it.track.title }.stripCommonFixes()).map { (combo, title) ->
        combo.copy(track = combo.track.copy(title = title.replace(Regex(" \\([^)]*$"), "")))
    }
