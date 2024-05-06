package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.baseName
import com.anggrayudi.storage.file.extension
import com.anggrayudi.storage.file.isWritable
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import us.huseli.retaintheme.extensions.combineEquals
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.retaintheme.extensions.mostCommonValue
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.copyFrom
import us.huseli.thoucylinder.copyTo
import us.huseli.thoucylinder.dataclasses.ArtistTitlePair
import us.huseli.thoucylinder.dataclasses.ID3Data
import us.huseli.thoucylinder.dataclasses.LocalImportableAlbum
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.listCoverImages
import us.huseli.thoucylinder.dataclasses.extractID3Data
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import us.huseli.thoucylinder.escapeQuotes
import us.huseli.thoucylinder.interfaces.ILogger
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class LocalMediaRepository @Inject constructor(@ApplicationContext private val context: Context) : ILogger {
    private val _isImportingLocalMedia = MutableStateFlow(false)

    val isImportingLocalMedia = _isImportingLocalMedia.asStateFlow()

    @WorkerThread
    fun convertAndTagTrack(
        tmpInFile: File,
        extension: String,
        track: Track,
        trackArtists: List<AbstractArtistCredit>,
        album: Album? = null,
        albumArtists: List<AbstractArtistCredit>? = null,
    ): File {
        val tmpOutFile = File(context.cacheDir, "${UUID.randomUUID()}.$extension")
        val tagCommands = ID3Data.fromTrack(
            track = track,
            albumArtists = albumArtists,
            trackArtists = trackArtists,
            album = album,
        ).toTagMap().map { (key, value) ->
            "-metadata \"$key=${value.escapeQuotes()}\" -metadata:s \"$key=${value.escapeQuotes()}\""
        }.joinToString(" ")
        val ffmpegCommand =
            "-i \"${tmpInFile.path}\" -y -c:a copy -id3v2_version 3 $tagCommands \"${tmpOutFile.path}\""

        log("convertAndTagTrack: running ffmpeg $ffmpegCommand")

        val session = FFmpegKit.execute(ffmpegCommand)

        if (session.returnCode.isValueSuccess) {
            tmpInFile.delete()
            return tmpOutFile
        }

        session.allLogsAsString?.also { logError(it) }
        throw Exception("Error when converting audio file: ${session.returnCode.value}")
    }

    @WorkerThread
    fun copyTempAudioFile(
        basename: String,
        tempFile: File,
        mimeType: String,
        directory: DocumentFile,
    ): DocumentFile {
        val documentFile = directory.createFile(mimeType, basename) ?: throw Exception(
            "DocumentFile.createFile() returned null. mimeType=$mimeType, basename=$basename, directory=$directory"
        )

        if (documentFile.extension.isEmpty()) documentFile.renameTo("$basename.${tempFile.extension}")
        context.contentResolver.openFileDescriptor(documentFile.uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { outputStream ->
                outputStream.write(tempFile.readBytes())
            }
        }
        return documentFile
    }

    fun deleteLocalAlbumArt(albumCombo: AlbumWithTracksCombo, albumDirectory: DocumentFile?) {
        val tracks = albumCombo.trackCombos.map { it.track }

        albumCombo.album.albumArt?.deleteInternalFiles()
        if (albumDirectory != null) albumCombo.album.albumArt?.deleteDirectoryFiles(context, albumDirectory)
        tracks.listCoverImages(context, includeThumbnails = true).forEach { documentFile ->
            if (documentFile.isFile && documentFile.canWrite()) documentFile.delete()
        }
    }

    fun flowImportableAlbums(
        treeDocumentFile: DocumentFile,
        existingTrackUris: Collection<Uri>,
    ): Flow<LocalImportableAlbum> = flow {
        val tracks = mutableListOf<LocalImportableAlbum.LocalImportableTrack>()
        val pathData = ArtistTitlePair.fromDirectory(treeDocumentFile)
        val imageFiles = mutableListOf<DocumentFile>()

        treeDocumentFile.listFiles().forEach { documentFile ->
            if (documentFile.isDirectory) {
                // Go through subdirectories recursively:
                emitAll(
                    flowImportableAlbums(
                        treeDocumentFile = documentFile,
                        existingTrackUris = existingTrackUris,
                    )
                )
            } else if (documentFile.isFile && !existingTrackUris.contains(documentFile.uri)) {
                // Just to avoid copying lots of irrelevant files:
                val mimeTypeGuess = MimeTypeMap.getSingleton().getMimeTypeFromExtension(documentFile.extension)

                if (mimeTypeGuess?.startsWith("image/") == true) imageFiles.add(documentFile)
                if (mimeTypeGuess?.startsWith("audio/") == true) {
                    // Seems like FFprobeKit doesn't get permission to access the file even though we have read/write
                    // permissions through the DocumentFile API. So we do this stupid copy to temp file shit until we
                    // have a better solution.
                    val tempFile = documentFile.copyTo(context, context.cacheDir)
                    val mediaInfo = FFprobeKit.getMediaInformation(tempFile.path)?.mediaInformation
                    val metadata = tempFile.extractTrackMetadata(mediaInfo)

                    tempFile.delete()

                    if (metadata?.mimeType?.startsWith("audio/") == true) {
                        // Make an educated guess on artist/album names from path. Segment 1 is most likely a standard
                        // directory like "Music", so drop it:
                        val id3 = mediaInfo?.extractID3Data() ?: ID3Data()
                        val (filenameAlbumPosition, filenameTitle) =
                            Regex("^(\\d+)?[ -.]*(.*)$").find(documentFile.baseName)
                                ?.groupValues
                                ?.takeLast(2)
                                ?.map { it.nullIfEmpty() }
                                ?: listOf(null, null)

                        tracks.add(
                            LocalImportableAlbum.LocalImportableTrack(
                                title = id3.title ?: filenameTitle ?: context.getString(R.string.unknown_title),
                                albumPosition = id3.trackNumber ?: filenameAlbumPosition?.toIntOrNull(),
                                metadata = metadata,
                                localUri = documentFile.uri.toString(),
                                id3 = id3,
                            )
                        )
                    }
                }
            }
        }

        val coverImage = imageFiles
            .sortedByDescending { it.length() }
            .maxByOrNull { it.name?.startsWith("cover.") == true }

        // Group the tracks by distinct musicBrainzReleaseId tags and treat each group as an album. In the absence of
        // such tags, we will work under the assumption "1 directory == 1 album" for now.
        tracks.combineEquals { a, b -> a.id3.musicBrainzReleaseId == b.id3.musicBrainzReleaseId }
            .forEach { albumTracks ->
                val albumTitle = albumTracks.mapNotNull { it.id3.album }.mostCommonValue()
                    ?: pathData.title
                    ?: context.getString(R.string.unknown_album)
                val albumArtist = albumTracks.mapNotNull { it.id3.albumArtist }.mostCommonValue()
                    ?: albumTracks.mapNotNull { it.id3.artist }.mostCommonValue()
                    ?: pathData.artist

                emit(
                    LocalImportableAlbum(
                        title = albumTitle,
                        artistName = albumArtist,
                        trackCount = albumTracks.size,
                        year = albumTracks.mapNotNull { it.id3.year }.toSet().takeIf { it.size == 1 }?.first(),
                        musicBrainzReleaseId = albumTracks.map { it.id3.musicBrainzReleaseId }.first(),
                        musicBrainzReleaseGroupId = albumTracks.firstNotNullOfOrNull { it.id3.musicBrainzReleaseGroupId },
                        tracks = albumTracks,
                        duration = albumTracks.sumOf { it.metadata.durationMs }.milliseconds,
                        thumbnailUrl = coverImage?.uri?.toString(),
                    )
                )
            }
    }

    fun listTracksWithBrokenLocalUris(allTracks: Collection<Track>): List<Track> {
        return allTracks.associateWith { it.localUri }
            .filterValuesNotNull()
            .filter { (_, uri) -> DocumentFile.fromSingleUri(context, uri.toUri())?.exists() != true }
            .map { it.key }
    }

    fun setIsImporting(value: Boolean) {
        _isImportingLocalMedia.value = value
    }

    @WorkerThread
    fun tagTrack(
        track: Track,
        trackArtists: List<AbstractArtistCredit>,
        album: Album? = null,
        albumArtists: List<AbstractArtistCredit>? = null,
    ) {
        val documentFile = track.getDocumentFile(context)

        if (documentFile == null) {
            logError("tagTrack: DocumentFile not found")
        } else if (!documentFile.isWritable(context)) {
            logError("tagTrack: Cannot write to $documentFile")
        } else {
            val tmpInFile =
                File(context.cacheDir, "${documentFile.baseName}.in.tmp.${documentFile.extension}")
            val tmpOutFile =
                File(context.cacheDir, "${documentFile.baseName}.out.tmp.${documentFile.extension}")
            val tagCommands = ID3Data.fromTrack(
                track = track,
                albumArtists = albumArtists,
                trackArtists = trackArtists,
                album = album,
            ).toTagMap().map { (key, value) ->
                "-metadata \"$key=${value.escapeQuotes()}\" -metadata:s \"$key=${value.escapeQuotes()}\""
            }.joinToString(" ")
            val ffmpegCommand =
                "-i \"${tmpInFile.path}\" -y -codec copy -id3v2_version 3 $tagCommands \"${tmpOutFile.path}\""

            log("tagTrack: running ffmpeg $ffmpegCommand")
            documentFile.copyTo(tmpInFile, context)

            val tagSession = FFmpegKit.execute(ffmpegCommand)

            if (tagSession.returnCode.isValueSuccess) {
                documentFile.copyFrom(tmpOutFile, context)
            } else if (tagSession.returnCode.isValueError) {
                logError("tagTrack: error, return code=${tagSession.returnCode.value}")
                tagSession.allLogsAsString?.also { logError(it) }
            } else if (tagSession.returnCode.isValueCancel) {
                logWarning("tagTrack: cancel, return code=${tagSession.returnCode.value}")
                tagSession.allLogsAsString?.also { logError(it) }
            }

            tmpInFile.delete()
            tmpOutFile.delete()
        }
    }
}
