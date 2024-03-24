package us.huseli.thoucylinder.dataclasses.views

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.retaintheme.extensions.stripCommonFixes
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

@DatabaseView(
    """
    SELECT Track.*, Album.*, GROUP_CONCAT(AlbumArtist_name, '/') AS albumArtist
    FROM Track
        LEFT JOIN Album ON Track_albumId = Album_albumId
        LEFT JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId
    GROUP BY Track_trackId
    ORDER BY Track_discNumber, Track_albumPosition, Track_title
    """
)
data class TrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album? = null,
    override val albumArtist: String? = null,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId")
    override val artists: List<TrackArtistCredit> = emptyList(),
) : AbstractTrackCombo()

fun Iterable<TrackCombo>.stripTitleCommons(): List<TrackCombo> =
    zip(map { it.track.title }.stripCommonFixes()).map { (combo, title) ->
        combo.copy(track = combo.track.copy(title = title.replace(Regex(" \\([^)]*$"), "")))
    }
