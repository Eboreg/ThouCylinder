package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.Track

data class TrackPojo(
    @Embedded override val track: Track,
    @Embedded override val album: Album? = null,
    @Embedded override val spotifyTrack: SpotifyTrack? = null,
    @Embedded override val lastFmTrack: LastFmTrack? = null,
) : AbstractTrackPojo()
