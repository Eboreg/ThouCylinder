package us.huseli.thoucylinder.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.baseName
import com.anggrayudi.storage.file.extension
import com.anggrayudi.storage.file.fullName
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.isWritable
import com.anggrayudi.storage.file.openInputStream
import com.anggrayudi.storage.file.toRawFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.ID3Data
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.extractID3Data
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.escapeQuotes
import us.huseli.thoucylinder.getRelativePathWithoutFilename
import us.huseli.thoucylinder.nullIfEmpty
import us.huseli.thoucylinder.padStart
import us.huseli.thoucylinder.toBitmap
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
) {
    private val _isImportingLocalMedia = MutableStateFlow(false)

    val isImportingLocalMedia = _isImportingLocalMedia.asStateFlow()

    fun setIsImporting(value: Boolean) {
        _isImportingLocalMedia.value = value
    }

    /** IMAGE RELATED METHODS ************************************************/
    @WorkerThread
    fun collectAlbumImages(pojo: AlbumWithTracksPojo): List<Bitmap> =
        pojo.listCoverImages(context).mapNotNull { it.toBitmap(context) }


    /** AUDIO RELATED METHODS ************************************************/
    @WorkerThread
    fun listNewLocalAlbums(existingTrackUris: Collection<Uri>): List<AlbumWithTracksPojo> {
        return settingsRepo.musicImportUri.value
            ?.let { DocumentFile.fromTreeUri(context, it) }
            ?.let { listNewLocalAlbums(it, existingTrackUris) }
            ?: emptyList()
    }

    @WorkerThread
    fun listOrphanTracks(allTracks: Collection<Track>): List<Track> {
        /** Collect tracks that have no existing media files. */
        return allTracks.filterNot { track ->
            track.localUri?.let { DocumentFile.fromSingleUri(context, it)?.exists() } ?: false
        }
    }

    @WorkerThread
    fun tagAlbumTracks(pojo: AlbumWithTracksPojo) {
        pojo.tracks.forEach { track ->
            val documentFile = track.getDocumentFile(context)

            if (documentFile != null) {
                if (!documentFile.isWritable(context))
                    Log.e(this::class.simpleName, "tagAlbumTracks: Cannot write to $documentFile")
                else
                    tagTrack(track = track, documentFile = documentFile, albumPojo = pojo)
            }
        }
    }

    @WorkerThread
    fun tagTrack(track: Track, documentFile: DocumentFile, albumPojo: AbstractAlbumPojo? = null) {
        val rawFile = documentFile.toRawFile(context) ?: throw Error("Could not convert $documentFile to raw File")
        val path = documentFile.getAbsolutePath(context)
        val tmpFile = File(context.cacheDir, "${documentFile.baseName}.tmp.${documentFile.extension}")
        val tagCommands = getTagMap(track, albumPojo)
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
    private fun getTagMap(track: Track, albumPojo: AbstractAlbumPojo? = null): Map<String, String> {
        val tags = mutableMapOf("title" to track.title)

        (track.artist ?: albumPojo?.album?.artist)?.let { tags["artist"] = it }
        albumPojo?.album?.artist?.let { tags["album_artist"] = it }
        albumPojo?.album?.title?.let { tags["album"] = it }
        track.albumPosition?.let { tags["track"] = it.toString() }
        (track.year ?: albumPojo?.album?.year)?.let { tags["date"] = it.toString() }

        return tags
    }

    @WorkerThread
    private fun listNewLocalAlbums(
        treeDocumentFile: DocumentFile,
        existingTrackUris: Collection<Uri>,
    ): List<AlbumWithTracksPojo> {
        val albums = mutableSetOf<Album>()
        val tracks = mutableListOf<Track>()
        val albumPojos = mutableListOf<AlbumWithTracksPojo>()

        treeDocumentFile.listFiles().forEach { documentFile ->
            if (documentFile.isDirectory) {
                // Go through subdirectories recursively:
                albumPojos.addAll(listNewLocalAlbums(documentFile, existingTrackUris))
            } else if (documentFile.isFile && !existingTrackUris.contains(documentFile.uri)) {
                /**
                 * TODO: Seems like FFprobeKit doesn't get permission to access
                 * the file even though we have read/write permissions through
                 * the DocumentFile API. So we do this stupid copy to temp file
                 * shit until we have a better solution.
                 */
                val mimeTypeGuess = MimeTypeMap.getSingleton().getMimeTypeFromExtension(documentFile.extension)

                // Just to avoid copying lots of irrelevant files:
                if (mimeTypeGuess?.startsWith("audio/") == true) {
                    val tempFile = File(context.cacheDir, documentFile.fullName).also {
                        it.outputStream().use { outputStream ->
                            documentFile.openInputStream(context)?.use { inputStream ->
                                outputStream.write(inputStream.readBytes())
                            }
                        }
                        it.deleteOnExit()
                    }
                    val ff = FFprobeKit.getMediaInformation(tempFile.path)?.mediaInformation
                    val metadata = tempFile.extractTrackMetadata(ff)

                    if (metadata?.mimeType?.startsWith("audio/") == true) {
                        // Make an educated guess on artist/album names from path.
                        // Segment 1 is most likely a standard directory like "Music",
                        // so drop it:
                        val pathSegments = documentFile.uri.getRelativePathWithoutFilename()?.split('/')?.drop(1)
                        val id3 = ff?.extractID3Data() ?: ID3Data()
                        val (pathArtist, pathTitle) = when (pathSegments?.size) {
                            1 -> pathSegments[0].split(Regex(" +- +"), 2).padStart(2)
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
                        val albumArtist = id3.albumArtist ?: pathArtist
                        val albumTitle = id3.album ?: pathTitle ?: context.getString(R.string.unknown_album)
                        val album = albums.find { it.artist == albumArtist && it.title == albumTitle }
                            ?: Album(
                                title = albumTitle,
                                artist = albumArtist,
                                isInLibrary = true,
                                isLocal = true,
                            ).also { albums.add(it) }

                        try {
                            tracks.add(
                                Track(
                                    title = id3.title ?: filenameTitle ?: context.getString(R.string.unknown_title),
                                    isInLibrary = true,
                                    artist = id3.artist ?: pathArtist,
                                    albumPosition = id3.trackNumber ?: filenameAlbumPosition?.toIntOrNull(),
                                    discNumber = id3.discNumber,
                                    year = id3.year,
                                    albumId = album.albumId,
                                    metadata = metadata,
                                    localUri = documentFile.uri,
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(javaClass.simpleName, "listNewMediaStoreAlbums: $e, documentFile=$documentFile", e)
                        }
                    }
                }
            }
        }

        albumPojos.addAll(
            albums.map { album ->
                AlbumWithTracksPojo(
                    album = album,
                    tracks = tracks.filter { it.albumId == album.albumId }.sortedBy { it.albumPosition },
                )
            }
        )
        return albumPojos
    }
}
