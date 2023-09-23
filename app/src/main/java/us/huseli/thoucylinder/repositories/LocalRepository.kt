package us.huseli.thoucylinder.repositories

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.ExtractTrackDataException
import us.huseli.thoucylinder.MediaStoreException
import us.huseli.thoucylinder.MediaStoreFormatException
import us.huseli.thoucylinder.TrackDownloadException
import us.huseli.thoucylinder.database.dao.AlbumDao
import us.huseli.thoucylinder.database.dao.TrackDao
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.TempAlbum
import us.huseli.thoucylinder.dataclasses.TempTrack
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.deleteExistingMediaFile
import us.huseli.thoucylinder.extrackTrackMetadata
import us.huseli.thoucylinder.getAudioCollection
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumDao: AlbumDao,
    private val trackDao: TrackDao,
) {
    val albums: Flow<List<Album>> = albumDao.listWithTracks().map { albumMap ->
        albumMap.map { (album, tracks) ->
            album.copy(tracks = tracks)
        }
    }
    val singleTracks: Flow<List<Track>> = trackDao.listSingle()
    val tracks: Flow<List<Track>> = trackDao.list()

    fun getLocalTrackUri(track: Track?): Uri? = track?.localUri

    suspend fun moveAndInsertTrack(tempTrack: TempTrack, progressCallback: (DownloadProgress) -> Unit) {
        val metadata = moveTrack(tempTrack = tempTrack, progressCallback = progressCallback)
        trackDao.insert(tempTrack.toTrack(metadata = metadata))
    }

    suspend fun moveAndInsertAlbum(
        tempAlbum: TempAlbum,
        progressCallback: (DownloadProgress) -> Unit,
    ) {
        val subdirName = tempAlbum.subdirName
        val tracks = mutableListOf<Track>()
        val albumId = UUID.randomUUID()

        tempAlbum.tracks.forEachIndexed { index, tempTrack ->
            val metadata = moveTrack(
                tempTrack = tempTrack,
                subdirName = subdirName,
                extraContentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.ALBUM, tempAlbum.title)
                    (tempTrack.artist ?: tempAlbum.artist)?.let { put(MediaStore.Audio.Media.ARTIST, it) }
                },
                progressCallback = {
                    progressCallback(it.copy(progress = (index + it.progress) / tempAlbum.tracks.size))
                }
            )
            tracks.add(
                tempTrack.toTrack(
                    metadata = metadata,
                    localSubdir = subdirName,
                    albumId = albumId,
                )
            )
        }

        albumDao.insertWithTracks(
            Album(
                id = albumId,
                title = tempAlbum.title,
                artist = tempAlbum.artist,
                youtubePlaylist = tempAlbum.youtubePlaylist,
                albumArt = tempAlbum.albumArt,
                tracks = tracks,
            )
        )
    }

    private fun moveTrack(
        tempTrack: TempTrack,
        subdirName: String = "",
        extraContentValues: ContentValues? = null,
        progressCallback: (DownloadProgress) -> Unit,
    ): TrackMetadata {
        val progress = DownloadProgress(status = DownloadProgress.Status.MOVING, progress = 0.0, item = tempTrack.title)

        progressCallback(progress)
        return try {
            moveFileToMediaStore(
                file = tempTrack.localFile,
                subdirName = subdirName,
                basename = tempTrack.basename,
                extraContentValues = extraContentValues,
            ).also { progressCallback(progress.copy(progress = 1.0)) }
        } catch (e: MediaStoreFormatException) {
            progressCallback(progress.copy(status = DownloadProgress.Status.CONVERTING))
            val session = FFmpegKit.execute("-i ${tempTrack.localFile.path} -vn ${tempTrack.localFile.path}.opus")
            tempTrack.localFile.delete()
            if (!session.returnCode.isValueSuccess)
                throw TrackDownloadException(TrackDownloadException.ErrorType.FFMPEG_CONVERT)
            progressCallback(progress.copy(progress = 0.5))
            moveFileToMediaStore(
                file = File(tempTrack.localFile.path + ".opus"),
                subdirName = subdirName,
                basename = tempTrack.basename,
                extraContentValues = extraContentValues,
            ).also { progressCallback(progress.copy(progress = 1.0)) }
        } catch (e: MediaStoreException) {
            throw TrackDownloadException(TrackDownloadException.ErrorType.MEDIA_STORE, cause = e)
        } catch (e: ExtractTrackDataException) {
            throw TrackDownloadException(TrackDownloadException.ErrorType.EXTRACT_TRACK_DATA, cause = e)
        }
    }

    /**
     * @throws MediaStoreFormatException
     */
    private fun moveFileToMediaStore(
        file: File,
        subdirName: String,
        basename: String,
        extraContentValues: ContentValues? = null,
    ): TrackMetadata {
        val metadata = file.extrackTrackMetadata()
        val filename = "$basename.${metadata.extension}"

        // If file already exists, just delete it first.
        deleteExistingMediaFile(context, filename, subdirName)

        val trackDetails = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$subdirName")
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, metadata.mimeType)
            extraContentValues?.let { putAll(it) }
            put(MediaStore.Audio.Media.DURATION, metadata.durationMs.toInt())
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val trackUri = try {
            context.contentResolver.insert(getAudioCollection(), trackDetails)
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
            trackDetails.clear()
            trackDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(trackUri, trackDetails, null, null)
            return metadata
        }
        throw MediaStoreException()
    }
}
