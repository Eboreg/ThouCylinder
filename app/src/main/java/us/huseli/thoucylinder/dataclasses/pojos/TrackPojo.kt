package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

data class TrackPojo(
    @Embedded val track: Track,
    @Embedded val album: Album?,
)
