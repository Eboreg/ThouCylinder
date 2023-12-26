package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack

data class LastFmAlbumPojo(
    @Embedded val album: LastFmAlbum,
    @Relation(
        entity = LastFmTrack::class,
        parentColumn = "LastFmAlbum_musicBrainzId",
        entityColumn = "LastFmTrack_lastFmAlbumId",
    )
    val tracks: List<LastFmTrack>,
)
