package us.huseli.thoucylinder

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
class ContainerFormat(private val mimeType: String) : Parcelable {
    val audioFileExtension: String
        get() = audioMimeType.split("/").last()

    val audioMimeType: String
        get() {
            if (shortMimeType == "audio/mp4") {
                val oti = getMpeg4OTI()

                if (oti == "40") return "audio/aac"
                if (oti == "ad") return "audio/opus"
            } else if (shortMimeType == "audio/webm") {
                if (codecs.contains("opus")) return "audio/opus"
                if (codecs.contains("vorbis")) return "audio/ogg"
            }
            return shortMimeType
        }

    val shortMimeType: String
        get() = mimeType.split(";").first()

    private val codecs: List<String>
        get() = Regex("^.*codecs=\"?([^\"]*)\"?$")
            .find(mimeType)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(",")
            ?: emptyList()

    private fun getMpeg4OTI(): String? {
        /**
         * Extract (for example) the "40" in "mp4a.40.5", which is the Object Type Indication (OTI).
         * (40 means MPEG-4 audio, which in turn has a bunch of subtypes, but for our purposes, just AAC should
         * probably suffice.)
         */
        for (codec in codecs) {
            val oti = Regex("^mp4a\\.([a-f0-9]+)\\.?.*", RegexOption.IGNORE_CASE)
                .find(codec)
                ?.groupValues
                ?.getOrNull(1)
                ?.lowercase()

            if (oti != null) return oti
        }
        return null
    }
}
