package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.stripCommonFixes
import us.huseli.thoucylinder.dataclasses.interfaces.IExternalTrack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class YoutubeVideo(
    override val id: String,
    override val title: String,
    val durationMs: Long? = null,
    @Embedded("metadata_") val metadata: YoutubeMetadata? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
) : Parcelable, IExternalTrack {
    val duration: Duration?
        get() = durationMs?.milliseconds ?: metadata?.durationMs?.milliseconds

    val metadataRefreshNeeded: Boolean
        get() = metadata == null || metadata.isOld
}

fun Iterable<YoutubeVideo>.stripTitleCommons(): List<YoutubeVideo> = zip(map { it.title }.stripCommonFixes())
    .map { (video, title) -> video.copy(title = title.replace(Regex(" \\([^)]*$"), "")) }
