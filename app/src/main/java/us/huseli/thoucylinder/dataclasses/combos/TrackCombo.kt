package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.Track

data class TrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album? = null,
    @Embedded override val spotifyTrack: SpotifyTrack? = null,
) : AbstractTrackCombo()
