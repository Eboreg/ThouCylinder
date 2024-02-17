package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.baseName
import com.anggrayudi.storage.file.extension
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.isWritable
import com.anggrayudi.storage.file.toRawFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.retaintheme.extensions.padStart
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.copyTo
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.ID3Data
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.listCoverImages
import us.huseli.thoucylinder.dataclasses.entities.stripTitleCommons
import us.huseli.thoucylinder.dataclasses.extractID3Data
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import us.huseli.thoucylinder.escapeQuotes
import us.huseli.thoucylinder.getRelativePathWithoutFilename
import us.huseli.thoucylinder.getSquareSize
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    database: Database,
) {
    private val albumDao = database.albumDao()
    private val trackDao = database.trackDao()
    private val _isImportingLocalMedia = MutableStateFlow(false)

    val isImportingLocalMedia = _isImportingLocalMedia.asStateFlow()

    @WorkerThread
    fun createAlbumDirectory(album: Album): DocumentFile? =
        settingsRepo.getLocalMusicDirectory()?.let { album.createDirectory(it, context) }

    @WorkerThread
    fun getAlbumDirectory(album: Album): DocumentFile? =
        settingsRepo.getLocalMusicDirectory()?.let {
            album.getDirectory(it, context)
        }

    fun setIsImporting(value: Boolean) {
        _isImportingLocalMedia.value = value
    }

    /** IMAGE RELATED METHODS ************************************************/
    @WorkerThread
    suspend fun getBestNewLocalAlbumArt(combo: AlbumWithTracksCombo): MediaStoreImage? =
        combo.tracks.listCoverImages(context)
            .filter { it.uri != combo.album.albumArt?.uri }
            .map { MediaStoreImage.fromUri(it.uri, context) }
            .maxByOrNull { albumArt -> albumArt.getFullImageBitmap(context)?.getSquareSize() ?: 0 }

    @WorkerThread
    fun collectNewLocalAlbumArtUris(combo: AlbumWithTracksCombo): List<Uri> = combo.tracks.listCoverImages(context)
        .map { it.uri }
        .filter { it != combo.album.albumArt?.uri }

    @WorkerThread
    fun deleteAlbumDirectoryAlbumArt(album: Album) {
        getAlbumDirectory(album)?.also {
            album.albumArt?.deleteDirectoryFiles(context, it)
        }
    }

    @WorkerThread
    fun deleteAlbumDirectoryAlbumArt(combo: AlbumWithTracksCombo) {
        deleteAlbumDirectoryAlbumArt(combo.album)
        combo.tracks.listCoverImages(context, includeThumbnails = true).forEach { documentFile ->
            if (documentFile.isFile && documentFile.canWrite()) documentFile.delete()
        }
    }

    @WorkerThread
    suspend fun saveInternalAlbumArtFiles(image: MediaStoreImage, album: Album): MediaStoreImage? =
        image.saveInternal(album, context)


    /** AUDIO RELATED METHODS ************************************************/
    suspend fun importNewLocalAlbums(treeDocumentFile: DocumentFile, existingTrackUris: Collection<Uri>) {
        val albums = mutableSetOf<Album>()
        val tracks = mutableListOf<Track>()
        val getAlbum: suspend (ID3Data, String?, String) -> Album = { id3, defaultArtist, defaultTitle ->
            val artist = id3.albumArtist ?: defaultArtist
            val title = id3.album ?: defaultTitle
            val album = albums.find {
                (it.title == title && it.artist == artist) ||
                    (it.musicBrainzReleaseId != null && it.musicBrainzReleaseId == id3.musicBrainzReleaseId)
            }

            if (album != null) {
                if (
                    (id3.musicBrainzReleaseId != null && album.musicBrainzReleaseId == null) ||
                    (id3.musicBrainzReleaseGroupId != null && album.musicBrainzReleaseGroupId == null)
                ) {
                    album.copy(
                        musicBrainzReleaseId = id3.musicBrainzReleaseId ?: album.musicBrainzReleaseId,
                        musicBrainzReleaseGroupId = id3.musicBrainzReleaseGroupId
                            ?: album.musicBrainzReleaseGroupId,
                    ).also { albumDao.updateAlbums(it) }
                } else album
            } else Album(
                title = title,
                artist = artist,
                isInLibrary = true,
                isLocal = true,
                musicBrainzReleaseId = id3.musicBrainzReleaseId,
                musicBrainzReleaseGroupId = id3.musicBrainzReleaseGroupId,
            ).also {
                albums.add(it)
                albumDao.insertAlbums(it)
            }
        }

        treeDocumentFile.listFiles().forEach { documentFile ->
            if (documentFile.isDirectory) {
                // Go through subdirectories recursively:
                importNewLocalAlbums(documentFile, existingTrackUris)
            } else if (documentFile.isFile && !existingTrackUris.contains(documentFile.uri)) {
                // TODO: Seems like FFprobeKit doesn't get permission to access the file even though we have read/write
                // permissions through the DocumentFile API. So we do this stupid copy to temp file shit until we have
                // a better solution.
                val mimeTypeGuess = MimeTypeMap.getSingleton().getMimeTypeFromExtension(documentFile.extension)

                // Just to avoid copying lots of irrelevant files:
                if (mimeTypeGuess?.startsWith("audio/") == true) {
                    val tempFile = documentFile.copyTo(context, context.cacheDir).also { it.deleteOnExit() }
                    val mediaInfo = FFprobeKit.getMediaInformation(tempFile.path)?.mediaInformation
                    val metadata = tempFile.extractTrackMetadata(mediaInfo)

                    if (metadata?.mimeType?.startsWith("audio/") == true) {
                        // Make an educated guess on artist/album names from path. Segment 1 is most likely a standard
                        // directory like "Music", so drop it:
                        val pathSegments = documentFile.uri.getRelativePathWithoutFilename()?.split('/')?.drop(1)
                        val id3 = mediaInfo?.extractID3Data() ?: ID3Data()
                        val (pathArtist, pathTitle) = when (pathSegments?.size) {
                            1 -> pathSegments[0].split(Regex(" +- +"), 2).padStart(2).toList()
                            0 -> listOf(null, null)
                            null -> listOf(null, null)
                            else -> pathSegments.takeLast(2)
                        }
                        val (filenameAlbumPosition, filenameTitle) =
                            Regex("^(\\d+)?[ -.]*(.*)$").find(documentFile.baseName)
                                ?.groupValues
                                ?.takeLast(2)
                                ?.map { it.nullIfEmpty() }
                                ?: listOf(null, null)
                        val track = Track(
                            title = id3.title ?: filenameTitle ?: context.getString(R.string.unknown_title),
                            isInLibrary = true,
                            artist = id3.artist ?: pathArtist,
                            albumPosition = id3.trackNumber ?: filenameAlbumPosition?.toIntOrNull(),
                            discNumber = id3.discNumber,
                            year = id3.year,
                            albumId = getAlbum(
                                id3,
                                pathArtist,
                                pathTitle ?: context.getString(R.string.unknown_album)
                            ).albumId,
                            metadata = metadata,
                            localUri = documentFile.uri,
                            musicBrainzId = id3.musicBrainzTrackId,
                        )

                        trackDao.insertTracks(track)
                        tracks.add(track)
                    }
                }
            }
        }

        albums.forEach { album ->
            val combo = AlbumWithTracksCombo(
                album = album,
                tracks = tracks.filter { it.albumId == album.albumId }.stripTitleCommons(),
            )
            val albumArt = getBestNewLocalAlbumArt(combo)

            if (albumArt != null) albumDao.updateAlbumArt(album.albumId, albumArt)
        }
    }

    @WorkerThread
    fun listOrphanTracks(allTracks: Collection<Track>): List<Track> {
        /** Collect tracks that have no existing media files. */
        return allTracks.filterNot { track ->
            track.localUri?.let { DocumentFile.fromSingleUri(context, it)?.exists() } ?: false
        }
    }

    @WorkerThread
    fun tagAlbumTracks(combo: AlbumWithTracksCombo) {
        combo.tracks.forEach { track -> tagTrack(track, combo) }
    }

    @WorkerThread
    fun tagTrack(track: Track, albumCombo: AbstractAlbumCombo? = null) {
        val documentFile = track.getDocumentFile(context)

        if (documentFile != null) {
            if (!documentFile.isWritable(context))
                Log.e(this::class.simpleName, "tagAlbumTracks: Cannot write to $documentFile")
            else
                tagTrack(track = track, documentFile = documentFile, albumCombo = albumCombo)
        }
    }

    @WorkerThread
    fun tagTrack(track: Track, documentFile: DocumentFile, albumCombo: AbstractAlbumCombo? = null) {
        val rawFile = documentFile.toRawFile(context) ?: throw Exception("Could not convert $documentFile to raw File")
        val path = documentFile.getAbsolutePath(context)
        val tmpFile = File(context.cacheDir, "${documentFile.baseName}.tmp.${documentFile.extension}")
        val tagCommands = getTagMap(track, albumCombo)
            .map { (key, value) -> "-metadata \"$key=${value.escapeQuotes()}\"" }
            .joinToString(" ")
        val ffmpegCommand =
            "-i \"$path\" -map 0 -y -codec copy -write_id3v2 1 $tagCommands \"${tmpFile.path}\""
        Log.i(javaClass.simpleName, "tagTrack: ffmpegCommand=$ffmpegCommand")

        val tagSession = FFmpegKit.execute(ffmpegCommand)

        if (tagSession.returnCode.isValueSuccess) {
            tmpFile.copyTo(rawFile, overwrite = true)
            tmpFile.delete()
        }
    }


    /** PRIVATE METHODS *******************************************************/
    private fun getTagMap(track: Track, albumCombo: AbstractAlbumCombo? = null): Map<String, String> {
        val tags = mutableMapOf("title" to track.title)

        (track.artist ?: albumCombo?.album?.artist)?.also { tags["artist"] = it }
        albumCombo?.album?.artist?.also { tags["album_artist"] = it }
        albumCombo?.album?.title?.also { tags["album"] = it }
        track.albumPosition?.also { tags["track"] = it.toString() }
        (track.year ?: albumCombo?.album?.year)?.also { tags["date"] = it.toString() }
        track.musicBrainzId?.also { tags["mb_track_id"] = it }
        albumCombo?.album?.musicBrainzReleaseId?.also { tags["mb_release_id"] = it }
        albumCombo?.album?.musicBrainzReleaseGroupId?.also { tags["mb_release_group_id"] = it }

        return tags
    }
}
