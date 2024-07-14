package us.huseli.thoucylinder

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.map.Mapper
import coil.request.Options
import coil.size.pxOrElse
import okio.buffer
import okio.source
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_PX
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveResponse
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.interfaces.IAlbumArtOwner
import us.huseli.thoucylinder.interfaces.IHasMusicBrainzIds
import us.huseli.thoucylinder.interfaces.ILogger
import java.io.InputStream
import java.net.HttpURLConnection

abstract class AbstractThumbnailMapper<T : Any> : Mapper<T, String> {
    fun shouldGetFullImage(options: Options): Boolean {
        val height = options.size.height.pxOrElse { 0 }
        val width = options.size.width.pxOrElse { 0 }

        return height > IMAGE_THUMBNAIL_MAX_WIDTH_PX || width > IMAGE_THUMBNAIL_MAX_WIDTH_PX
    }
}


class AlbumArtMapper : AbstractThumbnailMapper<IAlbumArtOwner>() {
    override fun map(data: IAlbumArtOwner, options: Options): String? =
        if (shouldGetFullImage(options)) data.fullImageUrl ?: data.thumbnailUrl
        else data.thumbnailUrl ?: data.fullImageUrl
}


class MediaStoreImageMapper : AbstractThumbnailMapper<MediaStoreImage>() {
    override fun map(data: MediaStoreImage, options: Options): String =
        if (shouldGetFullImage(options)) data.fullUriString
        else data.thumbnailUriString
}


@OptIn(ExperimentalCoilApi::class)
class CoverArtArchiveFetcher(
    private val data: IHasMusicBrainzIds,
    private val options: Options,
    private val imageLoader: ImageLoader,
) : Fetcher, ILogger {
    private val shouldGetFullImage: Boolean
        get() {
            val height = options.size.height.pxOrElse { 0 }
            val width = options.size.width.pxOrElse { 0 }
            return height > IMAGE_THUMBNAIL_MAX_WIDTH_PX || width > IMAGE_THUMBNAIL_MAX_WIDTH_PX
        }

    private suspend fun fetch(coverArtArchiveUrl: String): FetchResult {
        val readSnapshot = imageLoader.diskCache?.openSnapshot(coverArtArchiveUrl)

        if (readSnapshot != null) {
            log("Got snapshot from disk cache (data=${readSnapshot.data})")

            return SourceResult(
                source = ImageSource(
                    file = readSnapshot.data,
                    fileSystem = imageLoader.diskCache!!.fileSystem,
                    diskCacheKey = coverArtArchiveUrl,
                    closeable = readSnapshot,
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK,
            )
        }

        val headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)
        val albumArt = Request(url = coverArtArchiveUrl, headers = headers)
            .getObject<CoverArtArchiveResponse>()
            .images
            .first { it.front }
            .toMediaStoreImage()
        val url = if (shouldGetFullImage) albumArt.fullUriString else albumArt.thumbnailUriString
        val request = Request(url)

        log("Got $url from coverartarchive, building new request (size=${options.size})")

        val inputStream = request.getInputStream()
        val writeSnapshot = writeToDiskCache(
            diskCacheKey = coverArtArchiveUrl,
            responseCode = request.responseCode,
            inputStream = inputStream,
        )

        if (writeSnapshot != null) {
            return SourceResult(
                source = ImageSource(
                    file = writeSnapshot.data,
                    fileSystem = imageLoader.diskCache!!.fileSystem,
                    diskCacheKey = coverArtArchiveUrl,
                    closeable = writeSnapshot,
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.NETWORK,
            )
        }

        return SourceResult(
            source = ImageSource(
                source = inputStream.source().buffer(),
                context = options.context,
            ),
            mimeType = "image/jpeg",
            dataSource = DataSource.NETWORK,
        )
    }

    override suspend fun fetch(): FetchResult? {
        val coverArtArchiveUrls = listOfNotNull(
            data.musicBrainzReleaseGroupId?.let { "https://coverartarchive.org/release-group/$it" },
            data.musicBrainzReleaseId?.let { "https://coverartarchive.org/release/$it" },
        )

        for (coverArtArchiveUrl in coverArtArchiveUrls) {
            try {
                return fetch(coverArtArchiveUrl)
            } catch (e: Exception) {
                logError(e)
            }
        }

        return null
    }

    private fun writeToDiskCache(
        diskCacheKey: String,
        responseCode: Int?,
        inputStream: InputStream,
    ): DiskCache.Snapshot? {
        val editor = imageLoader.diskCache?.openEditor(diskCacheKey) ?: return null

        try {
            if (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
                imageLoader.diskCache?.fileSystem?.write(editor.data) {
                    write(inputStream.readBytes())
                }
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (_: Exception) {
            }
            throw e
        }
    }

    class Factory : Fetcher.Factory<IHasMusicBrainzIds> {
        override fun create(data: IHasMusicBrainzIds, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.musicBrainzReleaseId == null && data.musicBrainzReleaseGroupId == null) return null
            return CoverArtArchiveFetcher(data, options, imageLoader)
        }
    }
}
