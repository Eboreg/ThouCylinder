package us.huseli.thoucylinder.dataclasses

import com.arthenica.ffmpegkit.MediaInformation
import us.huseli.thoucylinder.getDoubleOrNull
import us.huseli.thoucylinder.getIntOrNull
import us.huseli.thoucylinder.getStringOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ID3Data(
    val title: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val discNumber: Int? = null,
    val duration: Duration? = null,
    val bitrate: Int? = null,
)


fun MediaInformation.extractID3Data(): ID3Data {
    val format = getProperty("format")
    val tags = if (format?.has("tags") == true) format.getJSONObject("tags") else null

    return ID3Data(
        title = tags?.getStringOrNull("title"),
        artist = tags?.getStringOrNull("artist"),
        albumArtist = tags?.getStringOrNull("album_artist"),
        album = tags?.getStringOrNull("album"),
        trackNumber = tags?.getIntOrNull("track"),
        year = tags?.getIntOrNull("date")?.takeIf { it in 1000..3000 },
        discNumber = tags?.getIntOrNull("disc"),
        duration = format?.getDoubleOrNull("duration")?.seconds,
        bitrate = format?.getIntOrNull("bit_rate"),
    )
}
