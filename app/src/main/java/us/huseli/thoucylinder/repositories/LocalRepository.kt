package us.huseli.thoucylinder.repositories

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import us.huseli.thoucylinder.ExtractTrackDataException
import us.huseli.thoucylinder.MediaStoreException
import us.huseli.thoucylinder.MediaStoreFormatException
import us.huseli.thoucylinder.TrackDownloadException
import us.huseli.thoucylinder.database.MusicDao
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.LocalAlbumData
import us.huseli.thoucylinder.dataclasses.LocalTrackData
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.deleteExistingMediaFile
import us.huseli.thoucylinder.escapeQuotes
import us.huseli.thoucylinder.extractTrackMetadata
import us.huseli.thoucylinder.getAudioCollection
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
) {
    private val _tempAlbums = MutableStateFlow<List<Album>>(emptyList())

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
    val singleTracks: Flow<List<Track>> = musicDao.listSingleTracks()
    val tempAlbums = _tempAlbums.asStateFlow()
    val tracks: Flow<List<Track>> = musicDao.listTracks()

    suspend fun addAlbumToLibrary(album: Album) = musicDao.upsertAlbumWithTracks(album)

    fun createTempAlbumFromYoutubePlaylist(playlist: YoutubePlaylist, videos: List<YoutubeVideo>): Album =
        playlist.toTempAlbum(videos = videos).also { _tempAlbums.value += it }

    suspend fun deleteAlbumWithTracks(album: Album) = musicDao.deleteAlbumWithTracks(album)

    suspend fun deleteAll() = musicDao.deleteAll()

    private fun deleteExistingMediaFile(filename: String, mediaStorePath: String?) =
        deleteExistingMediaFile(context, filename, mediaStorePath)

    suspend fun deleteLocalFiles(album: Album) {
        album.tracks.mapNotNull { it.localSubdirAndFilename }.forEach { (subdir, filename) ->
            deleteExistingMediaFile(filename, subdir)
        }
        musicDao.upsertAlbumWithTracks(album.copy(local = null, tracks = album.tracks.map { it.copy(local = null) }))
    }

    suspend fun moveAndInsertTrack(tempTrack: Track, progressCallback: (DownloadProgress) -> Unit) {
        val metadata = moveTrack(track = tempTrack, progressCallback = progressCallback)
        musicDao.insertTrack(tempTrack.copy(metadata = metadata, isInLibrary = true))
    }

    /**
     * tempAlbum.tracks should have tempTrackData set (done by e.g. YoutubeVideo.toTempTrack()), otherwise moveTrack()
     * will throw exception.
     */
    suspend fun moveAndSaveAlbum(album: Album, progressCallback: (DownloadProgress) -> Unit) {
        val mediaStorePath = album.getMediaStorePath()
        val tracks = mutableListOf<Track>()

        album.tracks.forEachIndexed { index, track ->
            val metadata = moveTrack(
                track = track,
                album = album,
                mediaStorePath = mediaStorePath,
                progressCallback = {
                    progressCallback(it.copy(progress = (index + it.progress) / album.tracks.size))
                }
            )
            tracks.add(
                track.copy(
                    metadata = metadata,
                    local = LocalTrackData(subdir = mediaStorePath),
                )
            )
        }
        musicDao.upsertAlbumWithTracks(
            album.copy(
                tracks = tracks,
                local = LocalAlbumData(mediaStorePath = mediaStorePath),
            )
        )
    }

    /**
     * @throws TrackDownloadException if tempTrack.tempTrackData.localFile is not set or File.extractTrackMetadata(),
     * moveFileToMediaStore(), or FFmpegKit.execute() fails.
     */
    private fun moveTrack(
        track: Track,
        album: Album? = null,
        mediaStorePath: String? = null,
        extraContentValues: ContentValues? = null,
        progressCallback: (DownloadProgress) -> Unit,
    ): TrackMetadata {
        val progress = DownloadProgress(status = DownloadProgress.Status.MOVING, progress = 0.0, item = track.title)
        val localFile =
            track.tempTrackData?.localFile ?: throw TrackDownloadException(TrackDownloadException.ErrorType.NO_FILE)
        val metadata = localFile.extractTrackMetadata()
        val filename = "${track.localBasename}.${metadata.extension}"

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$mediaStorePath")
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, metadata.mimeType)
            put(MediaStore.Audio.Media.DURATION, metadata.durationMs.toInt())
            put(MediaStore.Audio.Media.SIZE, localFile.length())
            put(MediaStore.Audio.Media.TITLE, track.title)
            track.albumPosition?.let { put(MediaStore.Audio.Media.TRACK, it.toString()) }
            track.artist?.let { put(MediaStore.Audio.Media.ARTIST, it) }
            track.metadata?.bitrate?.let { put(MediaStore.Audio.Media.BITRATE, it) }
            album?.let { put(MediaStore.Audio.Media.ALBUM, it.title) }
            album?.artist?.let { put(MediaStore.Audio.Media.ALBUM_ARTIST, it) }
            (track.year ?: album?.year)?.let { put(MediaStore.Audio.Media.YEAR, it) }
            extraContentValues?.let { putAll(it) }
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        progressCallback(progress)
        tagTrack(track = track, localFile = localFile, metadata = metadata, album = album)

        return try {
            moveFileToMediaStore(
                file = localFile,
                mediaStorePath = mediaStorePath,
                contentValues = contentValues,
                filename = filename,
            )
            progressCallback(progress.copy(progress = 1.0))
            metadata
        } catch (e: MediaStoreFormatException) {
            progressCallback(progress.copy(status = DownloadProgress.Status.CONVERTING))

            val convertedFile = File(localFile.path + ".opus")
            val session = FFmpegKit.execute("-i ${localFile.path} -vn ${convertedFile.path}")

            localFile.delete()
            if (!session.returnCode.isValueSuccess)
                throw TrackDownloadException(TrackDownloadException.ErrorType.FFMPEG_CONVERT)
            tagTrack(track = track, localFile = convertedFile, metadata = metadata, album = album)
            progressCallback(progress.copy(progress = 0.5))
            moveFileToMediaStore(
                file = convertedFile,
                mediaStorePath = mediaStorePath,
                contentValues = contentValues,
                filename = filename,
            )
            progressCallback(progress.copy(progress = 1.0))
            metadata
        } catch (e: MediaStoreException) {
            throw TrackDownloadException(TrackDownloadException.ErrorType.MEDIA_STORE, cause = e)
        } catch (e: ExtractTrackDataException) {
            throw TrackDownloadException(TrackDownloadException.ErrorType.EXTRACT_TRACK_DATA, cause = e)
        }
    }

    private fun tagTrack(track: Track, localFile: File, metadata: TrackMetadata, album: Album? = null) {
        val tmpFile = File(localFile.path + ".tmp")
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
            "-i \"${localFile.path}\" -map 0 -y -codec copy -write_id3v2 1 $tagCommands " +
                "-f ${metadata.extension} \"${tmpFile.path}\""
        )
        if (tagSession.returnCode.isValueSuccess) {
            localFile.delete()
            tmpFile.renameTo(localFile)
        }

        (track.image ?: album?.albumArt)?.let { image ->
            // This does not work on Opus files yet, but there is a ticket about it somewhere:
            val imageSession = FFmpegKit.execute(
                "-i \"${localFile.path}\" -i \"${image.localFile.path}\" -map 0 -map 1:0 -y -codec copy " +
                    "-f ${metadata.extension} \"${tmpFile.path}\""
            )
            if (imageSession.returnCode.isValueSuccess) {
                localFile.delete()
                tmpFile.renameTo(localFile)
            }
        }
    }

    /**
     * @throws MediaStoreFormatException
     */
    private fun moveFileToMediaStore(
        file: File,
        mediaStorePath: String?,
        filename: String,
        contentValues: ContentValues,
    ) {
        // If file already exists, just delete it first.
        deleteExistingMediaFile(filename, mediaStorePath)

        val trackUri = try {
            context.contentResolver.insert(getAudioCollection(), contentValues)
        } catch (e: IllegalArgumentException) {
            throw MediaStoreFormatException(filename)
        }

        if (trackUri != null) {
            context.contentResolver.openOutputStream(trackUri, "wt")?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    outputStream.write(inputStream.readBytes())
                }
            }
            file.delete()
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(trackUri, contentValues, null, null)
        } else throw MediaStoreException()
    }
}
