package us.huseli.thoucylinder.repositories

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.arthenica.ffmpegkit.FFmpegKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.thoucylinder.ExtractTrackDataException
import us.huseli.thoucylinder.MediaStoreException
import us.huseli.thoucylinder.MediaStoreFormatException
import us.huseli.thoucylinder.TrackDownloadException
import us.huseli.thoucylinder.database.MusicDao
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.MediaStoreData
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.extractID3Data
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import us.huseli.thoucylinder.deleteExistingMediaFile
import us.huseli.thoucylinder.escapeQuotes
import us.huseli.thoucylinder.getMediaStoreFile
import us.huseli.thoucylinder.getReadOnlyAudioCollection
import us.huseli.thoucylinder.getReadOnlyImageCollection
import us.huseli.thoucylinder.getReadWriteAudioCollection
import us.huseli.thoucylinder.getReadWriteImageCollection
import us.huseli.thoucylinder.loadThumbnailOrNull
import us.huseli.thoucylinder.toBitmap
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


data class MediaStoreEntry(val uri: Uri, val file: File, val track: Track)

data class ImportedImage(
    val bitmap: Bitmap,
    val file: File? = null,
    val relativePath: String? = null,
    val width: Int = bitmap.width,
    val height: Int = bitmap.height,
) {
    val size: Int
        get() = width * height

    /**
     * True if `paths` contains this.relativePath or at least one descendant of it.
     */
    fun matchesPaths(paths: Collection<String>): Boolean {
        if (relativePath == null) return false
        return paths.any { it.startsWith(relativePath) }
    }
}

@Singleton
class LocalRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
) {
    private val _albumArtAbsoluteDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "ThouCylinder/albumArt",
    )
    private val _albumArtRelativePath = "${Environment.DIRECTORY_PICTURES}/ThouCylinder/albumArt"
    private val _tempAlbums = MutableStateFlow<Map<UUID, Album>>(emptyMap())
    private val _imageCache = mutableMapOf<Image, ImageBitmap?>()
    private val _imageCacheMutex = Mutex()

    val libraryAlbums: Flow<List<Album>> = combine(
        musicDao.listAlbumsWithTracks(),
        musicDao.listAbumGenres(),
        musicDao.listAlbumStyles()
    ) { multimap, albumGenres, albumStyles ->
        multimap.map { (album, tracks) ->
            album.copy(
                tracks = tracks,
                genres = albumGenres.filter { it.albumId == album.albumId }.map { it.genreId },
                styles = albumStyles.filter { it.albumId == album.albumId }.map { it.styleId },
            )
        }
    }
    val tempAlbums = _tempAlbums.asStateFlow()
    val tracks: Flow<List<Track>> = combine(musicDao.listTracks(), libraryAlbums) { tracks, albums ->
        tracks.map { track ->
            val album = albums.find { it.albumId == track.albumId }
            track.copy(album = album, artist = track.artist ?: album?.artist)
        }
    }

    fun addOrUpdateTempAlbum(album: Album) {
        _tempAlbums.value += album.albumId to album
    }

    fun collectArtistImages(): Map<String, Image> {
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("artist.%")

        return collectImages(selection, selectionArgs).associate { image ->
            image.relativePath!!.trim('/').split('/').last() to
                Image(localFile = image.file!!, width = image.width, height = image.height)
        }
    }

    suspend fun deleteAlbumWithTracks(album: Album) = musicDao.deleteAlbumWithTracks(album)

    suspend fun deleteAll() = musicDao.deleteAll()

    suspend fun deleteOrphanTracksAndAlbums() {
        tracks.transformWhile { tracks ->
            emit(tracks.filter { !it.isOnYoutube })
            false
        }.collect { tracks ->
            // Collect tracks that have no Youtube connection and no existing media files:
            val orphanTracks = tracks.filterNot { track ->
                track.mediaStoreData?.uri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        true
                    } catch (_: FileNotFoundException) {
                        false
                    }
                } ?: false
            }
            // And albums that _only_ have orphan tracks in them:
            val orphanAlbums = orphanTracks
                .mapNotNull { it.album }
                .toSet()
                .filter { album -> orphanTracks.map { it.id }.containsAll(album.tracks.map { it.id }) }
            musicDao.deleteTracks(*orphanTracks.toTypedArray())
            musicDao.deleteAlbums(*orphanAlbums.toTypedArray())
        }
    }

    suspend fun getImageBitmap(image: Image): ImageBitmap? {
        return _imageCacheMutex.withLock {
            if (!_imageCache.containsKey(image)) {
                _imageCache += image to image.getImageBitmap()
            }
            _imageCache[image]
        }
    }

    suspend fun importNewMediaStoreAlbums() {
        val audioCollection = getReadOnlyAudioCollection()
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
        )

        tracks.transformWhile { tracks ->
            emit(tracks)
            false
        }.collect { existingTracks ->
            val mediaStoreUris = existingTracks.mapNotNull { it.mediaStoreData?.uri }
            val albums = mutableListOf<Album>()
            val tracks = mutableListOf<Pair<String, Track>>()

            context.contentResolver.query(audioCollection, projection, null, null)?.use { cursor ->
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
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

                while (cursor.moveToNext()) {
                    cursor.getStringOrNull(dataIdx)?.let { filename ->
                        val file = File(filename)
                        val mimeType = cursor.getStringOrNull(mimeTypeIdx)
                        val relativePath = cursor.getStringOrNull(relativePathIdx)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            cursor.getLong(idIdx),
                        )
                        val id3 = file.extractID3Data()

                        if (
                            mimeType != null &&
                            relativePath != null &&
                            mimeType.startsWith("audio/") &&
                            file.isFile &&
                            !mediaStoreUris.contains(contentUri)
                        ) {
                            val lastPathSegments = relativePath
                                .replace(Regex("^${Environment.DIRECTORY_MUSIC}/(.*?)/?$"), "$1")
                                .trim('/').split("/").last().split(" - ", limit = 2)
                            val pathArtist = lastPathSegments.takeIf { it.size > 1 }?.get(0)
                            val pathTitle = lastPathSegments.last().takeIf { it.isNotBlank() }
                            val trackArtist =
                                cursor.getStringOrNull(artistIdx)?.takeIf { it != "<unknown>" } ?: id3.artist
                            val albumArtist =
                                cursor.getStringOrNull(albumArtistIdx)?.takeIf { it != "<unknown>" } ?: id3.albumArtist
                            val albumTitle = cursor.getStringOrNull(albumIdx) ?: id3.album ?: pathTitle
                            val finalAlbumArtist = albumArtist ?: pathArtist ?: trackArtist
                            val finalAlbumTitle = albumTitle ?: "Unknown album"

                            val album =
                                albums.find { it.artist == finalAlbumArtist && it.title == finalAlbumTitle } ?: Album(
                                    title = finalAlbumTitle,
                                    artist = finalAlbumArtist,
                                    isInLibrary = false,
                                    isLocal = true,
                                ).also { albums.add(it) }

                            tracks.add(
                                Pair(
                                    relativePath,
                                    Track(
                                        title = cursor.getStringOrNull(titleIdx) ?: id3.title ?: "Unknown title",
                                        isInLibrary = false,
                                        artist = trackArtist ?: finalAlbumArtist,
                                        albumPosition = cursor.getIntOrNull(trackIdx) ?: id3.trackNumber,
                                        year = cursor.getIntOrNull(yearIdx) ?: id3.year,
                                        albumId = album.albumId,
                                        metadata = file.extractTrackMetadata().copy(
                                            durationMs = cursor.getIntOrNull(durationIdx)?.toLong() ?: 0L,
                                            extension = filename.split(".").last(),
                                            mimeType = mimeType,
                                            size = file.length(),
                                        ),
                                        mediaStoreData = MediaStoreData(uri = contentUri),
                                    )
                                )
                            )
                        }
                    }
                }
            }

            val relativePaths = tracks.map { it.first }.toSet()
            val images = collectAlbumArt(relativePaths)

            albums.forEach { album ->
                val (albumRelativePaths, albumTracks) = tracks
                    .filter { it.second.albumId == album.albumId }
                    .sortedBy { it.second.albumPosition }
                    .unzip()
                val albumImages = images
                    .filter { it.matchesPaths(albumRelativePaths) }
                    .toMutableList()
                val thumbnail = albumTracks.firstNotNullOfOrNull { track ->
                    track.mediaStoreData?.uri?.let { uri ->
                        context.contentResolver.loadThumbnailOrNull(uri, Size(1000, 1000), null)
                    }
                }
                if (thumbnail != null) albumImages.add(ImportedImage(bitmap = thumbnail))
                val albumArt = albumImages.maxByOrNull { it.size }?.let {
                    Image(
                        localFile = saveAlbumArtToDisk(it.bitmap, album),
                        width = it.width,
                        height = it.height,
                    )
                }

                saveAlbum(
                    album.copy(
                        tracks = albumTracks.map { it.copy(image = albumArt) },
                        albumArt = albumArt,
                    )
                )
            }
        }
    }

    suspend fun insertTrack(track: Track) = musicDao.insertTrack(track = track)

    /**
     * album.tracks should have tempTrackData set (done by e.g. YoutubeVideo.toTempTrack()), or exception is thrown.
     */
    fun moveTaggedAlbumToMediaStore(album: Album, progressCallback: (DownloadProgress) -> Unit): Album {
        val tracks = album.tracks.mapIndexed { index, track ->
            val entry = moveTrackToMediaStore(
                track = track,
                subdir = album.getMediaStoreSubdir(),
                progressCallback = {
                    progressCallback(it.copy(progress = (index + it.progress) / album.tracks.size))
                },
            )
            val metadata = entry.file.extractTrackMetadata()

            context.contentResolver.update(entry.uri, getTrackContentValues(entry.track, metadata, album), null, null)
            tagTrack(track = entry.track, localFile = entry.file, album = album)
            track.copy(
                metadata = metadata,
                mediaStoreData = MediaStoreData(uri = entry.uri),
                isInLibrary = true,
                tempTrackData = null,
                albumId = album.albumId,
            )
        }

        return album.copy(tracks = tracks, isLocal = true)
    }

    fun moveTaggedTrackToMediaStore(track: Track, progressCallback: (DownloadProgress) -> Unit): Track {
        val entry = moveTrackToMediaStore(track = track, progressCallback = progressCallback)
        val metadata = entry.file.extractTrackMetadata()

        tagTrack(track = track, localFile = entry.file)
        context.contentResolver.update(entry.uri, getTrackContentValues(track, metadata), null, null)

        return track.copy(
            metadata = metadata,
            isInLibrary = true,
            tempTrackData = null,
            mediaStoreData = MediaStoreData(uri = entry.uri),
        )
    }

    suspend fun saveAlbum(album: Album) = musicDao.upsertAlbumWithTracks(album)

    suspend fun tagAndUpdateAlbumWithTracks(album: Album) {
        saveAlbum(album)
        album.tracks.forEach { track ->
            val trackFile = getFileFromTrack(track)

            if (trackFile != null && track.metadata != null) {
                tagTrack(track = track, localFile = trackFile, album = album)
            }
            track.mediaStoreData?.uri?.also { uri ->
                val contentValues = getTrackContentValues(track, track.metadata, album)
                context.contentResolver.update(uri, contentValues, null, null)
            }
        }
    }

    /** PRIVATE METHODS ******************************************************/

    private fun collectAlbumArt(mediaStoreSubdirs: Collection<String>): List<ImportedImage> {
        val relativePathSet = mediaStoreSubdirs.map { "$it%" }.toSet()
        val pathSelectors = relativePathSet.map { "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" }
        var selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        if (pathSelectors.isNotEmpty())
            selection += " AND (${pathSelectors.joinToString(" OR ")})"
        val selectionArgs = arrayOf("cover.%", *relativePathSet.toTypedArray())

        return collectImages(selection, selectionArgs)
    }

    private fun collectImages(selection: String, selectionArgs: Array<String>): List<ImportedImage> {
        val imageCollection = getReadOnlyImageCollection()
        val projection = arrayOf(
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
        )
        val images = mutableListOf<ImportedImage>()

        try {
            context.contentResolver.query(imageCollection, projection, selection, selectionArgs, null)?.use { cursor ->
                val relativePathIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    cursor.getStringOrNull(dataIdx)?.let { filename ->
                        val file = File(filename)
                        val relativePath = cursor.getStringOrNull(relativePathIdx)
                        val bitmap = file.toBitmap()

                        if (relativePath != null && bitmap != null) {
                            images.add(ImportedImage(file = file, relativePath = relativePath, bitmap = bitmap))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalRepository", "collectImages: $e", e)
        }
        return images
    }

    private fun deleteExistingMediaFile(filename: String, mediaStorePath: String?) =
        context.deleteExistingMediaFile(filename, mediaStorePath)

    private fun getTrackContentValues(track: Track, metadata: TrackMetadata? = null, album: Album? = null) =
        ContentValues().apply {
            album?.getContentValues()?.also { putAll(it) }
            (metadata ?: track.metadata)?.getContentValues()?.let { putAll(it) }
            putAll(track.getContentValues())
        }

    /** Does not check if file exists. */
    private fun getFileFromTrack(track: Track): File? =
        track.tempTrackData?.localFile ?: track.mediaStoreData?.getFile(context)

    /**
     * @throws MediaStoreFormatException
     */
    private fun moveMusicFileToMediaStore(localFile: File, filename: String, subdir: String = ""): Uri {
        // If file already exists, just delete it first.
        deleteExistingMediaFile(filename, subdir)

        val relativePath = "${Environment.DIRECTORY_MUSIC}/$subdir"
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.SIZE, localFile.length())
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val trackUri = try {
            context.contentResolver.insert(getReadWriteAudioCollection(), contentValues)
        } catch (e: IllegalArgumentException) {
            throw MediaStoreFormatException(filename)
        }

        if (trackUri != null) {
            context.contentResolver.openOutputStream(trackUri, "w")?.use { outputStream ->
                localFile.inputStream().use { inputStream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        inputStream.transferTo(outputStream)
                    else outputStream.write(inputStream.readBytes())
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(trackUri, contentValues, null, null)
        } else throw MediaStoreException()

        localFile.delete()
        return trackUri
    }

    private fun moveTrackToMediaStore(
        track: Track,
        subdir: String = "",
        progressCallback: (DownloadProgress) -> Unit,
    ): MediaStoreEntry {
        val getFilename = { extension: String -> "${track.generateBasename()}.$extension" }
        val progress = DownloadProgress(status = DownloadProgress.Status.MOVING, progress = 0.0, item = track.title)
        val localFile = getFileFromTrack(track)
            ?: throw TrackDownloadException(TrackDownloadException.ErrorType.NO_FILE)

        progressCallback(progress)

        return try {
            val uri = moveMusicFileToMediaStore(
                localFile = localFile,
                subdir = subdir,
                filename = getFilename(localFile.extension),
            )
            progressCallback(progress.copy(progress = 1.0))
            MediaStoreEntry(uri = uri, file = context.getMediaStoreFile(uri), track = track)
        } catch (e: MediaStoreFormatException) {
            progressCallback(progress.copy(status = DownloadProgress.Status.CONVERTING))

            val convertedFile = File(localFile.path.substringBeforeLast('.') + ".opus")
            val session = FFmpegKit.execute("-i ${localFile.path} -vn ${convertedFile.path}")

            localFile.delete()
            if (!session.returnCode.isValueSuccess)
                throw TrackDownloadException(TrackDownloadException.ErrorType.FFMPEG_CONVERT)
            progressCallback(progress.copy(progress = 0.5))
            val uri = moveMusicFileToMediaStore(
                localFile = convertedFile,
                subdir = subdir,
                filename = getFilename("opus"),
            )
            progressCallback(progress.copy(progress = 1.0))
            MediaStoreEntry(uri = uri, file = context.getMediaStoreFile(uri), track = track)
        } catch (e: MediaStoreException) {
            throw TrackDownloadException(TrackDownloadException.ErrorType.MEDIA_STORE, cause = e)
        } catch (e: ExtractTrackDataException) {
            throw TrackDownloadException(TrackDownloadException.ErrorType.EXTRACT_TRACK_DATA, cause = e)
        }
    }

    private fun saveAlbumArtToDisk(bitmap: Bitmap, album: Album): File {
        val filename = "${album.albumId}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, _albumArtRelativePath)
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val imageUri = context.contentResolver.insert(getReadWriteImageCollection(), contentValues)

        checkNotNull(imageUri)
        context.contentResolver.openOutputStream(imageUri, "w")?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        return File(_albumArtAbsoluteDir, filename)
    }

    private fun tagTrack(track: Track, localFile: File, album: Album? = null) {
        val tmpFile = File(context.cacheDir, "${localFile.nameWithoutExtension}.tmp.${localFile.extension}")
        val tags = mutableMapOf("title" to track.title)

        (track.artist ?: album?.artist)?.let { tags["artist"] = it }
        album?.artist?.let { tags["album_artist"] = it }
        album?.title?.let { tags["album"] = it }
        track.albumPosition?.let { tags["track"] = it.toString() }
        (track.year ?: album?.year)?.let { tags["date"] = it.toString() }

        val tagCommands = tags
            .map { (key, value) -> "-metadata \"$key=${value.escapeQuotes()}\"" }
            .joinToString(" ")
        val tagSession = FFmpegKit.execute(
            "-i \"${localFile.path}\" -map 0 -y -codec copy -write_id3v2 1 $tagCommands \"${tmpFile.path}\""
        )
        if (tagSession.returnCode.isValueSuccess) {
            tmpFile.copyTo(localFile, overwrite = true)
            tmpFile.delete()
        }

        (track.image ?: album?.albumArt)?.let { image ->
            // This does not work on Opus files yet, but there is a ticket about it somewhere:
            val imageSession = FFmpegKit.execute(
                "-i \"${localFile.path}\" -i \"${image.localFile.path}\" -map 0 -map 1:0 -y -codec copy " +
                    "\"${tmpFile.path}\""
            )
            if (imageSession.returnCode.isValueSuccess) {
                tmpFile.copyTo(localFile, overwrite = true)
                tmpFile.delete()
            }
        }
    }
}
