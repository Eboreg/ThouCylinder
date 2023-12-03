package us.huseli.thoucylinder.repositories

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.DownloadStatus
import us.huseli.thoucylinder.MediaStoreFormatException
import us.huseli.thoucylinder.dataclasses.ID3Data
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.deleteMediaStoreUriAndFile
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.MediaStoreData
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.extractID3Data
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import us.huseli.thoucylinder.dataclasses.getMediaStoreEntries
import us.huseli.thoucylinder.dataclasses.getMediaStoreEntry
import us.huseli.thoucylinder.dataclasses.getMediaStoreFile
import us.huseli.thoucylinder.dataclasses.getMediaStoreFileNullable
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.escapeQuotes
import us.huseli.thoucylinder.getReadOnlyImageCollection
import us.huseli.thoucylinder.getReadWriteAudioCollection
import us.huseli.thoucylinder.getReadWriteImageCollection
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
) {
    private val _isImportingLocalMedia = MutableStateFlow(false)

    val isImportingLocalMedia = _isImportingLocalMedia.asStateFlow()

    fun setIsImporting(value: Boolean) {
        _isImportingLocalMedia.value = value
    }

    /** IMAGE RELATED METHODS ************************************************/

    fun collectAlbumImages(pojo: AlbumWithTracksPojo): Set<File> {
        val relativePathSet =
            pojo.tracks.mapNotNull { track -> track.mediaStoreData?.relativePath?.let { "$it%" } }.toSet()
        val pathSelectors = relativePathSet.map { "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" }
        if (pathSelectors.isEmpty()) return emptySet()
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND (${pathSelectors.joinToString(" OR ")})"

        return context.getMediaStoreEntries(
            collection = getReadOnlyImageCollection(),
            selection = selection,
            selectionArgs = arrayOf("cover.%", *relativePathSet.toTypedArray()),
        ).map { entry -> entry.file }.toMutableSet()
    }

    fun collectArtistImages(): Map<String, File> {
        return context.getMediaStoreEntries(
            collection = getReadOnlyImageCollection(),
            selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
            selectionArgs = arrayOf("artist.%"),
        ).associate { image ->
            image.relativePath.trim('/').split('/').last().lowercase() to image.file
        }
    }

    fun deleteImages(pojo: AlbumWithTracksPojo) {
        deleteImagesByAlbums(listOf(pojo.album))
        deleteImagesByTracks(pojo.tracks)
    }

    fun deleteImagesByAlbums(albums: Collection<Album>) = albums.forEach { it.albumArt?.delete(context) }

    fun deleteImagesByTracks(tracks: Collection<Track>) = tracks.forEach { it.image?.delete(context) }

    fun deleteOrphanImages(except: Collection<Uri>) {
        context.getMediaStoreEntries(
            collection = getReadWriteImageCollection(),
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ? OR ${MediaStore.Images.Media.RELATIVE_PATH} = ?",
            selectionArgs = arrayOf(MediaStoreImage.albumRelativePath, MediaStoreImage.trackRelativePath),
        ).forEach { entry ->
            if (!except.contains(entry.contentUri)) {
                entry.deleteWithEmptyParentDirs(context)
            }
        }
    }

    /** AUDIO RELATED METHODS ************************************************/

    fun deleteTracks(tracks: Collection<Track>) = tracks
        .mapNotNull { it.mediaStoreData?.uri }
        .mapNotNull { context.getMediaStoreEntry(it) }
        .forEach { it.deleteWithEmptyParentDirs(context) }

    fun listNewMediaStoreAlbums(existingTracks: Collection<Track>): List<AlbumWithTracksPojo> {
        val audioCollection = getReadWriteAudioCollection()
        val projection = arrayOf(
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.VOLUME_NAME,
        )
        val volume = settingsRepo.musicImportVolume.value
        val relativePath = settingsRepo.musicImportDirectory.value
        val selection =
            if (volume != null) "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Audio.Media.VOLUME_NAME} LIKE ?"
            else "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs =
            if (volume != null) arrayOf("$relativePath%", volume)
            else arrayOf("$relativePath%")
        val mediaStoreUris = existingTracks.mapNotNull { it.mediaStoreData?.uri }
        val albums = mutableListOf<Album>()
        val tracks = mutableListOf<Track>()

        context.contentResolver.query(audioCollection, projection, selection, selectionArgs, null)?.use { cursor ->
            val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeTypeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val trackIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val yearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val relativePathIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val discNumberIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            while (cursor.moveToNext()) {
                cursor.getStringOrNull(dataIdx)?.let { filename ->
                    val file = File(filename)
                    val mimeType = cursor.getStringOrNull(mimeTypeIdx)
                    val itemRelativePath = cursor.getStringOrNull(relativePathIdx)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idIdx),
                    )
                    val contentUriAlt =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContentUris.withAppendedId(
                            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                            cursor.getLong(idIdx),
                        )
                        else null

                    if (
                        mimeType != null &&
                        itemRelativePath != null &&
                        mimeType.startsWith("audio/") &&
                        file.isFile &&
                        !mediaStoreUris.contains(contentUri) &&
                        (contentUriAlt == null || !mediaStoreUris.contains(contentUriAlt))
                    ) {
                        val ff = FFprobeKit.getMediaInformation(file.path)?.mediaInformation
                        val id3 = ff?.extractID3Data() ?: ID3Data()
                        val lastPathSegments = itemRelativePath
                            .replace(Regex("^${relativePath}/(.*?)/?$"), "$1")
                            .trim('/').split("/").last().split(" - ", limit = 2)
                        val pathArtist = lastPathSegments.takeIf { it.size > 1 }?.get(0)
                        val pathTitle = lastPathSegments.last().takeIf { it.isNotBlank() }
                        val trackArtist =
                            id3.artist ?: cursor.getStringOrNull(artistIdx)?.takeIf { it != "<unknown>" }
                        val albumArtist =
                            id3.albumArtist ?: cursor.getStringOrNull(albumArtistIdx)?.takeIf { it != "<unknown>" }
                        val albumTitle = id3.album ?: cursor.getStringOrNull(albumIdx) ?: pathTitle
                        val finalAlbumArtist = albumArtist ?: pathArtist ?: trackArtist
                        val finalAlbumTitle = albumTitle ?: "Unknown album"
                        val durationMs = cursor.getIntOrNull(durationIdx)?.toLong()
                            ?: ff?.duration?.toFloat()?.times(1000)?.toLong() ?: 0L
                        val albumPosition = id3.trackNumber
                            ?: cursor.getIntOrNull(trackIdx)
                            ?: Regex("^\\d+").find(filename)?.groupValues?.first()?.toInt()

                        val album =
                            albums.find { it.artist == finalAlbumArtist && it.title == finalAlbumTitle } ?: Album(
                                title = finalAlbumTitle,
                                artist = finalAlbumArtist,
                                isInLibrary = false,
                                isLocal = true,
                            ).also { albums.add(it) }

                        try {
                            tracks.add(
                                Track(
                                    title = id3.title ?: cursor.getStringOrNull(titleIdx) ?: "Unknown title",
                                    isInLibrary = false,
                                    artist = trackArtist ?: finalAlbumArtist,
                                    albumPosition = albumPosition,
                                    discNumber = id3.discNumber ?: cursor.getIntOrNull(discNumberIdx),
                                    year = id3.year ?: cursor.getIntOrNull(yearIdx),
                                    albumId = album.albumId,
                                    metadata = file.extractTrackMetadata(ff).copy(
                                        durationMs = durationMs,
                                        extension = filename.split(".").last(),
                                        mimeType = mimeType,
                                        size = file.length(),
                                    ),
                                    mediaStoreData = MediaStoreData(uri = contentUri, relativePath = itemRelativePath),
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(javaClass.simpleName, "listNewMediaStoreAlbums: $e, file=$file", e)
                        }
                    }
                }
            }
        }

        return albums.map { album ->
            AlbumWithTracksPojo(
                album = album,
                tracks = tracks.filter { it.albumId == album.albumId }.sortedBy { it.albumPosition },
            )
        }
    }

    fun listOrphanTracks(allTracks: Collection<Track>): List<Track> {
        /** Collect tracks that have no existing media files. */
        return allTracks
            .filterNot { track ->
                track.mediaStoreData?.uri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        true
                    } catch (_: FileNotFoundException) {
                        false
                    }
                } ?: false
            }
    }

    suspend fun moveTaggedTrackToMediaStore(
        track: Track,
        tempFile: File,
        relativePath: String,
        albumPojo: AbstractAlbumPojo? = null,
        progressCallback: (Double) -> Unit,
        statusCallback: (DownloadStatus) -> Unit,
    ): Track {
        val getFilename = { extension: String -> "${track.generateBasename()}.$extension" }

        statusCallback(DownloadStatus.MOVING)

        val mediaStoreUri = try {
            moveTempFileToMediaStore(
                tempFile = tempFile,
                relativePath = relativePath,
                filename = getFilename(tempFile.extension),
            ).also { progressCallback(1.0) }
        } catch (e: MediaStoreFormatException) {
            if (!tempFile.path.endsWith(".opus")) {
                statusCallback(DownloadStatus.CONVERTING)

                val convertedFile = File(tempFile.path.substringBeforeLast('.') + ".opus")
                val ffmpegCommand = "-y -i ${tempFile.path} -vn ${convertedFile.path}"

                Log.i(javaClass.simpleName, "moveTaggedTrackToMediaStore: ffmpegCommand=$ffmpegCommand")
                val session = FFmpegKit.execute(ffmpegCommand)

                tempFile.delete()
                if (!session.returnCode.isValueSuccess) {
                    convertedFile.delete()
                    throw Exception("FFMPEG conversion of $track failed")
                }
                progressCallback(0.5)
                moveTempFileToMediaStore(
                    tempFile = convertedFile,
                    relativePath = relativePath,
                    filename = getFilename("opus"),
                ).also { progressCallback(1.0) }
            } else throw e
        }
        val mediaStoreFile = context.getMediaStoreFile(mediaStoreUri)
        val metadata = mediaStoreFile.extractTrackMetadata()

        context.contentResolver.update(
            mediaStoreUri,
            getTrackContentValues(track, metadata, albumPojo?.album),
            null,
            null,
        )
        tagTrack(track = track, localFile = mediaStoreFile, albumPojo = albumPojo)

        return track.copy(
            metadata = metadata,
            isInLibrary = true,
            mediaStoreData = MediaStoreData(uri = mediaStoreUri, relativePath = relativePath),
            albumId = albumPojo?.album?.albumId,
        )
    }

    suspend fun tagAlbumTracks(pojo: AlbumWithTracksPojo) {
        pojo.tracks.forEach { track ->
            val trackFile = track.mediaStoreData?.getFile(context)

            if (trackFile != null) {
                if (!trackFile.canWrite())
                    Log.e(this::class.simpleName, "tagAlbumTracks: Cannot write to $trackFile")
                else
                    tagTrack(track = track, localFile = trackFile, albumPojo = pojo)
            }
            track.mediaStoreData?.uri?.also { uri ->
                val contentValues = getTrackContentValues(track, track.metadata, pojo.album)
                try {
                    context.contentResolver.update(uri, contentValues, null, null)
                } catch (_: SecurityException) {
                    Log.e(this::class.simpleName, "tagAlbumTracks: Cannot update media store for $uri")
                }
            }
        }
    }

    /** PRIVATE METHODS *******************************************************/

    private fun getTrackContentValues(track: Track, metadata: TrackMetadata? = null, album: Album? = null) =
        ContentValues().apply {
            album?.also { put(MediaStore.Audio.Media.ALBUM, it.title) }
            (track.artist ?: album?.artist)?.also { put(MediaStore.Audio.Media.ARTIST, it) }
            album?.artist?.also { put(MediaStore.Audio.Media.ALBUM_ARTIST, it) }
            album?.year?.also { put(MediaStore.Audio.Media.YEAR, it) }
            (metadata ?: track.metadata)?.also {
                put(MediaStore.Audio.Media.MIME_TYPE, it.mimeType)
                put(MediaStore.Audio.Media.DURATION, it.durationMs.toInt())
                it.bitrate?.also { bitrate -> put(MediaStore.Audio.Media.BITRATE, bitrate) }
            }
            put(MediaStore.Audio.Media.TITLE, track.title)
            track.albumPosition?.also { put(MediaStore.Audio.Media.TRACK, it.toString()) }
        }

    /**
     * @throws MediaStoreFormatException
     */
    private fun moveTempFileToMediaStore(tempFile: File, relativePath: String, filename: String): Uri {
        // If file already exists, just delete it first.
        context.deleteMediaStoreUriAndFile(getReadWriteAudioCollection(), relativePath, filename)

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.SIZE, tempFile.length())
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val contentUri = try {
            context.contentResolver.insert(getReadWriteAudioCollection(), contentValues)
        } catch (e: IllegalArgumentException) {
            throw MediaStoreFormatException(filename, cause = e)
        }

        if (contentUri != null) {
            try {
                context.contentResolver.openOutputStream(contentUri, "w")?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            inputStream.transferTo(outputStream)
                        else outputStream.write(inputStream.readBytes())
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                context.contentResolver.update(contentUri, contentValues, null, null)
            } catch (e: Exception) {
                // Roll back:
                context.contentResolver.delete(contentUri, null, null)
                tempFile.delete()
                context.getMediaStoreFileNullable(contentUri)?.delete()
                throw e
            }
        } else throw Exception("Could not get content URI for $filename")

        tempFile.delete()
        return contentUri
    }

    private suspend fun tagTrack(track: Track, localFile: File, albumPojo: AbstractAlbumPojo? = null) {
        val tmpFile = File(context.cacheDir, "${localFile.nameWithoutExtension}.tmp.${localFile.extension}")
        val tags = mutableMapOf("title" to track.title)

        (track.artist ?: albumPojo?.album?.artist)?.let { tags["artist"] = it }
        albumPojo?.album?.artist?.let { tags["album_artist"] = it }
        albumPojo?.album?.title?.let { tags["album"] = it }
        track.albumPosition?.let { tags["track"] = it.toString() }
        (track.year ?: albumPojo?.album?.year)?.let { tags["date"] = it.toString() }

        val tagCommands = tags
            .map { (key, value) -> "-metadata \"$key=${value.escapeQuotes()}\"" }
            .joinToString(" ")
        val ffmpegCommand =
            "-i \"${localFile.path}\" -map 0 -y -codec copy -write_id3v2 1 $tagCommands \"${tmpFile.path}\""
        Log.i(javaClass.simpleName, "tagTrack: ffmpegCommand=$ffmpegCommand")
        val tagSession = FFmpegKit.execute(ffmpegCommand)
        if (tagSession.returnCode.isValueSuccess) {
            tmpFile.copyTo(localFile, overwrite = true)
            tmpFile.delete()
        }

        (track.image ?: albumPojo?.saveMediaStoreImage(context))?.getFile(context)?.let { imageFile ->
            // This does not work on Opus files yet, but there is a ticket about it somewhere:
            val ffmpegCommand2 = "-i \"${localFile.path}\" -i \"${imageFile.path}\" -map 0 -map 1:0 -y " +
                "-codec copy \"${tmpFile.path}\""
            val imageSession = FFmpegKit.execute(ffmpegCommand2)

            Log.i(javaClass.simpleName, "tagTrack: ffmpegCommand2=$ffmpegCommand2")
            if (imageSession.returnCode.isValueSuccess) {
                tmpFile.copyTo(localFile, overwrite = true)
                tmpFile.delete()
            }
        }
    }
}
