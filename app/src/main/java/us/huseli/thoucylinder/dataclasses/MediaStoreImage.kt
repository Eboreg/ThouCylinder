package us.huseli.thoucylinder.dataclasses

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.isRemote

@Parcelize
@WorkerThread
@Immutable
data class MediaStoreImage(val fullUriString: String, val thumbnailUriString: String) : Parcelable {
    constructor(fullUriString: String) : this(fullUriString, fullUriString)

    val fullUri: Uri
        get() = fullUriString.toUri()

    val isLocal: Boolean
        get() = !fullUri.isRemote
}

fun String.toMediaStoreImage(thumbnailUrl: String? = null): MediaStoreImage {
    return MediaStoreImage(
        fullUriString = this,
        thumbnailUriString = thumbnailUrl ?: this,
    )
}

fun Uri.toMediaStoreImage(): MediaStoreImage {
    return MediaStoreImage(
        fullUriString = toString(),
        thumbnailUriString = toString(),
    )
}
