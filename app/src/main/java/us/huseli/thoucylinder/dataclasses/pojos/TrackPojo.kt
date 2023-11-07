package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

data class TrackPojo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
) : AbstractTrackPojo()
