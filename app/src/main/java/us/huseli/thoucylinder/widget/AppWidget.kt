package us.huseli.thoucylinder.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.material3.ColorProviders
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import us.huseli.retaintheme.ui.theme.DarkColors
import us.huseli.retaintheme.ui.theme.LightColors
import us.huseli.thoucylinder.managers.ImageManager
import us.huseli.thoucylinder.repositories.PlayerRepository

class AppWidget : GlanceAppWidget() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppWidgetEntryPoint {
        fun imageManager(): ImageManager
        fun playerRepository(): PlayerRepository
    }

    private fun getImageManager(context: Context): ImageManager =
        EntryPointAccessors.fromApplication(context, AppWidgetEntryPoint::class.java).imageManager()

    private fun getPlayerRepository(context: Context): PlayerRepository =
        EntryPointAccessors.fromApplication(context, AppWidgetEntryPoint::class.java).playerRepository()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colors = ColorProviders(
            light = LightColors,
            dark = DarkColors.copy(onSurfaceVariant = Color(0xFF899294)),
        )
        val imageManager = getImageManager(context.applicationContext)
        val playerRepo = getPlayerRepository(context.applicationContext)

        provideContent {
            GlanceTheme(colors = colors) {
                Widget(playerRepo = playerRepo, imageManager = imageManager)
            }
        }
    }
}
