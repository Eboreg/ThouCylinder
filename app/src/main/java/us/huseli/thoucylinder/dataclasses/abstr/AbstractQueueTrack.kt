package us.huseli.thoucylinder.dataclasses.abstr

import android.net.Uri
import java.util.UUID

abstract class AbstractQueueTrack {
    abstract val queueTrackId: UUID
    abstract val trackId: UUID
    abstract val uri: Uri
    abstract val position: Int
}