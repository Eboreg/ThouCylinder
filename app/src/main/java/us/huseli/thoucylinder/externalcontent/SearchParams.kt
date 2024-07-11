package us.huseli.thoucylinder.externalcontent

data class SearchParams(
    val track: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val freeText: String? = null,
) {
    fun isEmpty() = track.isNullOrBlank() && album.isNullOrBlank() && artist.isNullOrBlank() && freeText.isNullOrBlank()
    fun isNotEmpty() = !isEmpty()
}
