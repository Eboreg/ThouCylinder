package us.huseli.thoucylinder.compose.track

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.Help
import androidx.compose.material.icons.automirrored.sharp.HelpOutline
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.bytesToString
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.formattedString
import us.huseli.thoucylinder.getUmlautifiedString
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.TrackInfoViewModel

fun spotifyKeyToString(key: Int?): String = when (key) {
    0 -> "C"
    1 -> "C♯/D♭"
    2 -> "D"
    3 -> "D♯/E♭"
    4 -> "E"
    5 -> "F"
    6 -> "F♯/G♭"
    7 -> "G"
    8 -> "G♯/A♭"
    9 -> "A"
    10 -> "A♯/B♭"
    11 -> "B"
    else -> "-"
}

fun spotifyModeToString(mode: Int?, context: Context): String = when (mode) {
    0 -> context.getUmlautifiedString(R.string.minor)
    1 -> context.getUmlautifiedString(R.string.major)
    else -> "-"
}

fun spotifyLoudnessToString(loudness: Float?): String = when (loudness) {
    null -> "-"
    else -> "$loudness dB"
}

fun spotifyTempoToString(tempo: Float?): String = when (tempo) {
    null -> "-"
    else -> "$tempo BPM"
}

fun spotifyTimeSignatureToString(timeSignature: Int?): String = when (timeSignature) {
    null -> "-"
    else -> "$timeSignature/4"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackInfoDialog(
    trackCombo: TrackCombo,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    viewModel: TrackInfoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val audioFeatures by viewModel.getAudioFeatures(trackCombo).collectAsStateWithLifecycle()
    val isFetchingAudioFeatures by viewModel.isFetchingAudioFeatures.collectAsStateWithLifecycle()
    val localPath = remember(trackCombo) { viewModel.getLocalAbsolutePath(trackCombo.track) }

    AlertDialog(
        modifier = modifier.padding(horizontal = 20.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        dismissButton = { CancelButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
        confirmButton = {},
        title = { Text(trackCombo.track.title.umlautify()) },
        text = {
            val fileSize = remember { trackCombo.track.getFileSize(context) }
            val albumArtist = trackCombo.albumArtists.joined()
            val trackArtist = trackCombo.trackArtists.joined()

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    maxItemsInEachRow = if (isInLandscapeMode()) 2 else 1,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (trackArtist != null) TrackInfoTextRow(
                        label = stringResource(R.string.artist),
                        value = trackArtist.umlautify(),
                    )
                    if (trackCombo.album?.title != null) TrackInfoTextRow(
                        label = stringResource(R.string.album),
                        value = trackCombo.album.title.umlautify(),
                    )
                    if (albumArtist != null) TrackInfoTextRow(
                        label = stringResource(R.string.album_artist),
                        value = albumArtist.umlautify(),
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.year),
                        value = trackCombo.year?.toString() ?: "-",
                    )
                    trackCombo.track.duration?.also { duration ->
                        TrackInfoTextRow(
                            label = stringResource(R.string.duration),
                            value = duration.sensibleFormat(),
                        )
                    }
                    trackCombo.track.metadata?.also { metadata ->
                        TrackInfoTextRow(
                            label = stringResource(R.string.mime_type),
                            value = metadata.mimeType.umlautify(),
                        )
                        fileSize?.also {
                            TrackInfoTextRow(
                                label = stringResource(R.string.file_size),
                                value = it.bytesToString(),
                            )
                        }
                        metadata.audioAttributes?.also {
                            TrackInfoTextRow(
                                label = stringResource(R.string.audio_attributes),
                                value = it.umlautify(),
                            )
                        }
                    }
                    TrackInfoTextRow(
                        label = stringResource(R.string.play_count),
                        value = trackCombo.track.playCount.toString(),
                    )
                    TrackInfoBooleanRow(
                        label = stringResource(R.string.is_in_library),
                        value = trackCombo.track.isInLibrary,
                    )
                    TrackInfoBooleanRow(
                        label = stringResource(R.string.is_downloaded),
                        value = trackCombo.track.isDownloaded,
                    )
                    TrackInfoBooleanRow(
                        label = stringResource(R.string.is_on_youtube),
                        value = trackCombo.track.isOnYoutube,
                    )
                    TrackInfoBooleanRow(
                        label = stringResource(R.string.is_on_spotify),
                        value = trackCombo.track.isOnSpotify,
                    )
                    localPath?.also {
                        Text(text = stringResource(R.string.local_file))
                        Text(text = it, style = FistopyTheme.bodyStyles.primarySmall)
                    }
                }

                if (trackCombo.track.spotifyId != null) {
                    Text(
                        text = stringResource(R.string.spotify_audio_features),
                        style = FistopyTheme.typography.titleMedium,
                    )

                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        maxItemsInEachRow = if (isInLandscapeMode()) 2 else 1,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        audioFeatures?.also { features ->
                            SpotifyTrackDetailsRow(
                                label = "Acousticness",
                                value = features.acousticness?.formattedString(3),
                                description = "A confidence measure from 0.0 to 1.0 of whether the track is " +
                                    "acoustic. 1.0 represents high confidence the track is acoustic.",
                            )
                            SpotifyTrackDetailsRow(
                                label = "Danceability",
                                value = features.danceability?.formattedString(3),
                                description = "Danceability describes how suitable a track is for dancing based " +
                                    "on a combination of musical elements including tempo, rhythm stability, " +
                                    "beat strength, and overall regularity. A value of 0.0 is least danceable " +
                                    "and 1.0 is most danceable.",
                            )
                            SpotifyTrackDetailsRow(
                                label = "Energy",
                                value = features.energy?.formattedString(3),
                                description = "Energy is a measure from 0.0 to 1.0 and represents a perceptual " +
                                    "measure of intensity and activity. Typically, energetic tracks feel fast, " +
                                    "loud, and noisy. For example, death metal has high energy, while a Bach " +
                                    "prelude scores low on the scale. Perceptual features contributing to this " +
                                    "attribute include dynamic range, perceived loudness, timbre, onset rate, " +
                                    "and general entropy.",
                            )
                            SpotifyTrackDetailsRow(
                                label = "Instrumentalness",
                                value = features.instrumentalness?.formattedString(3),
                                description = "Predicts whether a track contains no vocals. \"Ooh\" and \"aah\"" +
                                    " sounds are treated as instrumental in this context. Rap or spoken word " +
                                    "tracks are clearly \"vocal\". The closer the instrumentalness value is to " +
                                    "1.0, the greater likelihood the track contains no vocal content. Values " +
                                    "above 0.5 are intended to represent instrumental tracks, but confidence is " +
                                    "higher as the value approaches 1.0.",
                            )
                            SpotifyTrackDetailsRow(
                                label = "Liveness",
                                value = features.liveness?.formattedString(3),
                                description = "Detects the presence of an audience in the recording. Higher " +
                                    "liveness values represent an increased probability that the track was " +
                                    "performed live. A value above 0.8 provides strong likelihood that the track " +
                                    "is live.",
                            )
                            SpotifyTrackDetailsRow(
                                label = "Loudness",
                                value = spotifyLoudnessToString(features.loudness),
                                description = "The overall loudness of a track in decibels (dB). Loudness values " +
                                    "are averaged across the entire track and are useful for comparing relative " +
                                    "loudness of tracks. Loudness is the quality of a sound that is the primary " +
                                    "psychological correlate of physical strength (amplitude). Values typically " +
                                    "range between -60 and 0 db.",
                            )
                            SpotifyTrackDetailsRow(
                                label = "Key",
                                value = spotifyKeyToString(features.key).umlautify(),
                            )
                            SpotifyTrackDetailsRow(
                                label = "Mode",
                                value = spotifyModeToString(features.mode, context),
                            )
                            SpotifyTrackDetailsRow(
                                label = "Speechiness",
                                value = features.speechiness?.formattedString(3),
                                description = "Speechiness detects the presence of spoken words in a track. The " +
                                    "more exclusively speech-like the recording (e.g. talk show, audio book, " +
                                    "poetry), the closer to 1.0 the attribute value. Values above 0.66 describe " +
                                    "tracks that are probably made entirely of spoken words. Values between 0.33 " +
                                    "and 0.66 describe tracks that may contain both music and speech, either in " +
                                    "sections or layered, including such cases as rap music. Values below 0.33 " +
                                    "most likely represent music and other non-speech-like tracks.",
                            )
                            SpotifyTrackDetailsRow(
                                label = "Tempo",
                                value = spotifyTempoToString(features.tempo)
                            )
                            SpotifyTrackDetailsRow(
                                label = "Time signature",
                                value = spotifyTimeSignatureToString(features.timeSignature),
                            )
                            SpotifyTrackDetailsRow(
                                label = "Valence",
                                value = features.valence?.formattedString(3),
                                description = "A measure from 0.0 to 1.0 describing the musical positiveness " +
                                    "conveyed by a track. Tracks with high valence sound more positive (e.g. " +
                                    "happy, cheerful, euphoric), while tracks with low valence sound more " +
                                    "negative (e.g. sad, depressed, angry).",
                            )
                        } ?: run {
                            if (isFetchingAudioFeatures) CircularProgressIndicator()
                            else Text(text = "Failed to get audio features.")
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.TrackInfoBooleanRow(label: String, value: Boolean?, modifier: Modifier = Modifier) {
    val colors = LocalBasicColors.current
    val iconModifier = Modifier.size(20.dp)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.weight(1f).fillMaxWidth()) {
        Text(text = label.umlautify(), modifier = Modifier.weight(0.4f))
        when (value) {
            true -> Icon(Icons.Rounded.CheckCircle, null, tint = colors.Green, modifier = iconModifier)
            false -> Icon(Icons.Rounded.Cancel, null, tint = colors.Red, modifier = iconModifier)
            null -> Text(text = "-", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.TrackInfoTextRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label.umlautify(), modifier = Modifier.weight(0.4f))
        Text(text = value.umlautify(), modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.SpotifyTrackDetailsRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    var isDescriptionShown by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.weight(1f).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(0.6f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = label.umlautify())

                if (description != null) {
                    Icon(
                        imageVector = if (isDescriptionShown) Icons.AutoMirrored.Sharp.Help else Icons.AutoMirrored.Sharp.HelpOutline,
                        contentDescription = stringResource(R.string.description),
                        modifier = Modifier.clickable { isDescriptionShown = !isDescriptionShown },
                    )
                }
            }
            Text(text = value?.umlautify() ?: "-", modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
        }

        if (description != null) {
            AnimatedVisibility(visible = isDescriptionShown) {
                Text(
                    text = description.umlautify(),
                    style = FistopyTheme.typography.bodySmall,
                )
            }
        }
    }
}
