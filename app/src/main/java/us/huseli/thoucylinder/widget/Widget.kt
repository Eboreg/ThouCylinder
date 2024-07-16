package us.huseli.thoucylinder.widget

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import us.huseli.thoucylinder.Logger
import us.huseli.thoucylinder.MainActivity
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.getUmlautifiedString
import us.huseli.thoucylinder.managers.WidgetManager

@SuppressLint("RestrictedApi")
@Composable
fun Widget(manager: WidgetManager) {
    val context = LocalContext.current
    val size = LocalSize.current
    val baseBackgroundColor = GlanceTheme.colors.background.getColor(context)

    val albumArtAverageColor by manager.albumArtAverageColor.collectAsState()
    val currentBitmap by manager.currentBitmap.collectAsState()
    val currentTrackString by manager.currentTrackString.collectAsState()

    val backgroundColor = remember(albumArtAverageColor) {
        albumArtAverageColor?.compositeOver(baseBackgroundColor) ?: baseBackgroundColor
    }

    Logger.log("Widget", "size=$size")

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(backgroundColor)
            .cornerRadius(5.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
            Box(
                modifier = GlanceModifier
                    .size(size.height - 16.dp)
                    .background(GlanceTheme.colors.onPrimary)
                    .padding(2.dp)
                    .cornerRadius(4.dp)
            ) {
                Image(
                    provider = currentBitmap?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.splashscreen_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(size.height - 20.dp).cornerRadius(2.dp),
                )
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(start = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                ) {
                    Text(
                        text = (currentTrackString ?: context.getUmlautifiedString(R.string.no_track_playing)),
                        style = TextStyle(
                            color = GlanceTheme.colors.onBackground,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }

                WidgetButtonRow(manager = manager, modifier = GlanceModifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun WidgetButtonRow(
    manager: WidgetManager,
    modifier: GlanceModifier = GlanceModifier,
    buttonHeight: Dp = 36.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    val buttons by manager.buttons.collectAsState()
    val isPlaying by manager.isPlaying.collectAsState()

    Row(modifier = modifier, verticalAlignment = verticalAlignment) {
        val baseModifier = GlanceModifier.cornerRadius(10.dp).defaultWeight().height(buttonHeight)

        if (buttons.contains(WidgetButton.PREVIOUS)) {
            val canGotoPrevious by manager.canGotoPrevious.collectAsState()

            Image(
                provider = ImageProvider(R.drawable.media3_notification_seek_to_previous),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (isPlaying || canGotoPrevious) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = if (isPlaying || canGotoPrevious) baseModifier.clickable { manager.skipToStartOrPrevious() } else baseModifier,
            )
        }
        if (buttons.contains(WidgetButton.REWIND)) Image(
            provider = ImageProvider(R.drawable.media3_notification_seek_back),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (isPlaying) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = if (isPlaying) baseModifier.clickable { manager.seekBack() } else baseModifier,
        )
        if (buttons.contains(WidgetButton.PLAY_OR_PAUSE)) {
            val canPlay by manager.canPlay.collectAsState()

            if (isPlaying) {
                Image(
                    provider = ImageProvider(R.drawable.media3_notification_pause),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                    modifier = baseModifier.clickable { manager.playOrPauseCurrent() },
                )
            } else {
                Image(
                    provider = ImageProvider(R.drawable.media3_notification_play),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (canPlay) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = if (canPlay) baseModifier.clickable { manager.playOrPauseCurrent() } else baseModifier,
                )
            }
        }
        if (buttons.contains(WidgetButton.FORWARD)) Image(
            provider = ImageProvider(R.drawable.media3_notification_seek_forward),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (isPlaying) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = if (isPlaying) baseModifier.clickable { manager.seekForward() } else baseModifier,
        )
        if (buttons.contains(WidgetButton.REPEAT)) {
            val isRepeatEnabled by manager.isRepeatEnabled.collectAsState()

            Image(
                provider = ImageProvider(R.drawable.repeat),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (isRepeatEnabled) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = baseModifier.clickable { manager.toggleRepeat() },
            )
        }
        if (buttons.contains(WidgetButton.SHUFFLE)) {
            val isShuffleEnabled by manager.isShuffleEnabled.collectAsState()

            Image(
                provider = ImageProvider(R.drawable.shuffle),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (isShuffleEnabled) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = baseModifier.clickable { manager.toggleShuffle() },
            )
        }
        if (buttons.contains(WidgetButton.NEXT)) {
            val canGotoNext by manager.canGotoNext.collectAsState()

            Image(
                provider = ImageProvider(R.drawable.media3_notification_seek_to_next),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (canGotoNext) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = if (canGotoNext) baseModifier.clickable { manager.skipToNext() } else baseModifier,
            )
        }
    }
}
