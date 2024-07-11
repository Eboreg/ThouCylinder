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
import androidx.glance.unit.ColorProvider
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
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(5.dp)
            .background(backgroundColor)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
            Box(
                modifier = GlanceModifier
                    .size(size.height - 16.dp)
                    .background(ColorProvider(R.color.splash_background))
                    .padding(3.dp)
                    .cornerRadius(5.dp)
            ) {
                Image(
                    provider = currentBitmap?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.splashscreen_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(size.height - 22.dp).cornerRadius(3.dp),
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
    val canGotoNext by manager.canGotoNext.collectAsState()
    val canGotoPrevious by manager.canGotoPrevious.collectAsState()
    val canPlay by manager.canPlay.collectAsState()
    val isPlaying by manager.isPlaying.collectAsState()

    Row(modifier = modifier, verticalAlignment = verticalAlignment) {
        Image(
            provider = ImageProvider(R.drawable.media3_notification_seek_to_previous),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (isPlaying || canGotoPrevious) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.defaultWeight()
                .height(buttonHeight)
                .then(
                    if (isPlaying || canGotoPrevious) GlanceModifier.clickable { manager.skipToStartOrPrevious() }
                    else GlanceModifier
                )
        )
        Image(
            provider = ImageProvider(R.drawable.media3_notification_seek_back),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (isPlaying) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.defaultWeight()
                .height(buttonHeight)
                .then(
                    if (isPlaying) GlanceModifier.clickable { manager.seekBack() }
                    else GlanceModifier
                )
        )
        if (isPlaying) {
            Image(
                provider = ImageProvider(R.drawable.media3_notification_pause),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                modifier = GlanceModifier.defaultWeight()
                    .height(buttonHeight)
                    .clickable { manager.playOrPauseCurrent() },
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.media3_notification_play),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    if (canPlay) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.defaultWeight()
                    .height(buttonHeight)
                    .then(
                        if (canPlay) GlanceModifier.clickable { manager.playOrPauseCurrent() }
                        else GlanceModifier
                    )
            )
        }
        Image(
            provider = ImageProvider(R.drawable.media3_notification_seek_forward),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (isPlaying) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.defaultWeight()
                .height(buttonHeight)
                .then(
                    if (isPlaying) GlanceModifier.clickable { manager.seekForward() }
                    else GlanceModifier
                )
        )
        Image(
            provider = ImageProvider(R.drawable.media3_notification_seek_to_next),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (canGotoNext) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.defaultWeight()
                .height(buttonHeight)
                .then(
                    if (canGotoNext) GlanceModifier.clickable { manager.skipToNext() }
                    else GlanceModifier
                )
        )
    }
}
