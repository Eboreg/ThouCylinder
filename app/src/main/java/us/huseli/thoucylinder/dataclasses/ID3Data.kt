package us.huseli.thoucylinder.dataclasses

import com.arthenica.ffmpegkit.FFprobeKit
import us.huseli.thoucylinder.getIntOrNull
import us.huseli.thoucylinder.getStringOrNull
import java.io.File

data class ID3Data(
    val title: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
)


fun File.extractID3Data(): ID3Data {
    val ff = FFprobeKit.getMediaInformation(path)?.mediaInformation
    val format = ff?.getProperty("format")
    val tags = if (format?.has("tags") == true) format.getJSONObject("tags") else null

    return ID3Data(
        title = tags?.getStringOrNull("title"),
        artist = tags?.getStringOrNull("artist"),
        albumArtist = tags?.getStringOrNull("album_artist"),
        album = tags?.getStringOrNull("album"),
        trackNumber = tags?.getIntOrNull("track"),
        year = tags?.getIntOrNull("date")?.takeIf { it in 1000..3000 },
    )
}
