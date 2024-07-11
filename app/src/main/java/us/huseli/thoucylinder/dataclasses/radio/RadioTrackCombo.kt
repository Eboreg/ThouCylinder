package us.huseli.thoucylinder.dataclasses.radio

import us.huseli.thoucylinder.dataclasses.track.QueueTrackCombo

data class RadioTrackCombo(
    val queueTrackCombo: QueueTrackCombo,
    val spotifyId: String? = null,
    val localId: String? = null,
)
