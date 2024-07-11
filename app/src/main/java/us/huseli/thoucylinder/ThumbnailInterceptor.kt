package us.huseli.thoucylinder

import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.pxOrElse
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_PX
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveResponse
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumUiState
import us.huseli.thoucylinder.dataclasses.track.AbstractTrackUiState
import us.huseli.thoucylinder.interfaces.ILogger

class ThumbnailInterceptor : Interceptor, ILogger {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val height = chain.size.height.pxOrElse { 0 }
        val width = chain.size.width.pxOrElse { 0 }
        val getFullImage = height > IMAGE_THUMBNAIL_MAX_WIDTH_PX || width > IMAGE_THUMBNAIL_MAX_WIDTH_PX
        val imageUrl: String?
        val musicBrainzReleaseId: String?
        val musicBrainzReleaseGroupId: String?
        val data = chain.request.data

        when (data) {
            is IAlbumUiState -> {
                imageUrl = if (getFullImage) data.fullImageUri else data.thumbnailUri
                musicBrainzReleaseId = data.musicBrainzReleaseId
                musicBrainzReleaseGroupId = data.musicBrainzReleaseGroupId
            }
            is IAlbum -> {
                imageUrl = if (getFullImage) data.fullImageUrl else data.thumbnailUrl
                musicBrainzReleaseId = data.musicBrainzReleaseId
                musicBrainzReleaseGroupId = data.musicBrainzReleaseGroupId
            }
            is AbstractTrackUiState -> {
                imageUrl = if (getFullImage) data.fullImageUrl else data.thumbnailUrl
                musicBrainzReleaseId = data.musicBrainzReleaseId
                musicBrainzReleaseGroupId = data.musicBrainzReleaseGroupId
            }
            is MediaStoreImage -> {
                imageUrl = if (getFullImage) data.fullUriString else data.thumbnailUriString
                musicBrainzReleaseId = null
                musicBrainzReleaseGroupId = null
            }
            else -> {
                log("Data is ${data.javaClass.simpleName}, proceeding (size=${chain.size})")
                return chain.proceed(chain.request)
            }
        }

        if (imageUrl != null) {
            log("Data is ${data.javaClass.simpleName}, building new request for $imageUrl (size=${chain.size})")
            return chain.proceed(chain.request.newBuilder().data(imageUrl).build())
        }

        val coverArtArchiveUrl = musicBrainzReleaseId?.let { "https://coverartarchive.org/release/$it" }
            ?: musicBrainzReleaseGroupId?.let { "https://coverartarchive.org/release-group/$it" }

        if (coverArtArchiveUrl != null) {
            val headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)
            val albumArt = Request(url = coverArtArchiveUrl, headers = headers)
                .getObjectOrNull<CoverArtArchiveResponse>()
                ?.images
                ?.firstOrNull { it.front }
                ?.toMediaStoreImage()

            if (albumArt != null) {
                val url = if (getFullImage) albumArt.fullUriString else albumArt.thumbnailUriString

                log("Got $url from coverartarchive, building new request (size=${chain.size})")
                return chain.proceed(chain.request.newBuilder().data(url).build())
            }
        }

        return try {
            SuccessResult(chain.request.fallback!!, chain.request, DataSource.MEMORY_CACHE)
        } catch (e: Exception) {
            ErrorResult(chain.request.error, chain.request, e)
        }
    }
}
