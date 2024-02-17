package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.stripCommonFixes
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class YoutubeVideo(
    val id: String,
    val title: String,
    val durationMs: Long? = null,
    @Embedded("metadata_") val metadata: YoutubeMetadata? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
) : Parcelable {
    val duration: Duration?
        get() = durationMs?.milliseconds ?: metadata?.durationMs?.milliseconds

    val expiresAt: Instant?
        get() = metadata?.uri?.getQueryParameter("expire")?.toLong()?.let { Instant.ofEpochSecond(it) }
}

fun Iterable<YoutubeVideo>.stripTitleCommons(): List<YoutubeVideo> = zip(map { it.title }.stripCommonFixes())
    .map { (video, title) -> video.copy(title = title.replace(Regex(" \\([^)]*$"), "")) }
