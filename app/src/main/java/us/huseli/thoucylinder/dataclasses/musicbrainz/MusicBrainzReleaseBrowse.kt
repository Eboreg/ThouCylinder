package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzReleaseBrowse(
    val releases: List<MusicBrainzRelease>,
    @SerializedName("release-offset")
    val releaseOffset: Int,
    @SerializedName("release-count")
    val releaseCount: Int,
)
